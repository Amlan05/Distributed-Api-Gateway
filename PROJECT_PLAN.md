# Distributed API Gateway project plan

Build a portfolio system incrementally, with reproducible tests, intentional failure handling, observability, and measured performance. Milestone 1 is complete and Milestone 2 is authorized. The user requested Hibernate-managed local schema generation instead of SQL migrations for this phase.

## Target architecture

```text
Client -> API Gateway -> User service -> User PostgreSQL database
                      -> Order service -> Order PostgreSQL database
                      -> Product service -> Product PostgreSQL database

Gateway -> Redis (shared token-bucket state across gateway replicas)
Gateway -> JWT verification, routing, timeouts, Resilience4j circuit breakers
Services -> JWT verification and business authorization
Order/Product services <-> Kafka (events, retries, idempotent consumers)
Applications -> Prometheus -> Grafana
GitHub Actions -> tests/build/images -> deployment checks
Docker Compose -> reproducible local system
Kubernetes -> deployments, HPA, canary traffic and rollback
k6 -> measured baseline, overload and recovery experiments
```

Services own their data; no cross-service database joins. A local PostgreSQL instance may host separate databases. Choose a compatible Spring Cloud release train when introducing the gateway. Keep the gateway and servlet services as separate applications. Milestone 1's current artifact is the initial user API despite its existing distributed-api-gateway name; a later extraction will give it a dedicated service module.

## Technology stack

Current package convention: group by feature, then layer. The implemented user API lives in com.gateway.user.controller/service/repository/model/dto/exception, with application-wide error advice in com.gateway.shared.exception. Tests mirror the feature's packages. Future services will retain their own application boundaries; package organization does not create independently deployed microservices.

| Area | Technology and current status |
| --- | --- |
| Runtime/build | Java 17, Maven wrapper 3.10.0, existing Spring Boot 4.1.1 |
| Current HTTP API | Spring MVC, embedded Tomcat, Jackson, Jakarta Bean Validation |
| Current tests | JUnit, Mockito, AssertJ, MockMvc, Spring Boot context tests |
| Routing/security | Spring Cloud Gateway, Spring Security JWT resource servers (planned) |
| Storage | PostgreSQL 17.11, Spring Data JPA/Hibernate; local automatic schema updates selected by the user; migrations deferred |
| Distributed protection | Redis token bucket, Resilience4j (planned) |
| Events | Kafka, transactional outbox/idempotency where required (planned) |
| Reproducibility | Docker Compose for local PostgreSQL; Testcontainers for isolated PostgreSQL tests; full-system packaging remains planned |
| Delivery/operations | GitHub Actions, Kubernetes HPA/canary (planned) |
| Measurement | Actuator/Micrometer, Prometheus, Grafana, k6 (planned) |

Future library/image versions will be selected and pinned against the then-current Java/Spring compatibility requirements, rather than guessed in advance.

## Milestones and acceptance criteria

| Phase | Depends on | Deliverable and acceptance criteria |
| --- | --- | --- |
| 0: Inspect and prepare | Existing generated project | Inspect source/config/toolchain/Git, verify Java 17 compatibility, preserve working setup, create project documents. |
| 1: First REST API | 0 | All five /api/users endpoints, DTO validation, constructor injection, atomic in-memory storage, 201 + Location, 204 deletion, consistent 400/404 errors; service/controller/concurrency/integration tests and clean Maven build pass; PowerShell checks documented. Stop for approval. |
| 2: Durable user storage | 1; approved | PostgreSQL replaces volatile storage; Hibernate creates/updates the local schema as requested (migrations deferred); data survives restart; persistence constraints and transactions documented; Testcontainers CRUD/concurrency/rollback tests pass. Preserve duplicate-email behavior for this phase. |
| 3: Separate services | 2 | User, Product, and Order applications run independently on separate ports with owned databases; define API contracts; service tests pass; no shared mutable storage. |
| 4: Gateway routing | 3 | Separate gateway with compatible Spring Cloud BOM routes all three services; configurable targets; route integration tests and unavailable-upstream behavior documented. |
| 5: JWT and authorization | 4 | JWT validation with issuer/audience/expiry, explicit user roles/ownership; services enforce authorization; tests cover 401/403, forged/expired tokens, and direct-service access. No secrets committed. |
| 6: Distributed rate limiting | 5 | Redis token bucket shared by at least two gateway replicas; tests prove refill/burst rules and aggregate limits; 429 and retry metadata documented; Redis outage policy explicit. |
| 7: Resilience | 6 | Bounded upstream timeouts and circuit breakers; controlled downstream failure opens breaker and recovery closes it; fallback never reports a failed write as success; retry policies avoid duplicate writes. |
| 8: Asynchronous events | 7 | Kafka order/product flow with versioned contracts, idempotent processing, bounded retries/dead-letter handling; tests reproduce duplicates and broker outage; address DB/event atomicity with an outbox when needed. |
| 9: Local full-system packaging | 8 | Dockerfiles and Compose start apps + PostgreSQL + Redis + Kafka with health checks and documented configuration; clean checkout can run system; smoke and failure checks pass. |
| 10: Observability | 9 | Protected health/metrics endpoints, request/error/latency and JVM metrics, circuit-breaker/rate-limit visibility, Prometheus/Grafana dashboards; trace/request correlation without personal-data logging. |
| 11: Load and failure measurements | 10 | Versioned k6 scripts and recorded environment/workload; publish throughput, p50/p95/p99, error rates and recovery behavior; set evidence-based budgets and identify bottlenecks. |
| 12: CI/CD | 9, 10, 11 | GitHub Actions runs tests including container integration tests, builds reproducible images, checks deployment health/rollback; credentials externalized; intentionally failing tests block delivery. |
| 13: Kubernetes | 12 | Resource requests/limits, readiness/liveness probes, HPA with metrics and recorded scaling experiments; verified canary traffic split and rollback; document stateful-dependency assumptions. |

Container tooling and CI can be moved earlier after an explicit plan update, but do not add them to Milestone 1. Each phase closes with build/tests, a lifecycle explanation, manual checks, updated progress/learning notes, and approval before the next phase.

## Final evidence

- Reproducible startup from a clean environment and documented prerequisites.
- Automated API, infrastructure, security, concurrency, and failure tests.
- Dashboards and reproducible load scripts paired with actual measurements.
- Explicit trade-offs: consistency, retry safety, event delivery, data ownership, outage policies, and recovery.
