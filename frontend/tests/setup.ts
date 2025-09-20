import { test as base } from '@playwright/test';

// Extend basic test by providing authentication utilities
export const test = base.extend<{
  authenticatedPage: any;
}>({
  authenticatedPage: async ({ page }, use) => {
    // Mock authenticated session for tests that need it
    await page.route('**/api/auth/session', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          user: {
            id: 'test-user-id',
            name: 'Test User',
            email: 'test@minibank.com'
          },
          expires: new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString(),
          accessToken: 'present'
        })
      });
    });

    // Mock token endpoint
    await page.route('**/api/auth/token', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          accessToken: 'test-access-token'
        })
      });
    });

    await use(page);
  }
});

export { expect } from '@playwright/test';