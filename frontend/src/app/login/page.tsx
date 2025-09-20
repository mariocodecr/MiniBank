'use client';

import { useEffect, useState, useCallback, Suspense } from 'react';
import { signIn, getSession } from 'next-auth/react';
import { useRouter, useSearchParams } from 'next/navigation';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Building2, UserPlus, LogIn, AlertCircle, RefreshCw } from 'lucide-react';
import { validateCallbackUrl, authRateLimiter } from '@/lib/auth/utils';

function LoginPageContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [isLoading, setIsLoading] = useState(false);
  const [isCheckingSession, setIsCheckingSession] = useState(true);
  const [authError, setAuthError] = useState<string | null>(null);
  const [keycloakAvailable, setKeycloakAvailable] = useState(true);
  const [retryCount, setRetryCount] = useState(0);
  const [rateLimited, setRateLimited] = useState(false);
  
  const error = searchParams.get('error');
  const rawCallbackUrl = searchParams.get('callbackUrl') || '/dashboard';
  const callbackUrl = validateCallbackUrl(rawCallbackUrl, typeof window !== 'undefined' ? window.location.origin : 'http://localhost:3000');

  const checkKeycloakAvailability = useCallback(async () => {
    if (!process.env.NEXT_PUBLIC_KEYCLOAK_ISSUER) {
      setKeycloakAvailable(false);
      setAuthError('Authentication service not configured.');
      return;
    }

    try {
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 5000);
      
      const response = await fetch(
        `${process.env.NEXT_PUBLIC_KEYCLOAK_ISSUER}/.well-known/openid-configuration`,
        { signal: controller.signal }
      );
      
      clearTimeout(timeoutId);
      
      if (response.ok) {
        setKeycloakAvailable(true);
        if (authError?.includes('service unavailable')) {
          setAuthError(null);
        }
      } else {
        throw new Error(`HTTP ${response.status}`);
      }
    } catch (err) {
      console.error('Keycloak availability check failed:', err);
      setKeycloakAvailable(false);
      setAuthError('Authentication service unavailable. Please try again later or contact support.');
    }
  }, [authError]);

  useEffect(() => {
    let mounted = true;

    // Check if user is already authenticated
    const checkSession = async () => {
      try {
        const session = await getSession();
        if (mounted && session && !session.error) {
          window.location.href = callbackUrl;
          return;
        }
      } catch (err) {
        console.error('Session check failed:', err);
      } finally {
        if (mounted) {
          setIsCheckingSession(false);
        }
      }
    };

    checkSession();

    // Handle authentication errors
    if (error && mounted) {
      switch (error) {
        case 'Configuration':
          setAuthError('Authentication service configuration error. Please contact support.');
          setKeycloakAvailable(false);
          break;
        case 'AccessDenied':
          setAuthError('Access denied. Please check your credentials.');
          break;
        case 'Verification':
          setAuthError('Unable to verify your identity. Please try again.');
          break;
        case 'Callback':
          setAuthError('Authentication callback failed. Please try again.');
          break;
        case 'OAuthAccountNotLinked':
          setAuthError('Account not linked. Please use the same authentication method.');
          break;
        default:
          setAuthError('Authentication failed. Please try again.');
      }
    }

    // Check Keycloak availability
    void checkKeycloakAvailability();

    return () => {
      mounted = false;
    };
  }, [router, callbackUrl, error, checkKeycloakAvailability]);

  const handleSignIn = async () => {
    console.log('Sign in button clicked'); // Debug log

    if (!keycloakAvailable || isLoading) {
      console.log('Sign in blocked - keycloak available:', keycloakAvailable, 'loading:', isLoading);
      return;
    }

    // Check rate limiting
    const clientId = 'login_attempt';
    if (!authRateLimiter.canAttempt(clientId)) {
      setRateLimited(true);
      setAuthError(`Too many login attempts. Please wait ${Math.ceil(authRateLimiter.getRemainingTime(clientId) / 1000)} seconds.`);
      return;
    }

    setIsLoading(true);
    setAuthError(null);
    setRateLimited(false);

    try {
      console.log('Calling signIn with callbackUrl:', callbackUrl);

      // Clear any stale authentication state
      if (typeof window !== 'undefined') {
        localStorage.removeItem('nextauth.message');
        sessionStorage.removeItem('auth-error');
      }

      // Use NextAuth signIn function
      await signIn('keycloak', {
        callbackUrl,
        redirect: true
      });

    } catch (error) {
      console.error('Sign in failed:', error);
      setRetryCount(prev => prev + 1);

      if (retryCount >= 2) {
        setAuthError('Repeated sign-in failures. Please check your network connection or contact support.');
        setKeycloakAvailable(false);
      } else {
        setAuthError('Failed to initiate sign in. Please try again.');
      }
      setIsLoading(false);
    }
  };

  const handleSignUp = () => {
    if (!keycloakAvailable || isLoading) return;
    
    const keycloakIssuer = process.env.NEXT_PUBLIC_KEYCLOAK_ISSUER;
    const clientId = process.env.NEXT_PUBLIC_KEYCLOAK_CLIENT_ID;
    
    if (!keycloakIssuer || !clientId) {
      setAuthError('Registration not available. Please contact support.');
      return;
    }
    
    try {
      const redirectUri = encodeURIComponent(`${window.location.origin}/api/auth/callback/keycloak`);
      const state = encodeURIComponent(callbackUrl); // Include callback URL in state
      
      const signUpUrl = `${keycloakIssuer}/protocol/openid-connect/registrations?client_id=${clientId}&redirect_uri=${redirectUri}&response_type=code&scope=openid&state=${state}`;
      window.location.href = signUpUrl;
    } catch (error) {
      console.error('Sign up redirect failed:', error);
      setAuthError('Failed to redirect to registration. Please try again.');
    }
  };

  const handleRetry = () => {
    setAuthError(null);
    setRetryCount(0);
    setRateLimited(false);
    void checkKeycloakAvailability();
  };

  // Show loading state while checking session
  if (isCheckingSession) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="text-center">
          <RefreshCw className="mx-auto h-8 w-8 animate-spin text-primary" />
          <p className="mt-2 text-sm text-gray-600">Checking authentication...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-md w-full space-y-8">
        <div className="text-center">
          <Building2 className="mx-auto h-12 w-12 text-primary" />
          <h2 className="mt-6 text-3xl font-extrabold text-gray-900">
            Welcome to MiniBank
          </h2>
          <p className="mt-2 text-sm text-gray-600">
            Digital Banking Platform
          </p>
        </div>
        
        {authError && (
          <Alert variant="destructive">
            <AlertCircle className="h-4 w-4" />
            <AlertDescription>
              {authError}
              {retryCount > 0 && !rateLimited && (
                <Button
                  variant="outline"
                  size="sm"
                  onClick={handleRetry}
                  className="mt-2 w-full"
                >
                  <RefreshCw className="mr-2 h-3 w-3" />
                  Retry Connection
                </Button>
              )}
            </AlertDescription>
          </Alert>
        )}
        
        <Card>
          <CardHeader>
            <CardTitle>Access your account</CardTitle>
            <CardDescription>
              Sign in to access your banking dashboard or create a new account
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <Button 
              onClick={handleSignIn}
              className="w-full"
              size="lg"
              disabled={!keycloakAvailable || isLoading || rateLimited}
            >
              {isLoading ? (
                <RefreshCw className="mr-2 h-4 w-4 animate-spin" />
              ) : (
                <LogIn className="mr-2 h-4 w-4" />
              )}
              {isLoading ? 'Signing in...' : rateLimited ? 'Please wait...' : 'Sign in with Keycloak'}
            </Button>
            
            <div className="relative">
              <div className="absolute inset-0 flex items-center">
                <span className="w-full border-t" />
              </div>
              <div className="relative flex justify-center text-xs uppercase">
                <span className="bg-white px-2 text-muted-foreground">
                  Or
                </span>
              </div>
            </div>
            
            <Button 
              onClick={handleSignUp}
              variant="outline"
              className="w-full"
              size="lg"
              disabled={!keycloakAvailable || isLoading}
            >
              <UserPlus className="mr-2 h-4 w-4" />
              Create new account
            </Button>
            
            <div className="mt-6 text-center">
              <p className="text-sm text-gray-500">
                {keycloakAvailable 
                  ? 'Secure authentication powered by Keycloak'
                  : 'Authentication service is currently unavailable'
                }
              </p>
            </div>
          </CardContent>
        </Card>
        
        <div className="text-center text-xs text-gray-400">
          <p>© 2024 MiniBank. All rights reserved.</p>
        </div>
      </div>
    </div>
  );
}

export default function LoginPage() {
  return (
    <Suspense fallback={
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="text-center">
          <RefreshCw className="mx-auto h-8 w-8 animate-spin text-primary" />
          <p className="mt-2 text-sm text-gray-600">Loading...</p>
        </div>
      </div>
    }>
      <LoginPageContent />
    </Suspense>
  );
}