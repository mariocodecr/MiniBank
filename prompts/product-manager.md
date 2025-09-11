product-manager-backend.md — Prompt

You are a Product Manager Director for a fintech core team. Write a backlog and delivery plan for the MiniBank backend. Focus on user value, risk reduction, and release slices that ship end-to-end value quickly. Include explicit acceptance criteria, non-functional requirements, and tracking KPIs. Avoid frontend scope.

Product Goals

Enable internal transfers with clear status, reliability, and notifications.

Provide auditable financial records (double-entry ledger) for compliance.

Personas

Retail User (sender/receiver)

Ops Analyst (conciliation, auditor)

Success Metrics (KPIs)

Payment success rate ≥ 99.5%

Latency P95 POST /payments < 300 ms

Time to consistency (ledger vs balances) < 5 s

% idempotent dedupes (monitor) and compensation rate (should be < 0.2%)

Release Plan (Milestones → Epics → Stories)
M0 — Foundations (2 weeks)

Epics: Platform readines, Auth/Users, Accounts CRUD, Observability baseline.

Stories & AC

Observability baseline: Prometheus, metrics for request rate, error rate; traces across gateway→payments→accounts. AC: dashboards deployed; golden signals visible.

Accounts CRUD: POST/GET endpoints; balances default 0; optimistic locking enabled. AC: concurrency test proves no lost updates.

Security: OIDC integration; service-to-service auth via token. AC: all endpoints require scopes.

M1 — Payments (monocurrency, synchronous happy-path) (2–3 weeks)

Epics: Initiate payment, debit/credit, basic ledger.

Initiate payment: POST /payments accepts requestId. AC: duplicated requestId returns same result (idempotent).

Debit/Credit accounts: atomic in orchestrator with fallback. AC: cannot overdraft; validation errors clear.

Ledger entries: DEBIT/CREDIT persisted and queryable by paymentId. AC: sum(debits)=sum(credits).

M2 — Event-Driven & Reliability (3 weeks)

Epics: Kafka + Outbox/Inbox, Sagas, Notifications, Retries.

Outbox enabled: events emitted atomically on state changes. AC: chaos test (network flap) still delivers events once.

Saga compensation: credit failure reverses debit. AC: automated test proves compensation within 5s.

Notifications: email on COMPLETED. AC: retries with backoff; DLQ monitored.

M3 — Multi-Currency & FX (optional) (2–3 weeks)

Epics: currency conversion, rate provider with cache + circuit breaker.

Multi-currency payments: from CRC to USD. AC: FX applied using last good rate; audit trail records rate source.

Non-Functional Backlog

Backups/restore runbook; load tests (k6); penetration tests; RBAC scopes; data retention policies (GDPR-like).

Reporting & Ops

Dashboards: funnel (requested→completed→failed), ledger consistency, event lag, DLQ depth.

Alerts: P95 breach, saga timeouts, outbox lag, compensation spike.

Definition of Done

OpenAPI approved, Pact tests green, SLO dashboards updated, runbooks linked, Testcontainers CI passing, Helm chart release cut.