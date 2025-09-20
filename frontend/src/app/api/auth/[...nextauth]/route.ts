import NextAuth from 'next-auth';
import KeycloakProvider from 'next-auth/providers/keycloak';
import type { NextAuthOptions } from 'next-auth';
import { randomBytes } from 'crypto';

// Prevent concurrent refresh attempts
const refreshPromises = new Map<string, Promise<any>>();

async function refreshAccessToken(token: any) {
  try {
    const issuer = process.env.NEXT_PUBLIC_KEYCLOAK_ISSUER;
    if (!issuer || !token.refreshToken) {
      return { ...token, error: "RefreshAccessTokenError" };
    }

    // Prevent concurrent refresh for the same token
    const refreshKey = token.refreshToken;
    if (refreshPromises.has(refreshKey)) {
      return refreshPromises.get(refreshKey)!;
    }

    const refreshPromise = (async () => {
      try {
        const response = await fetch(`${issuer}/protocol/openid-connect/token`, {
          headers: {
            "Content-Type": "application/x-www-form-urlencoded",
          },
          body: new URLSearchParams({
            client_id: process.env.NEXT_PUBLIC_KEYCLOAK_CLIENT_ID!,
            client_secret: process.env.KEYCLOAK_CLIENT_SECRET!,
            grant_type: "refresh_token",
            refresh_token: token.refreshToken,
          }),
          method: "POST",
        });

        const refreshedTokens = await response.json();

        if (!response.ok) {
          console.error("Token refresh failed:", refreshedTokens);
          throw new Error(refreshedTokens.error_description || 'Token refresh failed');
        }

        // Validate response contains required fields
        if (!refreshedTokens.access_token) {
          throw new Error('Invalid refresh response: missing access_token');
        }

        return {
          ...token,
          accessToken: refreshedTokens.access_token,
          expiresAt: Date.now() + (refreshedTokens.expires_in * 1000),
          refreshToken: refreshedTokens.refresh_token ?? token.refreshToken,
          error: undefined, // Clear any previous errors
        };
      } finally {
        refreshPromises.delete(refreshKey);
      }
    })();

    refreshPromises.set(refreshKey, refreshPromise);
    return await refreshPromise;
  } catch (error) {
    console.error("Error refreshing access token:", error);
    refreshPromises.delete(token.refreshToken);
    return {
      ...token,
      error: "RefreshAccessTokenError",
      accessToken: undefined, // Clear invalid token
    };
  }
}

const authOptions: NextAuthOptions = {
  providers: [
    KeycloakProvider({
      clientId: process.env.NEXT_PUBLIC_KEYCLOAK_CLIENT_ID!,
      clientSecret: process.env.KEYCLOAK_CLIENT_SECRET!,
      issuer: process.env.NEXT_PUBLIC_KEYCLOAK_ISSUER!,
      authorization: {
        params: {
          scope: "openid email profile",
          // Enable PKCE for enhanced security
          code_challenge_method: "S256",
        },
      },
      checks: ["pkce", "state"], // Enable PKCE and state validation
    }),
  ],
  callbacks: {
    async jwt({ token, account, user, trigger }) {
      // Initial sign in
      if (account && user) {
        // Validate required fields from account
        if (!account.access_token || !account.refresh_token) {
          console.error('Missing required tokens from account');
          return { ...token, error: 'InvalidTokens' };
        }

        return {
          ...token,
          accessToken: account.access_token,
          refreshToken: account.refresh_token,
          expiresAt: (account.expires_at ?? 0) * 1000,
          user: {
            id: user.id,
            name: user.name,
            email: user.email,
            image: user.image,
          },
          error: undefined,
        };
      }

      // Handle session update triggers
      if (trigger === 'update' && token.error) {
        // Force token refresh on session update if there's an error
        return refreshAccessToken(token);
      }

      // Check if token is expired with buffer
      const bufferTime = 60 * 1000; // 1 minute buffer
      const isExpired = Date.now() >= ((token.expiresAt as number) - bufferTime);
      
      if (!isExpired) {
        return token;
      }

      // Access token has expired, try to update it
      return refreshAccessToken(token);
    },
    async session({ session, token }) {
      // Only send necessary data to client, avoid exposing sensitive tokens
      if (token.error) {
        session.error = token.error as string;
      }

      // Never expose actual access token to client session for security
      session.accessToken = token.accessToken ? 'present' : undefined;
      session.user = token.user as any;

      // Add token expiry information for client-side token management
      session.expires = new Date((token.expiresAt as number) || 0).toISOString();

      return session;
    },
    async signIn({ user, account, profile }) {
      // Allow sign in
      return true;
    },
    async redirect({ url, baseUrl }) {
      // Sanitize and validate redirect URLs to prevent open redirect attacks
      try {
        // Allow relative URLs
        if (url.startsWith("/")) {
          // Validate against path traversal
          const normalizedPath = new URL(url, baseUrl).pathname;
          if (normalizedPath.includes('../') || normalizedPath.includes('..\\')) {
            console.warn('Path traversal attempt detected:', url);
            return `${baseUrl}/dashboard`;
          }
          return `${baseUrl}${url}`;
        }
        
        // Allow same-origin URLs only
        const urlObj = new URL(url);
        const baseUrlObj = new URL(baseUrl);
        
        if (urlObj.origin === baseUrlObj.origin) {
          return url;
        }
        
        console.warn('Cross-origin redirect attempt:', url);
        return `${baseUrl}/dashboard`;
      } catch (error) {
        console.error('Invalid redirect URL:', url, error);
        return `${baseUrl}/dashboard`;
      }
    },
  },
  pages: {
    signIn: '/login',
    error: '/auth/error',
  },
  session: {
    strategy: 'jwt',
    maxAge: 24 * 60 * 60, // 24 hours
  },
  events: {
    async signOut({ token }) {
      if (token?.refreshToken) {
        try {
          const issuer = process.env.NEXT_PUBLIC_KEYCLOAK_ISSUER;
          if (issuer) {
            // Use timeout to prevent hanging logout
            const controller = new AbortController();
            const timeoutId = setTimeout(() => controller.abort(), 5000);
            
            await fetch(`${issuer}/protocol/openid-connect/logout`, {
              method: "POST",
              headers: {
                "Content-Type": "application/x-www-form-urlencoded",
              },
              body: new URLSearchParams({
                client_id: process.env.NEXT_PUBLIC_KEYCLOAK_CLIENT_ID!,
                client_secret: process.env.KEYCLOAK_CLIENT_SECRET!,
                refresh_token: token.refreshToken as string,
              }),
              signal: controller.signal,
            });
            
            clearTimeout(timeoutId);
          }
        } catch (error) {
          console.error("Error during Keycloak logout:", error);
          // Don't throw - logout should succeed even if Keycloak call fails
        }
      }
    },
  },
  debug: false, // Temporarily disable for cleaner output
};

const handler = NextAuth(authOptions);

export { handler as GET, handler as POST };