# 🏦 MiniBank - Digital Banking Platform

[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html)
[![Spring Boot 3.4.0](https://img.shields.io/badge/Spring%20Boot-3.4.0-green.svg)](https://spring.io/projects/spring-boot)
[![Next.js 14](https://img.shields.io/badge/Next.js-14-black.svg)](https://nextjs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.3.3-blue.svg)](https://www.typescriptlang.org/)
[![Security Rating](https://img.shields.io/badge/Security-A+-brightgreen.svg)](https://github.com/mariocodecr/MiniBank)

A production-grade digital banking platform featuring a modern **Next.js frontend** and **event-driven microservices backend**, supporting multi-currency operations, foreign exchange processing, and comprehensive financial compliance.

## 🚀 Project Overview

MiniBank is a comprehensive full-stack banking platform built with modern technologies and architectural patterns. The system demonstrates enterprise-grade financial software development with:

- **🔐 Security-First Design**: NextAuth.js + Keycloak OIDC integration with JWT token management
- **💱 Multi-Currency Support**: Real-time FX rates, cross-currency payments, and treasury management
- **📊 Event-Driven Architecture**: Kafka-based microservices with saga orchestration patterns
- **🏛️ Double-Entry Accounting**: Comprehensive ledger system ensuring financial data integrity
- **⚡ Real-Time Processing**: Live payment tracking, instant balance updates, and rate notifications
- **📈 Observability**: Prometheus metrics with Grafana dashboards for operational insights

### Development Milestones

- ✅ **M1**: Core payment and ledger functionality with double-entry accounting system
- ✅ **M2**: Event-driven architecture with Kafka, Outbox/Inbox patterns, and saga orchestration
- ✅ **M3**: Multi-currency accounts, real-time FX engine, treasury risk management, and AML compliance

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
│   ├── tests/                      # E2E and component tests
│   └── README.md                   # Frontend documentation
├── grafana/                        # Grafana dashboard configurations
│   ├── dashboards/                 # JSON dashboard definitions
│   └── prometheus.yml              # Prometheus configuration
├── docker-compose.yml              # Local development infrastructure
└── .claude/                        # Claude Code configuration
```

## 🚀 Getting Started (Local Development)

### Prerequisites

Ensure you have the following installed on your development machine:

- **Java 21** (OpenJDK or Oracle JDK) - Required for backend compilation and runtime
- **Node.js 18+** and **pnpm** - Frontend package management and build tools
- **Docker** and **Docker Compose** - Container orchestration for infrastructure services
- **Git** - Version control system

### 🐳 Quick Start with Docker

The fastest way to get MiniBank running locally:

```bash
# Clone the repository
git clone https://github.com/mariocodecr/MiniBank.git
cd MiniBank

# Start all infrastructure services
docker-compose up -d

# Build and run the backend
./gradlew bootRun

# In a new terminal, start the frontend
cd frontend && pnpm install && pnpm dev
```

🎉 **You're ready!** Visit:
- **Frontend**: http://localhost:3000
- **Backend API**: http://localhost:8080
- **Keycloak Admin**: http://localhost:8082 (admin/admin)

### 🔧 Manual Setup

#### Backend Setup

1. **Clone and navigate to repository**
   ```bash
   git clone https://github.com/mariocodecr/MiniBank.git
   cd MiniBank
   ```

2. **Start infrastructure services**
   ```bash
   # Start PostgreSQL, Redis, Kafka, and Keycloak
   docker-compose up -d postgres redis kafka zookeeper keycloak

   # Wait for services to be ready (check with docker-compose ps)
   ```

3. **Configure environment**
   ```bash
   # Backend configuration is in src/main/resources/application.properties
   # Default configuration works with Docker Compose setup
   ```

4. **Build and run the backend**
   ```bash
   # Clean build with tests
   ./gradlew clean build

   # Start the Spring Boot application
   ./gradlew bootRun
   ```

   🌐 Backend will be available at `http://localhost:8080`

#### Frontend Setup

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

   # Generate secure secrets
   openssl rand -base64 32  # Use output for NEXTAUTH_SECRET
   openssl rand -hex 32     # Use output for KEYCLOAK_CLIENT_SECRET
   ```

4. **Start development server**
   ```bash
   pnpm dev
   ```

   🌐 Frontend will be available at `http://localhost:3000`

### 🔐 Environment Variables (.env.local)

Create a `.env.local` file in the `frontend/` directory with the following configuration:

```bash
# API Configuration
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
NEXT_PUBLIC_API_VERSION=v1

# Authentication - REQUIRED
NEXTAUTH_URL=http://localhost:3000
# Generate with: openssl rand -base64 32
NEXTAUTH_SECRET=your-secure-nextauth-secret-here-minimum-32-characters

# Keycloak OIDC Configuration - REQUIRED
NEXT_PUBLIC_KEYCLOAK_ISSUER=http://localhost:8082/realms/minibank
NEXT_PUBLIC_KEYCLOAK_CLIENT_ID=minibank-web
# Generate with: openssl rand -hex 32
KEYCLOAK_CLIENT_SECRET=your-secure-keycloak-client-secret

# Development Environment
NODE_ENV=development
NEXT_TELEMETRY_DISABLED=1

# Security Configuration
NEXTAUTH_SESSION_MAX_AGE=86400  # 24 hours
NEXTAUTH_COOKIE_SECURE=false   # Set to true in production
```

> ⚠️ **Security Note**: Never commit `.env.local` files to version control. The secrets shown above are examples only.

### 👥 Demo Users & Authentication

Once Keycloak is running, you can access the application with these pre-configured demo users:

| Username | Password | Role | Description |
|----------|----------|------|-------------|
| `testuser` | `testuser` | **Customer** | Standard user with payment and account access |
| `mario` | `mario` | **Customer** | Standard user for testing multi-user scenarios |
| `admin` | `admin` | **Administrator** | Admin access to all features and settings |

**Login Process**:
1. Visit http://localhost:3000
2. Click "Sign In"
3. You'll be redirected to Keycloak
4. Use any of the credentials above
5. After successful authentication, you'll return to the MiniBank dashboard

**Keycloak Admin Console**: Access at http://localhost:8082 with `admin/admin` for user management.

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

## 📊 Observability & Operations

### Prometheus/Grafana Dashboards

Access monitoring dashboards at `http://localhost:3001` (admin/admin)

**Available Dashboards**:
- **🏦 MiniBank M1 Dashboard**: Core payment metrics, system health, ledger statistics
- **💱 FX & Treasury Dashboard**: Real-time exchange rates, currency exposure monitoring, risk alerts
- **🔍 Payment Metrics**: Success rates, P95 latency, volume trends, saga completion rates
- **⚡ System Health**: JVM metrics, database connections, cache hit rates, API response times

**Pre-configured Grafana Features**:
- Real-time payment success rate monitoring
- Cross-currency payment saga tracking
- FX rate provider health checks
- Account balance distribution analytics
- System resource utilization graphs

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

## ✨ Key Features Showcase

### 🔐 Secure Authentication
- **NextAuth.js + Keycloak**: Enterprise-grade OIDC authentication flow
- **JWT Token Management**: Secure token refresh with automatic retry logic
- **Role-Based Access Control**: Different UI views based on user permissions

### 💳 Account Management
- **Multi-Currency Accounts**: Support for USD, EUR, GBP, CRC, and more currencies
- **Real-Time Balances**: Live balance updates with WebSocket connections
- **Transaction History**: Comprehensive audit trail with filters and search
- **Account Settings**: Personalized preferences and security controls

### 💸 Payment Processing
- **Instant Payments**: Same-currency transfers with immediate settlement
- **Cross-Currency Payments**: FX conversion with real-time rate locking
- **Payment Status Tracking**: Real-time payment lifecycle with visual timeline
- **Saga Orchestration**: Reliable distributed transaction handling

### 📈 Financial Dashboard
- **Payment Analytics**: Success rates, volume trends, and performance metrics
- **FX Rate Monitoring**: Live exchange rates with historical charts
- **System Health**: Real-time monitoring of all microservices
- **Treasury Management**: Currency exposure and risk monitoring

### 🛡️ Security & Compliance
- **AML Screening**: Automated anti-money laundering checks
- **Audit Trails**: Comprehensive logging for regulatory compliance
- **Data Protection**: GDPR-compliant data handling and retention
- **Rate Limiting**: API protection against abuse and DoS attacks

## 🤝 Contributing

We welcome contributions! Please see our contributing guidelines:

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

### Development Commands

```bash
# Backend
./gradlew build           # Full build with tests
./gradlew test            # Run unit tests
./gradlew bootRun         # Start application
./gradlew spotlessCheck   # Check code style

# Frontend
pnpm install              # Install dependencies
pnpm dev                  # Start development server
pnpm build                # Production build
pnpm test                 # Run tests
pnpm lint                 # Check code style
pnpm typecheck            # TypeScript validation
```

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 📞 Support & Contact

- **Issues**: [GitHub Issues](https://github.com/mariocodecr/MiniBank/issues)
- **Discussions**: [GitHub Discussions](https://github.com/mariocodecr/MiniBank/discussions)
- **Email**: mario@minibank.dev

---

**🏦 Built with ❤️ for modern banking infrastructure by [@mariocodecr](https://github.com/mariocodecr)**

> *"Demonstrating enterprise-grade financial software architecture with modern technologies and best practices."*