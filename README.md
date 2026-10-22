# Distributed API Gateway

A Java backend system with five independently running applications, JWT authentication, service-level authorization and Redis-backed distributed rate limiting.

**Stack:** Java 17, Spring Boot 4.1.1, Spring Cloud Gateway 5.0.3, Spring Security, PostgreSQL 17.11, Spring Data JPA, Redis 8.2.10, Docker Compose and Maven.

## Architecture

```text
Client -> API Gateway :8080
           |-- Auth service    :8091 -> gateway_auth
           |-- User service    :8081 -> gateway_users
           |-- Product service :8082 -> gateway_products
           `-- Order service   :8083 -> gateway_orders
                 |-- authenticated User lookup
                 `-- authenticated Product lookup

Gateway instances -> Redis :6380 -> shared rate-limit state
Business services -> Auth service JWKS -> JWT signature verification
```

The four services own separate PostgreSQL databases hosted in one local PostgreSQL container. The reactive gateway runs separately from the Spring MVC services. Packages group code by feature and responsibility; request/response DTOs separate HTTP contracts from persistence entities.

## Features

- RSA-signed JWT access tokens with a 15-minute lifetime, BCrypt password hashing and public-key distribution through JWKS.
- Independent signature, algorithm, issuer, audience and timestamp validation at the gateway and all business services.
- USER/ADMIN authorization, account-owned user profiles and creator-owned orders. Authentication account IDs and user profile IDs are independent.
- Redis token-bucket limits per account and route: 5 tokens replenished per second, 10-token burst capacity and one token per request. Gateway replicas share bucket state through atomic Redis scripts.
- HTTP 429 responses with rate-limit metadata and a filter for calculated Retry-After guidance.
- Product CRUD and order creation with purchase-time price snapshots, validated inputs and authenticated downstream calls.
- Transactional persistence, row locking for User/Product updates and deletes, bounded downstream connection/read timeouts and sanitized ProblemDetail errors.

### Access policy

| API | USER | ADMIN |
| --- | --- | --- |
| POST /api/auth/register, POST /api/auth/login | Public; registration assigns USER | Public |
| GET /api/products and /api/products/{id} | Allowed | Allowed |
| POST/PUT/DELETE products | Forbidden | Allowed |
| POST /api/users | One account-owned profile | Unowned managed profiles |
| GET /api/users | Forbidden | All profiles |
| GET/PUT/DELETE /api/users/{id} | Own profile | Any profile |
| POST /api/orders | Own profile and existing product | Any existing profile |
| GET orders, including by id | Own orders | All orders |

Missing or invalid authentication returns 401; forbidden operations return 403. Other accounts' profiles/orders are hidden with 404. Legacy rows without an owner remain administrator-only. Order ownership records the caller that created the order, including administrator-created orders.

The gateway's rate limiter currently fails open during Redis outages: authenticated requests proceed without quota enforcement and report `X-RateLimit-Remaining: -1`. Authentication and service authorization remain enforced. Public authentication endpoints are outside the account-based rate limiter. Services and infrastructure are configured for local loopback access.

## Local configuration

**Prerequisites:** Java 17 and Docker Desktop. Run application processes from the repository root.

Store local secrets in the ignored `.secrets/` directory:

| File | Configuration |
| --- | --- |
| postgres_password.txt | PostgreSQL initial `gateway` role |
| product_password.txt | `product_app` role |
| order_password.txt | `order_app` role |
| auth_password.txt | `auth_app` role |
| auth_keystore_password.txt | PKCS12 signing-keystore password |
| auth-signing.p12 | RSA signing key, alias `auth-signing` |

```powershell
docker compose up -d --wait postgres redis
docker compose exec postgres psql -U gateway -d gateway_users
```

Compose initializes `gateway_users`. On a fresh database, provision the other service databases in psql, entering passwords matching the corresponding secret files:

```sql
CREATE ROLE product_app LOGIN;
\password product_app
CREATE DATABASE gateway_products OWNER product_app;
CREATE ROLE order_app LOGIN;
\password order_app
CREATE DATABASE gateway_orders OWNER order_app;
CREATE ROLE auth_app LOGIN;
\password auth_app
CREATE DATABASE gateway_auth OWNER auth_app;
\q
```

Create the signing keystore once, using the password file above:

```powershell
keytool -genkeypair -alias auth-signing -keyalg RSA -keysize 3072 -sigalg SHA256withRSA -validity 365 -storetype PKCS12 -keystore .secrets/auth-signing.p12 -storepass:file .secrets/auth_keystore_password.txt -keypass:file .secrets/auth_keystore_password.txt -dname "CN=Gateway Auth"
```

Do not recreate existing roles/databases or overwrite an existing signing key. PostgreSQL and Redis are exposed on `127.0.0.1:5433` and `127.0.0.1:6380`. PostgreSQL uses a persistent volume; Redis rate-limit state is ephemeral. Application configuration imports `configtree:./.secrets/`. The local profile uses Hibernate `ddl-auto=update`; common database configuration uses `validate`.

## Build and run

Each application has its own Maven project. Packaging commands below skip automated test execution:

```powershell
foreach ($service in 'auth-service', 'user-service', 'product-service', 'order-service', 'api-gateway') {
    & ".\$service\mvnw.cmd" -f ".\$service\pom.xml" -B package -DskipTests
    if ($LASTEXITCODE -ne 0) { throw "Build failed: $service" }
}
```

Run each application in a separate terminal from the repository root:

```powershell
java -Duser.timezone=UTC -jar auth-service/target/auth-service-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
java -Duser.timezone=UTC -jar user-service/target/distributed-api-gateway-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
java -Duser.timezone=UTC -jar product-service/target/product-service-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
java -Duser.timezone=UTC -jar order-service/target/order-service-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
java -Duser.timezone=UTC -jar api-gateway/target/api-gateway-0.0.1-SNAPSHOT.jar
```

A second gateway can run with `--server.port=8084`. Gateway targets accept `AUTH_SERVICE_URL`, `USER_SERVICE_URL`, `PRODUCT_SERVICE_URL` and `ORDER_SERVICE_URL`. Order clients use `clients.user.base-url` and `clients.product.base-url`.

The default JWT issuer is `http://localhost:8091`, audience is `gateway-api`, and public JWKS endpoint is `http://127.0.0.1:8091/.well-known/jwks.json`. Register and log in through `/api/auth/register` and `/api/auth/login`; send the returned `accessToken` as `Authorization: Bearer <token>` on protected requests. User profiles are created separately through `/api/users`.

For local administrator provisioning, place a password in `.secrets/admin_password.txt` (12–72 characters, at most 72 UTF-8 bytes) and start auth-service with the `local` profile and these additional arguments:

```text
--auth.bootstrap.admin.enabled=true
--auth.bootstrap.admin.username=localadmin
--auth.bootstrap.admin.password-file=file:./.secrets/admin_password.txt
```

Provisioning is disabled by default, refuses to promote existing USER accounts and does not reset existing administrator passwords. Remove the bootstrap arguments after provisioning.

## Verification

- 86 HTTP security smoke checks passed against packaged applications and isolated PostgreSQL databases, covering authentication, authorization, ownership and invalid JWT rejection.
- Manual rate-limit checks demonstrated shared allowances across two gateway instances, independent account allowances, burst rejection and Redis-outage fail-open behavior.
- Retry-After header integration verification remains outstanding. The existing automated User API tests require security-aware setup; a passing full regression suite is not claimed.

Access tokens are not immediately revoked when accounts are disabled; they remain usable until expiry. The custom issuer does not implement a complete OAuth/OIDC authorization server. No load-test throughput or latency benchmark is claimed.
