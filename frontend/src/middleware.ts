import { withAuth } from 'next-auth/middleware';
import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';

export default withAuth(
  function middleware(req) {
    const { pathname, search } = req.nextUrl;
    const token = req.nextauth.token;

    // Security headers for all responses
    const response = NextResponse.next();
    response.headers.set('X-Frame-Options', 'DENY');
    response.headers.set('X-Content-Type-Options', 'nosniff');
    response.headers.set('Referrer-Policy', 'strict-origin-when-cross-origin');
    response.headers.set('Permissions-Policy', 'camera=(), microphone=(), geolocation=()');

    // If user is not authenticated and trying to access protected routes
    if (!token && isProtectedRoute(pathname)) {
      // Create secure callback URL without exposing sensitive data
      const callbackUrl = sanitizeCallbackUrl(pathname + search);
      return NextResponse.redirect(
        new URL(`/login?callbackUrl=${encodeURIComponent(callbackUrl)}`, req.url)
      );
    }

    // If user is authenticated and trying to access auth pages, redirect to dashboard
    if (token && isAuthRoute(pathname)) {
      return NextResponse.redirect(new URL('/dashboard', req.url));
    }

    // Check for expired tokens
    if (token && token.error === 'RefreshAccessTokenError') {
      // Force re-authentication
      const callbackUrl = sanitizeCallbackUrl(pathname + search);
      return NextResponse.redirect(
        new URL(`/login?callbackUrl=${encodeURIComponent(callbackUrl)}`, req.url)
      );
    }

    return response;
  },
  {
    callbacks: {
      authorized: ({ token, req }) => {
        const { pathname } = req.nextUrl;
        
        // Always allow access to public routes
        if (isPublicRoute(pathname)) {
          return true;
        }
        
        // Always allow access to auth routes
        if (isAuthRoute(pathname)) {
          return true;
        }
        
        // Protected routes require a token
        return !!token;
      },
    },
    pages: {
      signIn: '/login',
    },
  }
);

function sanitizeCallbackUrl(url: string): string {
  try {
    // Remove potentially sensitive query parameters
    const urlObj = new URL(url, 'http://localhost');
    const allowedParams = ['tab', 'page', 'filter', 'sort', 'view'];
    
    // Only keep safe query parameters
    const sanitizedParams = new URLSearchParams();
    urlObj.searchParams.forEach((value, key) => {
      if (allowedParams.includes(key) && value.length < 100) {
        sanitizedParams.set(key, value);
      }
    });
    
    const sanitizedUrl = urlObj.pathname + (sanitizedParams.toString() ? '?' + sanitizedParams.toString() : '');
    return sanitizedUrl;
  } catch {
    // If URL parsing fails, return just the pathname
    return url.split('?')[0];
  }
}

function isPublicRoute(pathname: string): boolean {
  const publicRoutes = [
    '/',
    '/api/auth',
    '/api/health',
    '/favicon.ico',
    '/_next',
    '/images',
    '/icons',
    '/robots.txt',
    '/sitemap.xml',
  ];
  
  return publicRoutes.some(route => pathname.startsWith(route));
}

function isAuthRoute(pathname: string): boolean {
  const authRoutes = ['/login', '/auth'];
  return authRoutes.some(route => pathname.startsWith(route));
}

function isProtectedRoute(pathname: string): boolean {
  // Whitelist approach: explicitly define protected routes
  const protectedRoutes = [
    '/dashboard',
    '/accounts',
    '/payments', 
    '/fx',
    '/transactions',
    '/settings',
    '/profile',
    '/admin',
    '/reports',
    '/audit',
  ];
  
  return protectedRoutes.some(route => pathname.startsWith(route));
}

export const config = {
  matcher: [
    /*
     * Match all request paths except for the ones starting with:
     * - api/auth (authentication routes)
     * - _next/static (static files)
     * - _next/image (image optimization files)
     * - favicon.ico (favicon file)
     * - images (public images)
     * - icons (public icons)
     * - robots.txt (SEO)
     * - sitemap.xml (SEO)
     */
    '/((?!api/auth|_next/static|_next/image|favicon.ico|images|icons|robots.txt|sitemap.xml).*)',
  ],
};