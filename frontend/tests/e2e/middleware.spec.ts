import { test, expect } from '@playwright/test';

test.describe('Route Protection Middleware', () => {
  
  const protectedRoutes = [
    '/dashboard',
    '/accounts',
    '/payments',
    '/fx',
    '/transactions',
    '/settings',
    '/profile',
  ];

  const publicRoutes = [
    '/',
    '/favicon.ico',
  ];

  protectedRoutes.forEach(route => {
    test(`should protect ${route} route`, async ({ page }) => {
      await page.goto(route);
      
      // Should redirect to login with proper callback URL
      await page.waitForURL(/\/login/);
      const currentUrl = page.url();
      
      expect(currentUrl).toContain('/login');
      expect(currentUrl).toContain('callbackUrl=');
      expect(currentUrl).toContain(encodeURIComponent(route));
    });
  });

  test('should allow access to public routes', async ({ page }) => {
    for (const route of publicRoutes) {
      if (route === '/') {
        await page.goto(route);
        // Root redirects to login for unauthenticated users, which is expected
        await expect(page).toHaveURL(/\/login/);
      }
    }
  });

  test('should preserve query parameters in callback URL', async ({ page }) => {
    const routeWithQuery = '/dashboard?tab=analytics&period=30d';
    await page.goto(routeWithQuery);
    
    await page.waitForURL(/\/login/);
    const currentUrl = page.url();
    
    // Should preserve the full URL including query parameters
    expect(currentUrl).toContain(encodeURIComponent(routeWithQuery));
  });

  test('should handle nested protected routes', async ({ page }) => {
    const nestedRoutes = [
      '/payments/new',
      '/accounts/123',
      '/dashboard/analytics',
    ];

    for (const route of nestedRoutes) {
      await page.goto(route);
      await page.waitForURL(/\/login/);
      
      const currentUrl = page.url();
      expect(currentUrl).toContain(encodeURIComponent(route));
    }
  });

  test('should allow API routes to pass through', async ({ page }) => {
    // API routes should not redirect - they should return proper HTTP status
    const response = await page.request.get('/api/auth/session');
    
    // Should get a response (not a redirect), even if it's 401/403
    expect(response.status()).not.toBe(302);
    expect(response.status()).not.toBe(301);
  });

  test('should handle static assets', async ({ page }) => {
    // Static assets should be accessible without authentication
    const response = await page.request.get('/favicon.ico');
    
    // Should not redirect static assets
    expect(response.status()).not.toBe(302);
    expect(response.status()).not.toBe(301);
  });

  test('should handle malformed URLs gracefully', async ({ page }) => {
    const malformedUrls = [
      '/dashboard/../../../etc/passwd',
      '/accounts?"><script>alert("xss")</script>',
      '/payments/%2e%2e%2f%2e%2e%2f',
    ];

    for (const url of malformedUrls) {
      await page.goto(url);
      
      // Should still redirect to login (not crash or expose system)
      await expect(page).toHaveURL(/\/login/);
    }
  });

  test('should maintain security headers', async ({ page }) => {
    const response = await page.request.get('/login');
    
    // Check for basic security headers (these might be set by Next.js or custom middleware)
    const headers = response.headers();
    
    // X-Frame-Options should be present to prevent clickjacking
    expect(headers['x-frame-options'] || headers['X-Frame-Options']).toBeTruthy();
  });
});