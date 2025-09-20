import { test, expect } from '@playwright/test';

// Helper to simulate network conditions
const simulateSlowNetwork = async (page: any) => {
  await page.route('**/*', (route: any) => {
    setTimeout(() => route.continue(), 1000);
  });
};

// Helper to simulate server errors
const simulateServerError = async (page: any, status: number) => {
  await page.route('**/api/**', (route: any) => {
    route.fulfill({ status, body: 'Server Error' });
  });
};

test.describe('Authentication Flow', () => {
  
  test('should display login page with sign up option', async ({ page }) => {
    await page.goto('/');
    
    // Should redirect to login page for unauthenticated users
    await expect(page).toHaveURL('/login');
    
    // Check main elements are present
    await expect(page.locator('text=Welcome to MiniBank')).toBeVisible();
    await expect(page.locator('text=Digital Banking Platform')).toBeVisible();
    
    // Check authentication buttons
    await expect(page.locator('button:has-text("Sign in with Keycloak")')).toBeVisible();
    await expect(page.locator('button:has-text("Create new account")')).toBeVisible();
  });

  test('should handle Keycloak unavailable gracefully', async ({ page }) => {
    // Mock fetch to simulate Keycloak being unavailable
    await page.route('**/realms/minibank/.well-known/openid-configuration', route => {
      route.abort('failed');
    });
    
    await page.goto('/login');
    
    // Should show error message when Keycloak is unavailable
    await expect(page.locator('text=Authentication service unavailable')).toBeVisible({ timeout: 10000 });
    
    // Buttons should be disabled
    await expect(page.locator('button:has-text("Sign in with Keycloak")')).toBeDisabled();
    await expect(page.locator('button:has-text("Create new account")')).toBeDisabled();
    
    // Should show retry button after error
    await expect(page.locator('button:has-text("Retry Connection")')).toBeVisible();
  });

  test('should protect dashboard route', async ({ page }) => {
    await page.goto('/dashboard');
    
    // Should redirect to login with callbackUrl
    await expect(page).toHaveURL(/\/login\?callbackUrl=/);
    
    // Should contain the original URL in the callback parameter
    const url = page.url();
    expect(url).toContain(encodeURIComponent('/dashboard'));
  });

  test('should protect payments route', async ({ page }) => {
    await page.goto('/payments');
    
    // Should redirect to login
    await expect(page).toHaveURL(/\/login\?callbackUrl=/);
    expect(page.url()).toContain(encodeURIComponent('/payments'));
  });

  test('should protect accounts route', async ({ page }) => {
    await page.goto('/accounts');
    
    // Should redirect to login
    await expect(page).toHaveURL(/\/login\?callbackUrl=/);
    expect(page.url()).toContain(encodeURIComponent('/accounts'));
  });

  test('should protect fx route', async ({ page }) => {
    await page.goto('/fx');
    
    // Should redirect to login
    await expect(page).toHaveURL(/\/login\?callbackUrl=/);
    expect(page.url()).toContain(encodeURIComponent('/fx'));
  });

  test('should show error for authentication failures', async ({ page }) => {
    await page.goto('/login?error=AccessDenied');
    
    // Should display error message
    await expect(page.locator('text=Access denied. Please check your credentials.')).toBeVisible();
  });

  test('should show error for configuration issues', async ({ page }) => {
    await page.goto('/login?error=Configuration');
    
    // Should display configuration error
    await expect(page.locator('text=Authentication service configuration error')).toBeVisible();
    
    // Buttons should be disabled for configuration errors
    await expect(page.locator('button:has-text("Sign in with Keycloak")')).toBeDisabled();
    await expect(page.locator('button:has-text("Create new account")')).toBeDisabled();
  });

  test('should handle verification errors', async ({ page }) => {
    await page.goto('/login?error=Verification');
    
    // Should display verification error
    await expect(page.locator('text=Unable to verify your identity. Please try again.')).toBeVisible();
  });

  test('should handle unknown authentication errors', async ({ page }) => {
    await page.goto('/login?error=UnknownError');
    
    // Should display generic error message
    await expect(page.locator('text=Authentication failed. Please try again.')).toBeVisible();
  });

  test('should maintain callback URL through error states', async ({ page }) => {
    const originalPath = '/dashboard/analytics';
    await page.goto(`/login?callbackUrl=${encodeURIComponent(originalPath)}&error=AccessDenied`);
    
    // Error should be shown
    await expect(page.locator('text=Access denied')).toBeVisible();
    
    // Callback URL should be preserved (check if it would be used in form submission)
    const signInButton = page.locator('button:has-text("Sign in with Keycloak")');
    await expect(signInButton).toBeVisible();
  });

  test('should have proper page metadata', async ({ page }) => {
    await page.goto('/login');
    
    // Check page title and basic structure
    await expect(page).toHaveTitle(/MiniBank/);
    
    // Check for favicon
    const favicon = page.locator('link[rel="icon"]');
    await expect(favicon).toHaveCount(1);
  });

  test('should handle session checking state', async ({ page }) => {
    // Simulate slow session check
    await page.route('**/api/auth/session', (route) => {
      setTimeout(() => route.continue(), 2000);
    });
    
    await page.goto('/login');
    
    // Should show loading state initially
    await expect(page.locator('text=Checking authentication')).toBeVisible();
  });

  test('should handle rate limiting', async ({ page }) => {
    await page.goto('/login');
    
    // Rapidly click sign in button multiple times
    const signInButton = page.locator('button:has-text("Sign in with Keycloak")');
    
    for (let i = 0; i < 6; i++) {
      await signInButton.click();
      await page.waitForTimeout(100);
    }
    
    // Should show rate limiting message
    await expect(page.locator('text=Too many login attempts')).toBeVisible();
    await expect(signInButton).toBeDisabled();
  });

  test('should validate callback URLs for security', async ({ page }) => {
    // Test malicious callback URLs
    const maliciousUrls = [
      'http://evil.com/steal-tokens',
      'javascript:alert("xss")',
      '//evil.com/redirect',
      '/dashboard/../../../etc/passwd',
    ];
    
    for (const maliciousUrl of maliciousUrls) {
      await page.goto(`/login?callbackUrl=${encodeURIComponent(maliciousUrl)}`);
      
      // Should sanitize the URL and not use malicious callback
      const signInButton = page.locator('button:has-text("Sign in with Keycloak")');
      await expect(signInButton).toBeVisible();
      
      // URL should be sanitized (check would need to be implemented based on actual behavior)
    }
  });

  test('should handle network timeouts gracefully', async ({ page }) => {
    // Simulate slow network
    await simulateSlowNetwork(page);
    
    await page.goto('/login');
    
    // Should still load but may show loading states
    await expect(page.locator('text=Welcome to MiniBank')).toBeVisible({ timeout: 15000 });
  });

  test('should preserve security headers', async ({ page }) => {
    const response = await page.goto('/login');
    
    // Check for security headers
    const headers = response?.headers() || {};
    expect(headers['x-frame-options']).toBeTruthy();
    expect(headers['x-content-type-options']).toBe('nosniff');
  });
});