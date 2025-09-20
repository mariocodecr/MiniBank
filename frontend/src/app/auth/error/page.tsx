'use client';

import { useEffect, useState, Suspense } from 'react';
import { useSearchParams, useRouter } from 'next/navigation';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Building2, AlertCircle, ArrowLeft, RefreshCw, Home } from 'lucide-react';
import { getSessionErrorMessage, SessionError } from '@/lib/auth/utils';

function AuthErrorContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [isRetrying, setIsRetrying] = useState(false);

  const error = searchParams.get('error');
  const callbackUrl = searchParams.get('callbackUrl') || '/dashboard';

  const getErrorDetails = (errorCode: string | null) => {
    switch (errorCode) {
      case 'Configuration':
        return {
          title: 'Configuration Error',
          description: 'Authentication service is misconfigured.',
          userMessage: 'There is a configuration issue with the authentication service. Please contact support.',
          canRetry: false,
          severity: 'critical'
        };
      case 'AccessDenied':
        return {
          title: 'Access Denied',
          description: 'You do not have permission to access this resource.',
          userMessage: 'Access was denied. Please check your credentials or contact your administrator.',
          canRetry: true,
          severity: 'warning'
        };
      case 'Verification':
        return {
          title: 'Verification Failed',
          description: 'Unable to verify your identity.',
          userMessage: 'We could not verify your identity. Please try signing in again.',
          canRetry: true,
          severity: 'error'
        };
      case 'Default':
      case 'Signin':
      case 'OAuthSignin':
      case 'OAuthCallback':
      case 'OAuthCreateAccount':
      case 'EmailCreateAccount':
      case 'Callback':
      case 'OAuthAccountNotLinked':
        return {
          title: 'Authentication Error',
          description: 'An error occurred during authentication.',
          userMessage: 'Something went wrong during sign in. Please try again.',
          canRetry: true,
          severity: 'error'
        };
      case 'SessionRequired':
        return {
          title: 'Session Required',
          description: 'You need to be signed in to access this page.',
          userMessage: 'Please sign in to continue.',
          canRetry: true,
          severity: 'info'
        };
      case SessionError.REFRESH_TOKEN_ERROR:
      case SessionError.EXPIRED_SESSION:
        return {
          title: 'Session Expired',
          description: 'Your session has expired.',
          userMessage: getSessionErrorMessage(errorCode),
          canRetry: true,
          severity: 'warning'
        };
      default:
        return {
          title: 'Unknown Error',
          description: 'An unexpected error occurred.',
          userMessage: 'An unexpected error occurred. Please try again or contact support.',
          canRetry: true,
          severity: 'error'
        };
    }
  };

  const errorDetails = getErrorDetails(error);

  const handleRetryAuth = async () => {
    setIsRetrying(true);
    try {
      // Clear any cached authentication state
      if (typeof window !== 'undefined') {
        localStorage.removeItem('nextauth.message');
        sessionStorage.clear();
      }

      // Wait a moment for cleanup
      await new Promise(resolve => setTimeout(resolve, 500));

      // Redirect to login
      router.push(`/login?callbackUrl=${encodeURIComponent(callbackUrl)}`);
    } catch (err) {
      console.error('Retry failed:', err);
      setIsRetrying(false);
    }
  };

  const handleGoHome = () => {
    router.push('/');
  };

  const handleGoBack = () => {
    if (window.history.length > 1) {
      router.back();
    } else {
      router.push('/');
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-md w-full space-y-8">
        <div className="text-center">
          <Building2 className="mx-auto h-12 w-12 text-primary" />
          <h2 className="mt-6 text-3xl font-extrabold text-gray-900">
            Authentication Error
          </h2>
          <p className="mt-2 text-sm text-gray-600">
            MiniBank Digital Banking Platform
          </p>
        </div>

        <Alert variant={errorDetails.severity === 'critical' ? 'destructive' : 'default'}>
          <AlertCircle className="h-4 w-4" />
          <AlertDescription>
            <div className="space-y-2">
              <p className="font-medium">{errorDetails.title}</p>
              <p className="text-sm">{errorDetails.userMessage}</p>
              {error && (
                <p className="text-xs text-gray-500 mt-2">
                  Error code: {error}
                </p>
              )}
            </div>
          </AlertDescription>
        </Alert>

        <Card>
          <CardHeader>
            <CardTitle>What happened?</CardTitle>
            <CardDescription>
              {errorDetails.description}
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            {errorDetails.canRetry && (
              <Button
                onClick={handleRetryAuth}
                className="w-full"
                size="lg"
                disabled={isRetrying}
              >
                {isRetrying ? (
                  <RefreshCw className="mr-2 h-4 w-4 animate-spin" />
                ) : (
                  <RefreshCw className="mr-2 h-4 w-4" />
                )}
                {isRetrying ? 'Retrying...' : 'Try Again'}
              </Button>
            )}

            <div className="flex space-x-2">
              <Button
                onClick={handleGoBack}
                variant="outline"
                className="flex-1"
                size="lg"
              >
                <ArrowLeft className="mr-2 h-4 w-4" />
                Go Back
              </Button>

              <Button
                onClick={handleGoHome}
                variant="outline"
                className="flex-1"
                size="lg"
              >
                <Home className="mr-2 h-4 w-4" />
                Home
              </Button>
            </div>

            <div className="mt-6 text-center">
              <p className="text-sm text-gray-500">
                If this problem persists, please contact{' '}
                <a
                  href="mailto:support@minibank.com"
                  className="text-primary hover:underline"
                >
                  technical support
                </a>
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

export default function AuthErrorPage() {
  return (
    <Suspense fallback={
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="text-center">
          <RefreshCw className="mx-auto h-8 w-8 animate-spin text-primary" />
          <p className="mt-2 text-sm text-gray-600">Loading...</p>
        </div>
      </div>
    }>
      <AuthErrorContent />
    </Suspense>
  );
}