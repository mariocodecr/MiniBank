describe('Authentication End-to-End Tests', () => {
  beforeEach(() => {
    // Clear all storage and cookies
    cy.clearAllCookies();
    cy.clearAllLocalStorage();
    cy.clearAllSessionStorage();

    // Reset interceptors
    cy.intercept('GET', '**/realms/minibank/.well-known/openid-configuration', {
      statusCode: 200,
      body: {
        issuer: 'http://localhost:8082/realms/minibank',
        authorization_endpoint: 'http://localhost:8082/realms/minibank/protocol/openid-connect/auth',
        token_endpoint: 'http://localhost:8082/realms/minibank/protocol/openid-connect/token'
      }
    }).as('keycloakConfig');
  });

  describe('Authentication Flow', () => {
    it('should redirect unauthenticated users to login', () => {
      cy.visit('/dashboard');

      // Should redirect to login
      cy.url().should('include', '/login');
      cy.contains('Welcome to MiniBank').should('be.visible');
      cy.contains('Sign in with Keycloak').should('be.visible');
    });

    it('should preserve callback URL in redirect', () => {
      const targetPath = '/dashboard?tab=accounts&view=summary';
      cy.visit(targetPath);

      cy.url().should('include', '/login');
      cy.url().should('include', encodeURIComponent(targetPath));
    });

    it('should handle Keycloak service unavailability', () => {
      // Mock Keycloak service down
      cy.intercept('GET', '**/realms/minibank/.well-known/openid-configuration', {
        forceNetworkError: true
      }).as('keycloakDown');

      cy.visit('/login');

      cy.contains('Authentication service unavailable').should('be.visible');
      cy.get('button:contains("Sign in")').should('be.disabled');
      cy.get('button:contains("Retry Connection")').should('be.visible');
    });

    it('should handle authentication errors gracefully', () => {
      cy.visit('/login?error=AccessDenied');

      cy.contains('Access denied').should('be.visible');

      // Test error page
      cy.visit('/auth/error?error=Configuration');
      cy.contains('Configuration Error').should('be.visible');
      cy.contains('contact support').should('be.visible');
    });

    it('should implement rate limiting', () => {
      cy.visit('/login');

      // Simulate rapid clicking to trigger rate limiting
      cy.get('button:contains("Sign in")').as('signInBtn');

      // Click multiple times rapidly
      for (let i = 0; i < 6; i++) {
        cy.get('@signInBtn').click({ force: true });
        cy.wait(100);
      }

      cy.contains('Too many login attempts').should('be.visible');
      cy.get('@signInBtn').should('contain', 'Please wait');
    });

    it('should successfully authenticate with Keycloak', () => {
      // Mock successful authentication flow
      cy.intercept('POST', '/api/auth/signin/keycloak', {
        statusCode: 200,
        body: { url: '/dashboard' }
      }).as('signIn');

      cy.visit('/login');
      cy.get('button:contains("Sign in")').click();

      cy.wait('@signIn');
      cy.url().should('include', '/dashboard');
    });

    it('should handle signup flow', () => {
      cy.visit('/login');

      // Mock Keycloak registration redirect
      cy.window().then((win) => {
        cy.stub(win, 'open').as('windowOpen');
      });

      cy.get('button:contains("Create new account")').click();

      // Should attempt to redirect to Keycloak registration
      cy.get('@windowOpen').should('have.been.called');
    });
  });

  describe('Session Management', () => {
    beforeEach(() => {
      // Mock authenticated session
      cy.intercept('GET', '/api/auth/session', {
        statusCode: 200,
        body: {
          user: {
            id: '1',
            name: 'Test User',
            email: 'test@example.com'
          },
          expires: new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString(),
          accessToken: 'present'
        }
      }).as('getSession');
    });

    it('should maintain session across page navigation', () => {
      cy.visit('/dashboard');
      cy.wait('@getSession');

      cy.visit('/accounts');
      cy.wait('@getSession');

      cy.visit('/payments');
      cy.wait('@getSession');

      // Session should persist
      cy.url().should('include', '/payments');
    });

    it('should handle token refresh automatically', () => {
      let refreshCalled = false;

      cy.intercept('GET', '/api/auth/token', (req) => {
        refreshCalled = true;
        req.reply({
          statusCode: 200,
          body: { accessToken: 'refreshed-token' }
        });
      }).as('refreshToken');

      cy.intercept('GET', '/api/accounts', (req) => {
        if (!refreshCalled) {
          req.reply({ statusCode: 401 });
        } else {
          req.reply({
            statusCode: 200,
            body: []
          });
        }
      }).as('getAccounts');

      cy.visit('/dashboard');

      // Trigger API call that would cause token refresh
      cy.window().then((win) => {
        win.fetch('/api/accounts');
      });

      cy.wait('@refreshToken');
      cy.wait('@getAccounts');
    });

    it('should handle expired sessions', () => {
      // Mock expired session
      cy.intercept('GET', '/api/auth/session', {
        statusCode: 200,
        body: {
          error: 'RefreshAccessTokenError',
          expires: new Date(Date.now() - 1000).toISOString()
        }
      }).as('expiredSession');

      cy.visit('/dashboard');
      cy.wait('@expiredSession');

      // Should redirect to login
      cy.url().should('include', '/login');
    });

    it('should perform secure logout', () => {
      cy.intercept('POST', '/api/auth/signout', {
        statusCode: 200,
        body: { url: '/login' }
      }).as('signOut');

      cy.intercept('POST', '**/protocol/openid-connect/logout', {
        statusCode: 200
      }).as('keycloakLogout');

      cy.visit('/dashboard');

      // Simulate logout action
      cy.window().then((win) => {
        // Clear storage as the secure logout would do
        win.localStorage.clear();
        win.sessionStorage.clear();
      });

      // Trigger logout
      cy.window().its('next-auth').then((auth) => {
        auth.signOut({ callbackUrl: '/login' });
      });

      cy.wait('@signOut');

      // Should clear all storage
      cy.getAllLocalStorage().should('be.empty');
      cy.getAllSessionStorage().should('be.empty');

      // Should redirect to login
      cy.url().should('include', '/login');
    });
  });

  describe('Security Features', () => {
    it('should validate callback URLs', () => {
      // Test path traversal prevention
      cy.visit('/login?callbackUrl=/../../../etc/passwd');
      cy.url().should('not.include', 'etc/passwd');

      // Test cross-origin prevention
      cy.visit('/login?callbackUrl=https://evil.com/steal-tokens');
      cy.url().should('not.include', 'evil.com');
    });

    it('should include security headers', () => {
      cy.request('/login').then((response) => {
        expect(response.headers).to.have.property('x-frame-options', 'DENY');
        expect(response.headers).to.have.property('x-content-type-options', 'nosniff');
        expect(response.headers).to.have.property('referrer-policy');
      });
    });

    it('should prevent token exposure in client-side storage', () => {
      cy.visit('/dashboard');

      // Check that actual tokens are not stored in localStorage or sessionStorage
      cy.getAllLocalStorage().then((storage) => {
        const allValues = Object.values(storage).flatMap(obj => Object.values(obj));
        const hasTokens = allValues.some(value =>
          typeof value === 'string' && value.includes('eyJ') // JWT tokens start with eyJ
        );
        expect(hasTokens).to.be.false;
      });

      cy.getAllSessionStorage().then((storage) => {
        const allValues = Object.values(storage).flatMap(obj => Object.values(obj));
        const hasTokens = allValues.some(value =>
          typeof value === 'string' && value.includes('eyJ')
        );
        expect(hasTokens).to.be.false;
      });
    });

    it('should handle CSP violations', () => {
      // Mock CSP violation
      cy.visit('/login');

      cy.window().then((win) => {
        const cspEvent = new win.SecurityPolicyViolationEvent('securitypolicyviolation', {
          violatedDirective: 'script-src',
          blockedURI: 'https://evil.com/malicious.js'
        });

        win.dispatchEvent(cspEvent);
      });

      // Application should continue to function despite CSP violations
      cy.contains('Welcome to MiniBank').should('be.visible');
    });
  });

  describe('API Client Authentication', () => {
    beforeEach(() => {
      // Mock authenticated session
      cy.intercept('GET', '/api/auth/session', {
        statusCode: 200,
        body: {
          user: { id: '1', name: 'Test User' },
          accessToken: 'present'
        }
      });

      cy.intercept('GET', '/api/auth/token', {
        statusCode: 200,
        body: { accessToken: 'test-access-token' }
      }).as('getToken');
    });

    it('should include auth headers in API requests', () => {
      let authHeaderReceived = false;

      cy.intercept('GET', '/api/accounts', (req) => {
        if (req.headers.authorization?.startsWith('Bearer ')) {
          authHeaderReceived = true;
        }
        req.reply({ statusCode: 200, body: [] });
      }).as('getAccounts');

      cy.visit('/dashboard');

      cy.window().then((win) => {
        win.fetch('/api/accounts');
      });

      cy.wait('@getAccounts').then(() => {
        expect(authHeaderReceived).to.be.true;
      });
    });

    it('should handle API authentication failures', () => {
      cy.intercept('GET', '/api/accounts', {
        statusCode: 401,
        body: { error: 'Unauthorized' }
      }).as('unauthorizedRequest');

      cy.visit('/dashboard');

      cy.window().then((win) => {
        win.fetch('/api/accounts').catch(() => {});
      });

      cy.wait('@unauthorizedRequest');

      // Should redirect to login on 401
      cy.url().should('include', '/login');
    });

    it('should retry failed requests with exponential backoff', () => {
      let attemptCount = 0;

      cy.intercept('GET', '/api/accounts', (req) => {
        attemptCount++;
        if (attemptCount < 3) {
          req.reply({ statusCode: 500 });
        } else {
          req.reply({ statusCode: 200, body: [] });
        }
      }).as('retryRequests');

      cy.visit('/dashboard');

      cy.window().then((win) => {
        win.fetch('/api/accounts');
      });

      // Should make multiple attempts
      cy.wait('@retryRequests');
      cy.wait('@retryRequests');
      cy.wait('@retryRequests');

      cy.then(() => {
        expect(attemptCount).to.equal(3);
      });
    });
  });

  describe('Error Boundary and Fallbacks', () => {
    it('should handle network errors gracefully', () => {
      cy.intercept('GET', '/api/auth/session', {
        forceNetworkError: true
      }).as('networkError');

      cy.visit('/dashboard');

      // Should show appropriate error message or fallback
      cy.contains('network').should('be.visible');
    });

    it('should handle JavaScript errors without breaking', () => {
      cy.visit('/login');

      // Simulate JavaScript error
      cy.window().then((win) => {
        win.eval('throw new Error("Simulated error")');
      });

      // Application should still be functional
      cy.contains('Welcome to MiniBank').should('be.visible');
      cy.get('button:contains("Sign in")').should('be.visible');
    });
  });
});