import { getSession, signOut } from 'next-auth/react';

export interface ApiResponse<T> {
  data: T;
  status: number;
  statusText: string;
}

export class ApiError extends Error {
  public status: number;
  public details?: any;

  constructor({ message, status, details }: { message: string; status: number; details?: any }) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.details = details;
  }
}

export class AuthenticationError extends ApiError {
  constructor(status: number) {
    super({
      message: status === 401 ? 'Authentication required' : 'Access forbidden',
      status,
    });
    this.name = 'AuthenticationError';
  }
}

export class ApiClient {
  private baseUrl: string;
  private retryAttempts: Map<string, number> = new Map();
  private maxRetries = 3;
  private retryDelay = 1000; // Base delay in ms

  constructor(baseUrl: string = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080') {
    this.baseUrl = baseUrl;
  }

  private async getAuthHeaders(): Promise<Record<string, string>> {
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
    };

    try {
      if (typeof window !== 'undefined') {
        // Client-side: get session from React context
        const session = await getSession();

        if (session?.error) {
          // Session has error, don't include auth header and trigger re-auth
          console.warn('Session error detected, skipping auth header:', session.error);
          return headers;
        }

        // For now, we'll handle authentication differently
        // In a production environment, you would implement a secure token endpoint
        if (session?.accessToken === 'present') {
          // We can include session info for API calls
          headers['X-Session-Present'] = 'true';
        }
      }
    } catch (error) {
      console.error('Failed to get session for auth headers:', error);
    }

    return headers;
  }

  private async delay(ms: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, ms));
  }

  private getRetryKey(endpoint: string, options: RequestInit): string {
    return `${options.method || 'GET'}:${endpoint}`;
  }

  async request<T>(
    endpoint: string,
    options: RequestInit = {},
    isRetry = false
  ): Promise<ApiResponse<T>> {
    const url = `${this.baseUrl}${endpoint}`;
    const retryKey = this.getRetryKey(endpoint, options);
    
    if (!isRetry) {
      this.retryAttempts.set(retryKey, 0);
    }

    const headers = await this.getAuthHeaders();
    const config: RequestInit = {
      ...options,
      headers: {
        ...headers,
        ...options.headers,
      },
      // Add timeout to prevent hanging requests
      signal: options.signal || AbortSignal.timeout(30000), // 30 second timeout
    };

    try {
      const response = await fetch(url, config);
      
      if (!response.ok) {
        // Handle authentication/authorization errors
        if (response.status === 401 || response.status === 403) {
          await this.handleAuthError(response.status);
          return Promise.reject(new AuthenticationError(response.status));
        }

        // Handle rate limiting with retry
        if (response.status === 429) {
          const retryCount = this.retryAttempts.get(retryKey) || 0;
          if (retryCount < this.maxRetries) {
            this.retryAttempts.set(retryKey, retryCount + 1);
            const retryAfter = response.headers.get('Retry-After');
            const delayMs = retryAfter ? parseInt(retryAfter) * 1000 : this.retryDelay * Math.pow(2, retryCount);
            await this.delay(delayMs);
            return this.request(endpoint, options, true);
          }
        }

        // Handle server errors with retry for non-mutating requests
        if (response.status >= 500 && (!options.method || options.method === 'GET')) {
          const retryCount = this.retryAttempts.get(retryKey) || 0;
          if (retryCount < this.maxRetries) {
            this.retryAttempts.set(retryKey, retryCount + 1);
            const delayMs = this.retryDelay * Math.pow(2, retryCount);
            await this.delay(delayMs);
            return this.request(endpoint, options, true);
          }
        }

        let errorMessage = `HTTP ${response.status}: ${response.statusText}`;
        let errorDetails;

        try {
          const contentType = response.headers.get('content-type');
          if (contentType?.includes('application/json')) {
            const errorData = await response.json();
            if (errorData.message) {
              errorMessage = errorData.message;
            }
            errorDetails = errorData;
          }
        } catch {
          // Response body is not JSON or couldn't be parsed, use default message
        }

        throw new ApiError({
          message: errorMessage,
          status: response.status,
          details: errorDetails,
        });
      }

      // Clear retry counter on success
      this.retryAttempts.delete(retryKey);

      // Handle empty responses
      const contentType = response.headers.get('content-type');
      const contentLength = response.headers.get('content-length');
      
      let data: T;
      if (contentLength === '0' || !contentType?.includes('application/json')) {
        data = {} as T;
      } else {
        data = await response.json();
      }
      
      return {
        data,
        status: response.status,
        statusText: response.statusText,
      };
    } catch (error) {
      if (error instanceof ApiError || error instanceof AuthenticationError) {
        throw error;
      }
      
      // Handle network errors with retry
      if (error instanceof TypeError && error.message.includes('fetch')) {
        const retryCount = this.retryAttempts.get(retryKey) || 0;
        if (retryCount < this.maxRetries && (!options.method || options.method === 'GET')) {
          this.retryAttempts.set(retryKey, retryCount + 1);
          const delayMs = this.retryDelay * Math.pow(2, retryCount);
          await this.delay(delayMs);
          return this.request(endpoint, options, true);
        }
      }
      
      throw new ApiError({
        message: 'Network error or invalid response',
        status: 0,
        details: error,
      });
    }
  }

  private async handleAuthError(status: number): Promise<void> {
    if (typeof window !== 'undefined') {
      // Client-side: get current path without exposing tokens
      const currentPath = window.location.pathname + window.location.search;
      
      // Sanitize the callback URL to prevent token exposure
      const sanitizedPath = currentPath.split('?')[0]; // Remove query parameters that might contain tokens
      
      try {
        await signOut({ 
          callbackUrl: `/login?callbackUrl=${encodeURIComponent(sanitizedPath)}`,
          redirect: true 
        });
      } catch (signOutError) {
        console.error('Failed to sign out:', signOutError);
        // Force redirect even if signOut fails
        window.location.href = `/login?callbackUrl=${encodeURIComponent(sanitizedPath)}`;
      }
    } else {
      // Server-side: throw error to be handled by caller
      throw new AuthenticationError(status);
    }
  }

  async get<T>(endpoint: string, options?: RequestInit): Promise<ApiResponse<T>> {
    return this.request<T>(endpoint, { ...options, method: 'GET' });
  }

  async post<T>(endpoint: string, body?: any, options?: RequestInit): Promise<ApiResponse<T>> {
    return this.request<T>(endpoint, {
      ...options,
      method: 'POST',
      body: body ? JSON.stringify(body) : undefined,
    });
  }

  async put<T>(endpoint: string, body?: any, options?: RequestInit): Promise<ApiResponse<T>> {
    return this.request<T>(endpoint, {
      ...options,
      method: 'PUT',
      body: body ? JSON.stringify(body) : undefined,
    });
  }

  async patch<T>(endpoint: string, body?: any, options?: RequestInit): Promise<ApiResponse<T>> {
    return this.request<T>(endpoint, {
      ...options,
      method: 'PATCH',
      body: body ? JSON.stringify(body) : undefined,
    });
  }

  async delete<T>(endpoint: string, options?: RequestInit): Promise<ApiResponse<T>> {
    return this.request<T>(endpoint, { ...options, method: 'DELETE' });
  }
}

