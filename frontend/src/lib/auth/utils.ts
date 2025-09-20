import { getSession } from 'next-auth/react';
import type { Session } from 'next-auth';

/**
 * Get session with error handling and validation
 */
export async function getValidSession(): Promise<Session | null> {
  try {
    if (typeof window !== 'undefined') {
      const session = await getSession();
      
      // Validate session structure
      if (session && !session.error && session.user?.id) {
        return session;
      }
    }

    return null;
  } catch (error) {
    console.error('Failed to get valid session:', error);
    return null;
  }
}

/**
 * Check if user is authenticated with valid token
 */
export async function isAuthenticated(): Promise<boolean> {
  const session = await getValidSession();
  return session !== null && !session.error;
}

/**
 * Get access token if available and valid
 */
export async function getAccessToken(): Promise<string | null> {
  const session = await getValidSession();
  
  if (session?.accessToken && session.accessToken !== 'present') {
    return session.accessToken;
  }
  
  return null;
}

/**
 * Validate callback URL for security
 */
export function validateCallbackUrl(url: string, baseUrl: string): string {
  try {
    // Handle relative URLs
    if (url.startsWith('/')) {
      const urlObj = new URL(url, baseUrl);
      
      // Check for path traversal attempts
      if (urlObj.pathname.includes('../') || urlObj.pathname.includes('..\\\\')) {
        console.warn('Path traversal attempt in callback URL:', url);
        return '/dashboard';
      }
      
      return url;
    }
    
    // Handle absolute URLs
    const urlObj = new URL(url);
    const baseUrlObj = new URL(baseUrl);
    
    // Only allow same-origin URLs
    if (urlObj.origin === baseUrlObj.origin) {
      return urlObj.pathname + urlObj.search;
    }
    
    console.warn('Cross-origin callback URL rejected:', url);
    return '/dashboard';
  } catch (error) {
    console.error('Invalid callback URL:', url, error);
    return '/dashboard';
  }
}

/**
 * Create secure redirect URL for login
 */
export function createLoginRedirect(originalUrl: string, baseUrl: string): string {
  const callbackUrl = validateCallbackUrl(originalUrl, baseUrl);
  return `/login?callbackUrl=${encodeURIComponent(callbackUrl)}`;
}

/**
 * Session error types for better error handling
 */
export enum SessionError {
  REFRESH_TOKEN_ERROR = 'RefreshAccessTokenError',
  INVALID_TOKENS = 'InvalidTokens',
  NETWORK_ERROR = 'NetworkError',
  EXPIRED_SESSION = 'ExpiredSession',
}

/**
 * Check if session has specific error
 */
export function hasSessionError(session: Session | null, errorType: SessionError): boolean {
  return session?.error === errorType;
}

/**
 * Get user-friendly error message for session errors
 */
export function getSessionErrorMessage(error: string): string {
  switch (error) {
    case SessionError.REFRESH_TOKEN_ERROR:
      return 'Your session has expired. Please sign in again.';
    case SessionError.INVALID_TOKENS:
      return 'Authentication failed. Please sign in again.';
    case SessionError.NETWORK_ERROR:
      return 'Network error occurred. Please check your connection.';
    case SessionError.EXPIRED_SESSION:
      return 'Your session has expired. Please sign in again.';
    default:
      return 'Authentication error occurred. Please sign in again.';
  }
}

/**
 * Rate limiting utilities for API calls
 */
export class RateLimiter {
  private attempts: Map<string, { count: number; resetTime: number }> = new Map();
  private maxAttempts: number;
  private windowMs: number;

  constructor(maxAttempts = 5, windowMs = 60000) {
    this.maxAttempts = maxAttempts;
    this.windowMs = windowMs;
  }

  canAttempt(key: string): boolean {
    const now = Date.now();
    const record = this.attempts.get(key);

    if (!record) {
      this.attempts.set(key, { count: 1, resetTime: now + this.windowMs });
      return true;
    }

    if (now > record.resetTime) {
      this.attempts.set(key, { count: 1, resetTime: now + this.windowMs });
      return true;
    }

    if (record.count >= this.maxAttempts) {
      return false;
    }

    record.count++;
    return true;
  }

  getRemainingTime(key: string): number {
    const record = this.attempts.get(key);
    if (!record) return 0;
    
    return Math.max(0, record.resetTime - Date.now());
  }
}

/**
 * Global rate limiter for authentication attempts
 */
export const authRateLimiter = new RateLimiter(5, 300000); // 5 attempts per 5 minutes

/**
 * Secure logout with cleanup
 */
export async function secureLogout(callbackUrl = '/login'): Promise<void> {
  try {
    // Clear client-side storage
    if (typeof window !== 'undefined') {
      localStorage.clear();
      sessionStorage.clear();

      // Clear service worker cache if available
      if ('serviceWorker' in navigator) {
        const registrations = await navigator.serviceWorker.getRegistrations();
        for (const registration of registrations) {
          await registration.unregister();
        }
      }

      // Clear cookies (best effort)
      document.cookie.split(";").forEach(cookie => {
        const eqPos = cookie.indexOf("=");
        const name = eqPos > -1 ? cookie.substr(0, eqPos).trim() : cookie.trim();
        document.cookie = `${name}=;expires=Thu, 01 Jan 1970 00:00:00 GMT;path=/`;
        document.cookie = `${name}=;expires=Thu, 01 Jan 1970 00:00:00 GMT;path=/;domain=${window.location.hostname}`;
      });
    }

    // Call NextAuth signOut
    const { signOut } = await import('next-auth/react');
    await signOut({
      callbackUrl,
      redirect: true
    });
  } catch (error) {
    console.error('Secure logout failed:', error);
    // Force redirect even if signOut fails
    if (typeof window !== 'undefined') {
      window.location.href = callbackUrl;
    }
  }
}

/**
 * Check if session is valid and not expired
 */
export function isSessionValid(session: any): boolean {
  if (!session || session.error) {
    return false;
  }

  // Check if session has expired
  if (session.expires) {
    const expiryTime = new Date(session.expires).getTime();
    const now = Date.now();

    if (now >= expiryTime) {
      return false;
    }
  }

  return true;
}

/**
 * Monitor session health and auto-refresh
 */
export class SessionMonitor {
  private checkInterval: number = 60000; // Check every minute
  private intervalId: NodeJS.Timeout | null = null;
  private onSessionExpired?: () => void;

  constructor(onSessionExpired?: () => void) {
    this.onSessionExpired = onSessionExpired;
  }

  start(): void {
    if (typeof window === 'undefined' || this.intervalId) {
      return;
    }

    this.intervalId = setInterval(async () => {
      try {
        const session = await getSession();

        if (!isSessionValid(session)) {
          this.stop();
          this.onSessionExpired?.();
        }
      } catch (error) {
        console.error('Session health check failed:', error);
      }
    }, this.checkInterval);
  }

  stop(): void {
    if (this.intervalId) {
      clearInterval(this.intervalId);
      this.intervalId = null;
    }
  }
}