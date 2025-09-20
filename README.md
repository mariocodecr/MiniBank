# MiniBank

A production-grade digital banking platform featuring a modern Next.js frontend and event-driven microservices backend, supporting multi-currency operations, foreign exchange processing, and comprehensive financial compliance.

## Project Overview

MiniBank is a full-stack banking platform built with modern technologies and architectural patterns. The project follows a milestone-based development approach:

- **M1**: Core payment and ledger functionality with double-entry accounting system
- **M2**: Event-driven architecture with Kafka, Outbox/Inbox patterns, and saga orchestration  
- **M3**: Multi-currency accounts, real-time FX engine, treasury risk management, and AML compliance

## System Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Next.js       │    │   API Gateway   │    │    Keycloak     │
│   Frontend      │────│   Spring Boot   │────│   (Auth/OIDC)   │
│ (localhost:3000)│    │ (localhost:8080)│    │ (localhost:8081)│
└─────────────────┘    └─────────────────┘    └─────────────────┘
                               │
    ┌──────────────────────────┼──────────────────────────┐
    │                          │                          │
┌───▼────────┐    ┌───────────▼─┐    ┌──────────▼────┐   │
│ Accounts   │    │  Payments   │    │    Ledger     │   │
│ Service    │    │  Service    │    │   Service     │   │
│            │    │             │    │               │   │
│ Multi-FX   │    │ Cross-FX    │    │ Double Entry  │   │
│ Balances   │    │ Saga        │    │               │   │
└────────────┘    └─────────────┘    └───────────────┘   │
       │                  │                  │           │
    ┌──▼──────────────────▼──────────────────▼───────────▼──┐
    │               Kafka Event Bus (localhost:9092)        │
    │  Topics: payment-events, account-events,              │
    │          fx-rate-events, treasury-events,             │
    │          cross-currency-payment-events                │
    └──────────────────────┬─────────────────────────────────┘
                           │
    ┌──────────────────────┼──────────────────────────┐
    │                      │                          │
┌───▼──────────┐    ┌─────▼────────┐    ┌──────────▼────┐
│     FX       │    │   Treasury   │    │Notifications  │
│   Service    │    │   Service    │    │   Service     │
│              │    │              │    │               │
│ Multi-       │    │ Risk         │    │  Email/SMS    │
│ Provider     │    │ Management   │    │  Webhooks     │
└──────────────┘    └──────────────┘    └───────────────┘
       │                     │                     │
┌──────▼─────┐    ┌─────────▼────┐    ┌──────────▼─────┐
│ PostgreSQL │    │    Redis     │    │  Prometheus    │
│ (Primary)  │    │   (Cache)    │    │   Grafana      │
└────────────┘    └──────────────┘    └────────────────┘
```

### Key Architectural Patterns

- **Clean/Hexagonal Architecture**: Domain-driven design with clear separation of concerns
- **Saga Orchestration**: Cross-currency payment workflows with compensation patterns
- **Outbox/Inbox Pattern**: Reliable event publishing and consumption
- **FX Rate Engine**: Multi-provider rate aggregation with circuit breakers
- **OIDC Authentication**: Keycloak integration for secure access

## Tech Stack

| Component | Technology | Version |
|-----------|------------|---------|
| **Backend** | Java | 21 |
| | Spring Boot | 3.4.0 |
| | PostgreSQL | 16+ |
| | Apache Kafka | 3.x |
| | Apache Avro | 1.11.3 |
| | Redis | 7.x |
| **Frontend** | Next.js | 14 |
| | TypeScript | 5.3.3 |
| | React Query | 5.17.19 |
| | Tailwind CSS | 3.4.1 |
| | NextAuth.js | 4.24.6 |
| **Observability** | Prometheus | Latest |
| | Grafana | Latest |
| **Testing** | JUnit | 5 |
| | Testcontainers | Latest |
| | Playwright | 1.41.2 |

## Repository Structure

```
├── build.gradle                    # Backend build configuration
├── gradlew / gradlew.bat           # Gradle wrapper scripts
├── src/main/java/com/minibank/     # Backend microservices
│   ├── accounts/                   # Account management domain
│   ├── payments/                   # Payment processing domain
│   ├── ledger/                     # Double-entry ledger domain
│   ├── fx/                         # Foreign exchange domain
│   ├── treasury/                   # Treasury and risk management
│   ├── compliance/                 # AML and regulatory compliance
│   ├── notifications/              # Notification service
│   └── events/                     # Event schemas and handlers
├── src/main/avro/                  # Avro schemas for events
│   ├── PaymentEvent.avsc
│   ├── AccountEvent.avsc
│   ├── FXRateEvent.avsc
│   └── ...
├── src/main/resources/
│   ├── db/migration/               # Flyway database migrations
│   └── application.properties      # Backend configuration
├── frontend/                       # Next.js frontend application
│   ├── package.json                # Frontend dependencies
│   ├── .env.example                # Environment variables template
│   ├── src/app/                    # Next.js App Router pages
│   ├── src/components/             # Reusable UI components
│   ├── src/lib/api/                # Typed API clients
│   └── README.md                   # Frontend documentation
├── grafana/dashboards/             # Grafana dashboard configurations
├── helm/minibank-services/         # Kubernetes deployment manifests
└── docker/                         # Docker configurations
```

## Getting Started (Local Development)

### Prerequisites

- **Java 21** (OpenJDK or Oracle JDK)
- **Node.js 18+** and **pnpm**
- **Docker** and **Docker Compose**
- **PostgreSQL 16+**
- **Redis 7+**
- **Apache Kafka 3.x**

### Backend Setup

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd minibank
   ```

2. **Start infrastructure services**
   ```bash
   # Start PostgreSQL, Redis, and Kafka
   docker-compose up -d postgres redis kafka
   ```

3. **Build and run the backend**
   ```bash
   # Build the application
   ./gradlew build
   
   # Run database migrations
   ./gradlew flywayMigrate
   
   # Start the application
   ./gradlew bootRun
   ```

   Backend will be available at `http://localhost:8080`

### Frontend Setup

1. **Navigate to frontend directory**
   ```bash
   cd frontend
   ```

2. **Install dependencies**
   ```bash
   pnpm install
   ```

3. **Configure environment variables**
   ```bash
   cp .env.example .env.local
   # Edit .env.local with your configuration
   ```

4. **Start development server**
   ```bash
   pnpm dev
   ```

   Frontend will be available at `http://localhost:3000`

### Environment Variables (.env.local)

```bash
# API Configuration
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
NEXT_PUBLIC_API_VERSION=v1

# Authentication
NEXTAUTH_URL=http://localhost:3000
NEXTAUTH_SECRET=your-nextauth-secret-here

# Keycloak OIDC Configuration
NEXT_PUBLIC_KEYCLOAK_ISSUER=http://localhost:8081/realms/minibank
NEXT_PUBLIC_KEYCLOAK_CLIENT_ID=minibank-web
KEYCLOAK_CLIENT_SECRET=your-keycloak-client-secret
```

## Backend Details

### Microservices Overview

| Service | Endpoints | Description |
|---------|-----------|-------------|
| **Accounts** | `/api/accounts/*` | Multi-currency account management |
| **Payments** | `/api/payments/*` | Payment processing and saga orchestration |
| **Ledger** | `/api/ledger/*` | Double-entry accounting system |
| **FX** | `/api/fx/*` | Exchange rates and currency conversion |
| **Treasury** | `/api/treasury/*` | Risk management and exposure monitoring |
| **Compliance** | `/api/compliance/*` | AML screening and regulatory reporting |

### Key REST Endpoints

```bash
# Accounts Service
GET    /api/accounts                    # List all accounts
POST   /api/accounts                    # Create new account
GET    /api/accounts/{id}/balances      # Get multi-currency balances

# Payments Service
POST   /api/payments                    # Initiate payment
GET    /api/payments/{id}               # Get payment status
POST   /api/payments/cross-currency     # Initiate FX payment

# FX Service
GET    /api/fx/rates                    # Get current exchange rates
POST   /api/fx/rates/lock               # Lock rate for conversion
GET    /api/fx/spreads                  # Get spread configurations

# Treasury Service
GET    /api/treasury/exposures          # Get currency exposures
GET    /api/treasury/alerts             # Get risk alerts
```

### Kafka Topics and Event Types

| Topic | Event Types | Description |
|-------|-------------|-------------|
| `payment-events` | PaymentRequested, PaymentDebited, PaymentCredited | Core payment lifecycle events |
| `account-events` | AccountCreated, BalanceUpdated, CurrencyEnabled | Account management events |
| `fx-rate-events` | RateUpdated, RateLocked, RateExpired | FX rate lifecycle events |
| `cross-currency-payment-events` | ConversionStarted, ConversionCompleted | Cross-currency payment saga events |
| `treasury-events` | ExposureCalculated, ThresholdBreached | Risk monitoring events |

### Database Schema

- **12 Flyway migrations** managing schema evolution
- **PostgreSQL 16** with ACID compliance
- **Key tables**: accounts, account_currency_balances, payments, ledger_entries, fx_rates, payment_sagas
- **Event Store tables**: payment_outbox_events, accounts_inbox_events, ledger_inbox_events

### Testing Strategy

- **Unit Tests**: JUnit 5 for domain logic and business rules
- **Integration Tests**: Testcontainers for database and Kafka interactions
- **Contract Tests**: Pact for service-to-service communication
- **Performance Tests**: Load testing for critical payment flows

```bash
# Run all tests
./gradlew test

# Run integration tests
./gradlew integrationTest

# Generate test coverage
./gradlew test jacocoTestReport
```

## Frontend Details

### Next.js App Router Structure

```
src/app/
├── (auth)/login/               # Authentication pages
├── dashboard/                  # Payment metrics and analytics
├── payments/
│   ├── new/                    # Payment creation form
│   └── [id]/                   # Payment detail and status
├── accounts/                   # Multi-currency account management
├── fx/                         # Foreign exchange rates and conversion
└── admin/
    └── notifications/          # Admin notification settings
```

### Key Pages and Features

| Page | Route | Features |
|------|-------|----------|
| **Dashboard** | `/dashboard` | Payment metrics, FX rates, system health with Recharts |
| **New Payment** | `/payments/new` | Form validation with React Hook Form + Zod |
| **Payment Status** | `/payments/[id]` | Real-time status timeline and ledger entries |
| **Accounts** | `/accounts` | Multi-currency balances and portfolio view |
| **FX Rates** | `/fx` | Live rates, currency converter, rate locking |
| **Admin** | `/admin/notifications` | Notification preferences management |

### Protected Routes with NextAuth/Keycloak

- **OIDC Integration**: Automatic redirect to Keycloak for authentication
- **JWT Session Management**: Secure token handling and refresh
- **Role-based Access**: Protected routes with automatic authorization
- **API Token Injection**: Automatic Bearer token attachment to API requests

### API Client Generation and React Query

```bash
# Generate typed API clients from OpenAPI specs
pnpm codegen

# Individual service generation
pnpm codegen:accounts
pnpm codegen:payments
pnpm codegen:fx
```

- **Type Safety**: Full TypeScript coverage with generated API clients
- **React Query Integration**: Efficient caching and background updates
- **Error Handling**: Comprehensive error boundaries and user feedback
- **Optimistic Updates**: Safe UI updates for better user experience

### Testing

- **Playwright E2E**: End-to-end workflow testing
- **MSW Mocks**: Service worker mocks for offline development
- **Component Testing**: React Testing Library for UI components

```bash
# Run E2E tests
pnpm test:e2e

# Run component tests
pnpm test

# Type checking
pnpm typecheck
```

## Observability & Operations

### Prometheus/Grafana Dashboards

Access dashboards at `http://localhost:3000` (admin/admin)

- **FX & Treasury Dashboard**: Real-time exchange rates, currency exposure monitoring, risk alerts
- **Payment Metrics**: Success rates, P95 latency, volume trends, saga completion rates
- **System Health**: JVM metrics, database connections, cache hit rates

### Key Business Metrics

```bash
# Payment Metrics
minibank.payments.processed.total
minibank.payments.success.rate
minibank.payments.cross.currency.total

# FX Metrics  
minibank.fx.rates.updated.total
minibank.fx.conversions.completed.total
minibank.fx.provider.health.gauge

# Treasury & Risk
minibank.treasury.exposure.net.gauge
minibank.treasury.alerts.generated.total
minibank.treasury.threshold.breaches.total
```

### Operational Monitoring

- **Consumer Lag Monitoring**: Kafka consumer group lag tracking
- **DLQ Handling**: Dead letter queue processing for failed events
- **Circuit Breaker Patterns**: FX provider failover monitoring
- **Health Checks**: `/actuator/health` endpoints for service monitoring

## Security & Compliance

### Keycloak OIDC Flow

1. **User Login**: Redirect to Keycloak authentication
2. **Token Exchange**: OIDC authorization code flow
3. **JWT Validation**: Backend validates JWT tokens
4. **Token Refresh**: Automatic refresh token handling
5. **Secure Logout**: Proper session termination

### AML & Cross-Border Compliance

- **Multi-Provider Screening**: Integration with multiple AML data providers
- **Sanctions List Matching**: Real-time screening against OFAC and other lists
- **PEP Screening**: Politically Exposed Persons identification
- **Risk Scoring**: Automated risk assessment and case management
- **Regulatory Reporting**: Automated compliance report generation

### Data Protection

- **Encryption at Rest**: Sensitive data encryption in PostgreSQL
- **PII Masking**: Personal information protection in logs and metrics
- **Audit Trails**: Comprehensive transaction and access logging
- **GDPR Compliance**: Data retention and deletion policies

## Deployment

### Docker Images

```bash
# Build backend Docker image
./gradlew bootBuildImage

# Build frontend Docker image
cd frontend && docker build -t minibank-frontend .
```

### Kubernetes with Helm

```bash
# Deploy to Kubernetes
helm install minibank ./helm/minibank-services \
  --set backend.image.tag=latest \
  --set frontend.image.tag=latest \
  --set keycloak.enabled=true
```

### Typical Production Commands

```bash
# Scale backend services
kubectl scale deployment minibank-backend --replicas=3

# Update application
helm upgrade minibank ./helm/minibank-services \
  --set backend.image.tag=v1.2.0

# Check service health
kubectl get pods -l app=minibank
curl https://api.minibank.com/actuator/health
```

## Contributing

### Code Style Guidelines

- **Java**: Google Java Style Guide with Clean Architecture principles
- **TypeScript**: ESLint + Prettier with strict TypeScript configuration
- **Commits**: Conventional commit messages (`feat:`, `fix:`, `docs:`)
- **Testing**: Comprehensive test coverage for new features

### Pull Request Checklist

- [ ] All tests pass (`./gradlew test` and `pnpm test`)
- [ ] Code follows style guidelines (`./gradlew spotlessCheck` and `pnpm lint`)
- [ ] Documentation updated for new features
- [ ] OpenAPI specs updated for API changes
- [ ] Database migrations included if schema changes
- [ ] Frontend types regenerated if backend API changes (`pnpm codegen`)

### Regenerating OpenAPI Types

```bash
# After backend API changes, regenerate frontend types
cd frontend
pnpm codegen

# Or individual services
pnpm codegen:payments
pnpm codegen:accounts
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

**Built with ❤️ for modern banking infrastructure**