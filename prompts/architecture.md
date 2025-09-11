architecture-backend.md — Prompt

You are an Enterprise Software Architect (20+ yrs) specialized in Java 21, Spring Boot 3, Distributed Systems, and Financial Services. Design the MiniBank backend as a production-grade microservices system. Produce a clear, opinionated architecture spec with diagrams (ASCII), technology choices, interfaces, and NFRs ready for implementation.

Objectives

Build internal account transfers (CRC/USD) with double-entry ledger, strong auditability, and event-driven processing.

Optimize for consistency + resilience using Sagas, Outbox/Inbox, Idempotency, and Optimistic Locking.

Backend-only scope; no UI. Deployment-ready for Docker/Kubernetes.

Style & Constraints

Language/Frameworks: Java 21, Spring Boot 3, Spring Data JPA, Hibernate, Lombok.

APIs: Choose REST for microservice-to-microservice and external API contracts (OpenAPI-first, JSON). Justification: high interoperability, simpler idempotency semantics, easier observability and governance for a banking core; GraphQL optional behind the API Gateway for read aggregation only (non-blocking, not required for MVP).

Database: PostgreSQL 16 (SQL, ACID), Flyway for migrations, UUID keys.

Messaging: Apache Kafka (exactly-once via Outbox), Dead Letter Topics.

Caching/Locks: Redis (idempotency keys, reservation/locks, ephemeral state).

Security: Keycloak (OIDC), mTLS (service mesh optional), Vault for secrets, OWASP ASVS L2.

Observability: Micrometer + Prometheus, OpenTelemetry Traces, Loki logs, Grafana dashboards.

Build/DevOps: Gradle, Testcontainers, JUnit 5, WireMock, Pact (consumer/provider), Dockerfiles, K8s manifests/Helm, GitHub Actions CI/CD.

Code Principles: Clean Architecture, SOLID, Hexagonal ports/adapters, Code-First domain entities, Factory Pattern for data/test object creation.

Services (bounded contexts)

auth-users: identity (delegated to Keycloak) + user profile (lightweight DB for non-auth attributes).

accounts: create/close accounts, query balances, versioned updates with optimistic locking.

payments (orchestrator): initiates and coordinates transfers, idempotency, compensations.

ledger: append-only double-entry events (DEBIT/CREDIT) per account; rebuildable projections.

notifications: email/SMS/Push via async queue.

fx (optional later): CRC↔USD rates with provider + cache + circuit breaker.

Domain Model (core)

User(id: UUID, email, kycLevel, status)

Account(id: UUID, userId, currency, balance: Money, status, version: Long)

Payment(id, requestId, fromAccount, toAccount, amount: Money, currency, status, createdAt)

LedgerEntry(id, paymentId, entryType(DEBIT|CREDIT), accountId, amount: Money, occurredAt)

API Contracts (REST, OpenAPI spec required)

accounts

POST /accounts — create

GET /accounts/{id} — details

GET /users/{userId}/accounts — list

internal: POST /accounts/{id}/reserve (idempotent)

internal: POST /accounts/{id}/post (apply debit/credit)

payments

POST /payments {requestId, from, to, amount, currency} → 202 Accepted

GET /payments/{id}

ledger

GET /ledger/payments/{paymentId}/entries

notifications

POST /notifications/email|sms (internal async producer)

Events (Kafka Topics)

payments.requested → payments.debited → payments.credited → payments.completed | payments.failed

ledger.entry.created

Use Outbox Pattern in payments and accounts to ensure atomic DB + event publishing. Consumers maintain Inbox table for dedupe.

Saga (ASCII)
[Client] --POST /payments--> [Gateway] -> [payments]
   payments(REQUESTED)
      | reserve debit (idempotent)
      v
   [accounts] --OK--> payments(DEBITED)
      | credit beneficiary
      v
   [accounts] --OK--> payments(CREDITED)
      | write ledger entries (DEBIT/CREDIT)
      v
   [ledger] --OK--> payments(COMPLETED) -> [notifications]
      | failure at any step triggers compensation (reverse debit)
Persistence & Transactions

accounts: Postgres; Account row updated with version (optimistic). Business invariants in domain services.

ledger: append-only table(s) + projections for queries; guarantee sum(DEBIT)=sum(CREDIT) per payment.

Monetary values stored in minor units (e.g., cents) as BIGINT, currency in VARCHAR(3).

Idempotency & Consistency

requestId (UUID) required in POST /payments. Store in Redis with TTL and in DB for dedupe.

Outbox ensures exactly-once delivery semantics when combined with transactional producers.

Non-Functional Requirements

P95 < 300ms for POST /payments under 500 rps; 99.9% availability for Gateway + Payments.

Recovery RTO < 15m, RPO < 5m; encrypted at rest/in transit.

Deliverables

C4 Diagrams (Context, Container, Component), OpenAPI specs per service, ERDs, Kafka topic contracts (Avro/JSON Schema), SLOs, Runbooks, Helm charts, Grafana dashboards (JSON).