# MiniBank Frontend

A modern, production-ready Next.js frontend for the MiniBank digital banking platform. Built with TypeScript, Tailwind CSS, and shadcn/ui components.

## Features

- **🔐 Authentication**: Keycloak OIDC integration with NextAuth.js
- **💳 Payment Management**: Create, track, and manage payments with real-time status updates
- **🏦 Multi-Currency Accounts**: Support for 8+ currencies with balance tracking
- **💱 Foreign Exchange**: Real-time FX rates, currency conversion, and rate locking
- **📊 Dashboard**: Comprehensive metrics and analytics
- **🎨 Modern UI**: Clean, responsive design with shadcn/ui components
- **🔒 Type Safety**: Full TypeScript coverage with generated API clients
- **⚡ Performance**: Optimized with React Query for efficient data fetching

## Tech Stack

- **Framework**: Next.js 14 (App Router)
- **Language**: TypeScript
- **Styling**: Tailwind CSS + shadcn/ui
- **State Management**: React Query (TanStack Query)
- **Authentication**: NextAuth.js + Keycloak
- **Forms**: React Hook Form + Zod validation
- **Charts**: Recharts
- **Icons**: Lucide React

## Prerequisites

- Node.js 18+ and pnpm (or npm/yarn)
- Running MiniBank backend at `http://localhost:8080`
- Keycloak instance at `http://localhost:8081` (optional for demo)

## Environment Setup

1. **Copy environment variables**:
   ```bash
   cp .env.example .env.local
   ```

2. **Configure environment variables**:
   ```bash
   # API Configuration
   NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
   
   # Authentication
   NEXTAUTH_URL=http://localhost:3000
   NEXTAUTH_SECRET=your-nextauth-secret-here
   
   # Keycloak OIDC
   NEXT_PUBLIC_KEYCLOAK_ISSUER=http://localhost:8081/realms/minibank
   NEXT_PUBLIC_KEYCLOAK_CLIENT_ID=minibank-web
   KEYCLOAK_CLIENT_SECRET=your-keycloak-client-secret
   ```

## Installation & Development

1. **Install dependencies**:
   ```bash
   pnpm install
   ```

2. **Generate API types** (optional, if OpenAPI specs are available):
   ```bash
   pnpm run codegen
   ```

3. **Start development server**:
   ```bash
   pnpm dev
   ```

4. **Open browser**: Navigate to `http://localhost:3000`

## Available Scripts

```bash
# Development
pnpm dev                 # Start development server
pnpm build              # Build for production
pnpm start              # Start production server

# Code Quality
pnpm lint               # Run ESLint
pnpm typecheck          # Run TypeScript compiler check
pnpm format             # Format code with Prettier
pnpm format:check       # Check code formatting

# API Code Generation
pnpm codegen            # Generate all API types from OpenAPI specs
pnpm codegen:accounts   # Generate accounts API types
pnpm codegen:payments   # Generate payments API types
pnpm codegen:ledger     # Generate ledger API types
pnpm codegen:fx         # Generate FX API types

# Testing (when configured)
pnpm test               # Run Jest tests
pnpm test:watch         # Run tests in watch mode
pnpm test:e2e          # Run Playwright E2E tests
```

## Project Structure

```
src/
├── app/                          # Next.js App Router pages
│   ├── (auth)/                   # Authentication routes
│   ├── dashboard/                # Dashboard and analytics
│   ├── payments/                 # Payment management
│   │   ├── new/                  # Create payment form
│   │   └── [id]/                 # Payment detail view
│   ├── accounts/                 # Account management
│   ├── fx/                       # Foreign exchange
│   └── admin/                    # Administrative functions
├── components/                   # Reusable UI components
│   ├── ui/                       # shadcn/ui base components
│   ├── layout/                   # Layout components (Sidebar, Header)
│   ├── auth/                     # Authentication components
│   └── dashboard/                # Dashboard-specific components
├── lib/                          # Utilities and configurations
│   ├── api/                      # API clients and types
│   │   ├── base.ts              # Base API client with auth
│   │   ├── types.ts             # Shared type definitions
│   │   ├── accounts.ts          # Accounts API client
│   │   ├── payments.ts          # Payments API client
│   │   ├── ledger.ts            # Ledger API client
│   │   └── fx.ts                # FX API client
│   ├── auth.ts                  # Authentication configuration
│   └── utils.ts                 # Utility functions
├── hooks/                        # Custom React hooks
├── types/                        # TypeScript type definitions
└── utils/                        # Additional utilities
```

## API Integration

The frontend uses typed API clients generated from OpenAPI specifications:

### API Clients

- **AccountsClient**: Multi-currency account operations
- **PaymentsClient**: Payment creation and tracking
- **LedgerClient**: Double-entry ledger queries
- **FXClient**: Exchange rates and currency conversion

### Authentication

All API requests automatically include JWT Bearer tokens from NextAuth sessions:

```typescript
// Automatic token injection
const response = await paymentsClient.createPayment(paymentData);
```

### Error Handling

Comprehensive error handling with user-friendly messages:

```typescript
// Automatic error handling with toast notifications
const mutation = useMutation({
  mutationFn: paymentsClient.createPayment,
  onError: (error) => {
    toast.error('Payment failed', {
      description: error.message
    });
  }
});
```

## Key Features

### 🔐 Authentication Flow

1. **Login**: Users can sign in through Keycloak OIDC integration
2. **Self-Registration**: New users can create accounts via Keycloak registration
3. **Route Protection**: Middleware protects routes (`/dashboard`, `/payments`, `/accounts`, etc.)
4. **Token Management**: JWT tokens with automatic refresh and proper expiration handling
5. **Logout**: Secure logout with Keycloak session termination
6. **API Integration**: All API requests include Bearer tokens with 401/403 error handling
7. **Graceful Degradation**: Clear error messages when Keycloak is unavailable

### 💳 Payment Processing

1. **Create Payment**: Form with validation and account selection
2. **Real-time Status**: Live updates of payment progress
3. **Timeline View**: Visual representation of payment stages
4. **Ledger Integration**: View double-entry accounting records

### 🏦 Multi-Currency Support

1. **Account Overview**: All currency balances in one view
2. **Balance Tracking**: Available vs reserved amounts
3. **Currency Management**: Enable/disable currencies per account
4. **Portfolio View**: Total value across all currencies

### 💱 Foreign Exchange

1. **Live Rates**: Real-time exchange rate display
2. **Currency Converter**: Interactive conversion tool
3. **Rate Locking**: Secure rate for guaranteed conversions
4. **Provider Status**: Monitor FX provider health

### 📊 Dashboard Analytics

1. **Payment Metrics**: Success rates, latency, volume
2. **FX Metrics**: Conversion volumes, spreads, provider status
3. **Interactive Charts**: Payment volume and success trends
4. **Real-time Updates**: Auto-refreshing data

## Assumptions & Notes

Since some backend components may still be in development, the frontend includes:

1. **Mock Data**: Fallback data for development and testing
2. **Error Boundaries**: Graceful handling of API failures
3. **Loading States**: Proper loading indicators throughout
4. **Optimistic Updates**: Where safe, immediate UI updates

## Development Notes

### Code Generation

If OpenAPI specs are available, run `pnpm codegen` to generate typed API clients. Otherwise, the current type definitions are based on backend controller analysis.

### Keycloak Configuration

For local development without Keycloak:
1. Comment out Keycloak provider in `src/app/api/auth/[...nextauth]/route.ts`
2. Add a development provider or mock authentication

### CORS Configuration

If you encounter CORS issues, ensure the backend allows `http://localhost:3000` in its CORS configuration.

## Deployment

### Production Build

```bash
pnpm build
pnpm start
```

### Environment Variables

Ensure all production environment variables are set:

- `NEXTAUTH_URL`: Production frontend URL
- `NEXT_PUBLIC_API_BASE_URL`: Production backend URL
- `KEYCLOAK_CLIENT_SECRET`: Production Keycloak secret
- `NEXTAUTH_SECRET`: Secure random string

### Docker Deployment

```dockerfile
FROM node:18-alpine
WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production
COPY . .
RUN npm run build
EXPOSE 3000
CMD ["npm", "start"]
```

## Contributing

1. **Code Style**: Follow the existing TypeScript/React patterns
2. **Components**: Use shadcn/ui components for consistency
3. **API Integration**: Follow the established client patterns
4. **Error Handling**: Implement comprehensive error boundaries
5. **Testing**: Add tests for new components and features

## Troubleshooting

### Common Issues

1. **Authentication Errors**: Check Keycloak configuration and client secrets
2. **API Connection**: Verify backend is running on port 8080
3. **CORS Issues**: Check backend CORS configuration
4. **Build Errors**: Run `pnpm typecheck` to identify TypeScript issues

### Debug Mode

Enable debug logging:

```bash
NEXT_PUBLIC_DEBUG_API=true
NEXT_PUBLIC_DEBUG_AUTH=true
```

## License

This project is part of the MiniBank banking platform and follows the same licensing terms.