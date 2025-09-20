// Custom Cypress commands for authentication testing

declare global {
  namespace Cypress {
    interface Chainable {
      /**
       * Mock authenticated session
       */
      mockAuthSession(user?: any): Chainable<Element>;

      /**
       * Mock Keycloak service availability
       */
      mockKeycloakService(available?: boolean): Chainable<Element>;

      /**
       * Clear all authentication state
       */
      clearAuthState(): Chainable<Element>;

      /**
       * Wait for authentication to complete
       */
      waitForAuth(): Chainable<Element>;

      /**
       * Mock API endpoints with authentication
       */
      mockAuthenticatedAPI(): Chainable<Element>;
    }
  }
}

Cypress.Commands.add('mockAuthSession', (user = {}) => {
  const defaultUser = {
    id: 'test-user-id',
    name: 'Test User',
    email: 'test@minibank.com',
    ...user
  };

  cy.intercept('GET', '/api/auth/session', {
    statusCode: 200,
    body: {
      user: defaultUser,
      expires: new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString(),
      accessToken: 'present'
    }
  }).as('authSession');

  cy.intercept('GET', '/api/auth/token', {
    statusCode: 200,
    body: {
      accessToken: 'test-access-token'
    }
  }).as('authToken');
});

Cypress.Commands.add('mockKeycloakService', (available = true) => {
  if (available) {
    cy.intercept('GET', '**/realms/minibank/.well-known/openid-configuration', {
      statusCode: 200,
      body: {
        issuer: 'http://localhost:8082/realms/minibank',
        authorization_endpoint: 'http://localhost:8082/realms/minibank/protocol/openid-connect/auth',
        token_endpoint: 'http://localhost:8082/realms/minibank/protocol/openid-connect/token',
        userinfo_endpoint: 'http://localhost:8082/realms/minibank/protocol/openid-connect/userinfo'
      }
    }).as('keycloakConfig');
  } else {
    cy.intercept('GET', '**/realms/minibank/.well-known/openid-configuration', {
      forceNetworkError: true
    }).as('keycloakUnavailable');
  }
});

Cypress.Commands.add('clearAuthState', () => {
  cy.clearAllCookies();
  cy.clearAllLocalStorage();
  cy.clearAllSessionStorage();

  // Clear any auth-related indexedDB
  cy.window().then((win) => {
    if (win.indexedDB) {
      // Clear common auth-related databases
      try {
        win.indexedDB.deleteDatabase('nextauth');
        win.indexedDB.deleteDatabase('auth-cache');
      } catch (e) {
        // Ignore errors if databases don't exist
      }
    }
  });
});

Cypress.Commands.add('waitForAuth', () => {
  // Wait for authentication-related network requests to complete
  cy.wait(['@authSession', '@authToken'], { timeout: 10000 }).then(() => {
    // Additional wait for any auth state to settle
    cy.wait(500);
  });
});

Cypress.Commands.add('mockAuthenticatedAPI', () => {
  // Mock common API endpoints that require authentication
  const endpoints = [
    '/api/accounts',
    '/api/payments',
    '/api/transactions',
    '/api/fx-rates',
    '/api/user/profile'
  ];

  endpoints.forEach((endpoint) => {
    cy.intercept('GET', endpoint, {
      statusCode: 200,
      body: []
    }).as(`api${endpoint.replace(/[\/\-]/g, '_')}`);

    cy.intercept('POST', endpoint, {
      statusCode: 201,
      body: { success: true }
    });

    cy.intercept('PUT', endpoint, {
      statusCode: 200,
      body: { success: true }
    });

    cy.intercept('DELETE', endpoint, {
      statusCode: 204
    });
  });

  // Mock error scenarios
  cy.intercept('GET', '/api/error-test-401', {
    statusCode: 401,
    body: { error: 'Unauthorized' }
  }).as('unauthorized');

  cy.intercept('GET', '/api/error-test-403', {
    statusCode: 403,
    body: { error: 'Forbidden' }
  }).as('forbidden');

  cy.intercept('GET', '/api/error-test-500', {
    statusCode: 500,
    body: { error: 'Internal Server Error' }
  }).as('serverError');
});

// Add custom assertions for auth testing
Cypress.Commands.add('shouldBeAuthenticated', { prevSubject: false }, () => {
  cy.url().should('not.include', '/login');
  cy.get('[data-testid="user-menu"]', { timeout: 10000 }).should('exist');
});

Cypress.Commands.add('shouldBeUnauthenticated', { prevSubject: false }, () => {
  cy.url().should('include', '/login');
  cy.contains('Welcome to MiniBank').should('be.visible');
});

// Security testing helpers
Cypress.Commands.add('testSecurityHeaders', { prevSubject: false }, (path = '/') => {
  cy.request(path).then((response) => {
    expect(response.headers).to.have.property('x-frame-options');
    expect(response.headers).to.have.property('x-content-type-options');
    expect(response.headers).to.have.property('referrer-policy');
  });
});

Cypress.Commands.add('testCSP', { prevSubject: false }, () => {
  cy.window().then((win) => {
    // Test that CSP is working by trying to inject a script
    const script = win.document.createElement('script');
    script.src = 'data:text/javascript,console.log("CSP bypass attempt")';

    // This should be blocked by CSP
    let cspWorking = false;
    script.onerror = () => {
      cspWorking = true;
    };

    win.document.head.appendChild(script);

    cy.wrap(null).should(() => {
      expect(cspWorking, 'CSP should block inline scripts').to.be.true;
    });
  });
});