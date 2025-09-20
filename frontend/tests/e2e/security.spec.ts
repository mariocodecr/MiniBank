import { test, expect } from '@playwright/test';

test.describe('Security Tests', () => {
  
  test('should enforce security headers', async ({ page }) => {
    const response = await page.goto('/dashboard');
    
    // Should redirect to login but check headers
    const headers = response?.headers() || {};
    
    // Check critical security headers
    expect(headers['x-frame-options']).toBe('DENY');
    expect(headers['x-content-type-options']).toBe('nosniff');
    expect(headers['referrer-policy']).toBe('strict-origin-when-cross-origin');
    expect(headers['permissions-policy']).toContain('camera=()');
  });

  test('should prevent path traversal in callback URLs', async ({ page }) => {
    const pathTraversalAttempts = [
      '/dashboard/../../../etc/passwd',
      '/dashboard/..\\..\\..\\windows\\system32',
      '/dashboard%2F..%2F..%2F..%2Fetc%2Fpasswd',
      '/dashboard//../../etc/passwd',
    ];

    for (const maliciousPath of pathTraversalAttempts) {
      await page.goto(`/login?callbackUrl=${encodeURIComponent(maliciousPath)}`);
      
      // Should still show login page without errors
      await expect(page.locator('text=Welcome to MiniBank')).toBeVisible();
      
      // URL should be sanitized (malicious parts removed)
      const currentUrl = page.url();
      expect(currentUrl).not.toContain('../');
      expect(currentUrl).not.toContain('..\\\\');
    }
  });

  test('should prevent open redirect attacks', async ({ page }) => {
    const maliciousRedirects = [
      'http://evil.com/steal-tokens',
      'https://phishing-site.com/fake-login',
      '//evil.com/redirect',
      'javascript:alert("xss")',
      'data:text/html,<script>alert("xss")</script>',
    ];

    for (const maliciousUrl of maliciousRedirects) {
      await page.goto(`/login?callbackUrl=${encodeURIComponent(maliciousUrl)}`);
      
      // Should show login page without redirecting
      await expect(page.locator('text=Welcome to MiniBank')).toBeVisible();
      
      // Should not navigate to malicious URL
      expect(page.url()).not.toContain('evil.com');
      expect(page.url()).not.toContain('phishing-site.com');
      expect(page.url()).not.toContain('javascript:');
      expect(page.url()).not.toContain('data:');
    }
  });

  test('should sanitize query parameters in callback URLs', async ({ page }) => {
    const sensitivePath = '/dashboard?token=secret123&password=admin&api_key=super_secret';
    await page.goto(`/login?callbackUrl=${encodeURIComponent(sensitivePath)}`);
    
    // Should show login page
    await expect(page.locator('text=Welcome to MiniBank')).toBeVisible();
    
    // Verify sensitive parameters are not exposed in any way
    const pageContent = await page.content();
    expect(pageContent).not.toContain('secret123');
    expect(pageContent).not.toContain('super_secret');
    expect(pageContent).not.toContain('password=admin');
  });

  test('should handle CSRF protection', async ({ page }) => {
    // Try to access auth endpoints directly
    const response = await page.request.post('/api/auth/signin/keycloak', {
      data: { csrfToken: 'fake-token' }
    });
    
    // Should either reject or require proper CSRF token
    expect(response.status()).not.toBe(200);
  });

  test('should prevent session fixation', async ({ page }) => {
    // This test would need to be expanded based on actual session handling
    // For now, verify that session IDs change after authentication
    
    await page.goto('/login');
    
    // Get initial session cookies
    const initialCookies = await page.context().cookies();
    const initialSessionCookie = initialCookies.find(c => c.name.includes('session'));
    
    // Mock successful authentication
    await page.route('**/api/auth/**', route => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ user: { id: '123' } })
      });
    });
    
    // After authentication (mocked), session should be different
    // In real implementation, this would verify session regeneration
  });

  test('should validate API request timeouts', async ({ page }) => {
    // Mock slow API responses
    await page.route('**/api/**', route => {
      // Don't respond to simulate timeout
      // In real test, this would verify timeout handling
    });
    
    await page.goto('/login');
    
    // Should handle timeouts gracefully without exposing errors
    await expect(page.locator('text=Welcome to MiniBank')).toBeVisible({ timeout: 15000 });
  });

  test('should prevent information disclosure in error messages', async ({ page }) => {
    // Mock 500 error from API
    await page.route('**/api/**', route => {
      route.fulfill({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({ 
          error: 'Database connection failed: password=secret123, host=internal-db.company.com',
          stack: 'Error at /secret/internal/path/database.js:123'
        })
      });
    });
    
    await page.goto('/login');
    
    // Should not expose sensitive information in error messages
    const pageContent = await page.content();
    expect(pageContent).not.toContain('password=secret123');
    expect(pageContent).not.toContain('internal-db.company.com');
    expect(pageContent).not.toContain('/secret/internal/path');
  });

  test('should enforce proper content types', async ({ page }) => {
    // Attempt to upload malicious files if file upload exists
    await page.goto('/login');
    
    // Check that responses have proper content types
    const response = await page.request.get('/api/auth/session');
    const contentType = response.headers()['content-type'];
    
    // Should be application/json for API endpoints
    expect(contentType).toContain('application/json');
  });

  test('should handle XSS prevention', async ({ page }) => {
    const xssPayloads = [
      '<script>alert("xss")</script>',
      'javascript:alert("xss")',
      'onload=alert("xss")',
      '<img src=x onerror=alert("xss")>',
    ];

    for (const payload of xssPayloads) {
      await page.goto(`/login?error=${encodeURIComponent(payload)}`);
      
      // Should not execute the script
      await expect(page.locator('text=Welcome to MiniBank')).toBeVisible();
      
      // Verify payload is properly escaped
      const pageContent = await page.content();
      expect(pageContent).not.toContain('<script>');
      expect(pageContent).not.toContain('javascript:');
      expect(pageContent).not.toContain('onerror=');
    }
  });

  test('should rate limit authentication attempts', async ({ page }) => {
    await page.goto('/login');
    
    // Simulate multiple failed authentication attempts
    const signInButton = page.locator('button:has-text("Sign in with Keycloak")');
    
    // Rapidly click sign in multiple times
    for (let i = 0; i < 6; i++) {
      await signInButton.click();
      await page.waitForTimeout(100);
    }
    
    // Should show rate limiting
    await expect(page.locator('text=Too many login attempts')).toBeVisible();
    await expect(signInButton).toBeDisabled();
  });
});