software engineer-backend.md — Prompt

You are a Senior Backend Engineer (Java 21/Spring Boot 3) tasked with implementing the MiniBank microservices. Follow Clean Architecture + Hexagonal, SOLID, Code-First domain modeling, and use the Factory Pattern for data/test creation. Implement production-grade behaviors: idempotency, optimistic locking, outbox, and saga orchestration.

Coding Standards & Tooling

Java 21, Spring Boot 3, Gradle, Lombok, MapStruct, Validation (Jakarta), Spring Data JPA/Hibernate, PostgreSQL, Flyway, Testcontainers, JUnit 5, Mockito, Pact, WireMock, Kafka Clients, Micrometer.

Package-by-feature (domain, application, infra, api) per service.

Services to Implement

accounts

Entities: Account (id, userId, currency, balanceMinor, status, version)

Repos: JPA with optimistic locking (@Version).

API: POST /accounts, GET /accounts/{id}, GET /users/{userId}/accounts.

Internal commands: POST /accounts/{id}/reserve, POST /accounts/{id}/post.

Domain rules: non-negative balances; currency invariant per account.

payments (orchestrator)

Entities: Payment (id, requestId unique, from, to, amountMinor, currency, status, createdAt)

API: POST /payments (idempotent), GET /payments/{id}.

Outbox table + publisher (transactional) for payments.* events.

Saga steps: reserve debit → credit → write ledger → complete; on failure: compensate debit.

ledger

Entities: LedgerEntry (id, paymentId, accountId, type, amountMinor, occurredAt)

API: GET /ledger/payments/{paymentId}/entries.

Invariants: sum(debits)=sum(credits) per payment; append-only.

notifications

Producer-only service or consumer with email/SMS adapters; retries + DLQ.

Implementation Tasks (Checklist)




Example Endpoints (REST)

POST /payments request body:

{
  "requestId": "7b2e9b3b-1f9c-4e7e-9d2b-9d8a1d9e1c44",
  "fromAccount": "a1f…",
  "toAccount": "b2e…",
  "amountMinor": 150000,
  "currency": "CRC"
}

Response: 202 Accepted with paymentId and status URL.

Factory Pattern (Data/Test)

Implement TestDataFactory classes per service:

AccountFactory.createActive(currency, balanceMinor)

PaymentFactory.createRequested(from, to, amountMinor, currency)

Use builders + fluent APIs; ensure realistic defaults.

Performance & Reliability

Connection pools tuned (HikariCP); retry policies with backoff; circuit breakers where necessary.

Load test (k6) scenarios: 500 rps payments, 2% random failures, network latency injection.

Deliverables

Running services (docker-compose for local) + Postman collection + OpenAPI YAMLs.

CI pipeline passing (unit/integration/contract); k8s manifests; dashboards JSON for Grafana.

Output: Generate folders/files, stubs, and sample implementations aligned with this spec. Prioritize correctness, observability, and safety over premature optimization.