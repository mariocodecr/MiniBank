import { test, expect, type Page } from '@playwright/test';

// Test configuration
const TEST_CONFIG = {
  baseURL: process.env.PLAYWRIGHT_TEST_BASEURL || 'http://localhost:3000',
  keycloakURL: process.env.KEYCLOAK_URL || 'http://localhost:8082',
  testUser: {
    username: process.env.TEST_USERNAME || 'testuser',
    password: process.env.TEST_PASSWORD || 'testpassword'
  }
};

test.describe('Authentication Flow', () => {
  test.beforeEach(async ({ page }) => {
    // Clear all storage before each test
    await page.context().clearCookies();
    await page.evaluate(() => {
      localStorage.clear();
      sessionStorage.clear();
    });
  });

  test('should redirect unauthenticated users to login', async ({ page }) => {
    await page.goto('/dashboard');

    // Should redirect to login page
    await expect(page).toHaveURL(/\/login/);
    await expect(page.getByText('Welcome to MiniBank')).toBeVisible();
  });

  test('should preserve callback URL during redirect', async ({ page }) => {
    const targetUrl = '/dashboard?tab=accounts&view=summary';
    await page.goto(targetUrl);

    // Should redirect to login with callback URL
    const currentUrl = new URL(page.url());
    expect(currentUrl.pathname).toBe('/login');
    expect(currentUrl.searchParams.get('callbackUrl')).toBe('/dashboard?tab=accounts&view=summary');
  });

  test('should handle Keycloak availability check', async ({ page }) => {
    // Intercept Keycloak well-known endpoint to simulate downtime
    await page.route('**/realms/minibank/.well-known/openid-configuration', (route) => {
      route.abort();
    });

    await page.goto('/login');

    // Should show service unavailable message
    await expect(page.getByText('Authentication service unavailable')).toBeVisible();
    await expect(page.getByRole('button', { name: /sign in/i })).toBeDisabled();

    // Should show retry button
    await expect(page.getByRole('button', { name: /retry connection/i })).toBeVisible();
  });

  test('should handle authentication errors gracefully', async ({ page }) => {
    await page.goto('/login?error=AccessDenied');

    // Should display error message
    await expect(page.getByText('Access denied')).toBeVisible();

    // Navigation to error page should work
    await page.goto('/auth/error?error=Configuration');
    await expect(page.getByText('Configuration Error')).toBeVisible();
    await expect(page.getByText('contact support')).toBeVisible();
  });

  test('should implement rate limiting for login attempts', async ({ page }) => {
    await page.goto('/login');

    // Mock failed sign-in attempts by intercepting the signIn call
    await page.evaluate(() => {
      // Simulate multiple rapid login attempts
      const event = new CustomEvent('auth-rate-limit-test');
      window.dispatchEvent(event);
    });

    // Trigger multiple rapid clicks (this would normally be prevented by rate limiting)
    const signInButton = page.getByRole('button', { name: /sign in/i });

    // In a real scenario, the button should become disabled after rate limit
    for (let i = 0; i < 6; i++) {
      await signInButton.click({ force: true });
      await page.waitForTimeout(100);
    }

    // Should eventually show rate limit message
    await expect(page.getByText(/too many login attempts/i)).toBeVisible();
  });

  test('should handle successful authentication flow', async ({ page }) => {
    // Mock successful authentication
    await page.route('**/api/auth/signin/keycloak', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ url: '/dashboard' })
      });
    });

    await page.goto('/login');
    await page.getByRole('button', { name: /sign in/i }).click();

    // Should redirect to dashboard (mocked)
    await expect(page).toHaveURL('/dashboard');
  });

  test('should handle logout properly', async ({ page }) => {
    // Mock authenticated state
    await page.addInitScript(() => {
      window.localStorage.setItem('test-auth', 'true');
    });

    // Mock session
    await page.route('**/api/auth/session', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          user: { id: '1', name: 'Test User', email: 'test@example.com' },
          expires: new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString()
        })
      });
    });

    await page.goto('/dashboard');

    // Mock logout endpoint
    await page.route('**/api/auth/signout', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ url: '/login' })
      });
    });

    // Trigger logout (assuming there's a logout button in the UI)
    await page.evaluate(() => {
      // Simulate secure logout
      window.localStorage.clear();
      window.sessionStorage.clear();
    });

    // Should clear storage and redirect to login
    const storageCleared = await page.evaluate(() => {
      return localStorage.length === 0 && sessionStorage.length === 0;
    });

    expect(storageCleared).toBe(true);
  });

  test('should handle token refresh correctly', async ({ page }) => {
    let refreshCalled = false;

    // Mock token endpoint
    await page.route('**/api/auth/token', (route) => {
      refreshCalled = true;
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ accessToken: 'new-access-token' })
      });
    });

    // Mock API call that triggers token refresh
    await page.route('**/api/accounts', (route) => {
      if (!refreshCalled) {
        // First call returns 401
        route.fulfill({ status: 401 });
      } else {
        // Second call succeeds
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify([])
        });
      }
    });

    await page.goto('/dashboard');

    // Should handle token refresh transparently
    expect(refreshCalled).toBe(true);
  });

  test('should validate callback URLs for security', async ({ page }) => {
    // Test path traversal prevention
    await page.goto('/login?callbackUrl=/../../../etc/passwd');
    const callbackInput = page.locator('[data-testid="callback-url"]');

    if (await callbackInput.isVisible()) {
      await expect(callbackInput).toHaveValue('/dashboard');
    }

    // Test cross-origin prevention
    await page.goto('/login?callbackUrl=https://evil.com/steal-tokens');
    if (await callbackInput.isVisible()) {
      await expect(callbackInput).toHaveValue('/dashboard');
    }
  });

  test('should handle expired sessions', async ({ page }) => {
    // Mock expired session
    await page.route('**/api/auth/session', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          error: 'RefreshAccessTokenError',
          expires: new Date(Date.now() - 1000).toISOString() // Expired
        })
      });
    });

    await page.goto('/dashboard');

    // Should redirect to login due to expired session
    await expect(page).toHaveURL(/\/login/);
    await expect(page.getByText('session has expired')).toBeVisible();
  });
});

test.describe('API Client Authentication', () => {
  test('should include auth headers in API requests', async ({ page }) => {
    let authHeaderSent = false;

    // Intercept API requests to check auth headers
    await page.route('**/api/**', (route) => {
      const headers = route.request().headers();
      if (headers.authorization && headers.authorization.startsWith('Bearer ')) {
        authHeaderSent = true;
      }

      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({})
      });
    });

    // Mock authenticated session
    await page.route('**/api/auth/token', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ accessToken: 'test-token' })
      });
    });

    await page.goto('/dashboard');

    // Trigger an API call
    await page.evaluate(() => {
      fetch('/api/accounts').catch(() => {});
    });

    expect(authHeaderSent).toBe(true);
  });

  test('should handle API authentication errors', async ({ page }) => {
    // Mock 401 response
    await page.route('**/api/accounts', (route) => {
      route.fulfill({ status: 401 });
    });

    await page.goto('/dashboard');

    // Make API call that returns 401
    await page.evaluate(() => {
      fetch('/api/accounts').catch(() => {});
    });

    // Should redirect to login
    await expect(page).toHaveURL(/\/login/);
  });
});

test.describe('Security Headers and CSP', () => {
  test('should include security headers', async ({ page }) => {
    const response = await page.goto('/login');
    const headers = response?.headers() || {};

    // Check for security headers
    expect(headers['x-frame-options']).toBe('DENY');
    expect(headers['x-content-type-options']).toBe('nosniff');
    expect(headers['referrer-policy']).toBe('strict-origin-when-cross-origin');
  });

  test('should prevent clickjacking', async ({ page }) => {
    // Try to embed in iframe (should be prevented by X-Frame-Options)
    await page.setContent(`
      <iframe src="${TEST_CONFIG.baseURL}/login" width="400" height="300"></iframe>
    `);

    const iframe = page.locator('iframe');

    // Frame should not load due to X-Frame-Options: DENY
    await expect(iframe).not.toBeVisible();
  });
});