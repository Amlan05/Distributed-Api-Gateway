# Distributed API Gateway — User API

The User service in `user-service/` hosts a user-management REST API backed by PostgreSQL, with Spring Data JPA and isolated database tests. Separate Product and Order services are the approved next milestone; gateway routing remains future work. Java 17 and Spring Boot 4.1.1 are preserved.

## Package structure

```text
com.gateway
├── DistributedApiGatewayApplication.java
├── user
│   ├── controller
│   │   └── UserController.java
│   ├── service
│   │   └── UserService.java
│   ├── repository
│   │   ├── UserRepository.java
│   │   └── UserJpaRepository.java
│   ├── model
│   │   ├── User.java
│   │   └── UserEntity.java
│   ├── dto
│   │   ├── UserRequest.java
│   │   └── UserResponse.java
│   └── exception
│       └── UserNotFoundException.java
└── shared
    └── exception
        └── GlobalExceptionHandler.java
```

Packages group classes by feature, then responsibility. The request flow remains Controller -> Service -> Repository. Tests mirror the user feature's layer packages, with the full API lifecycle test in com.gateway.user.integration. Shared HTTP exception handling lives in shared.exception; user-specific exceptions live in user.exception.

## Run on Windows

Keep Docker Desktop running. Tests start disposable PostgreSQL containers through Testcontainers; they do not require the local development password or PostgreSQL Compose service. In PowerShell, from this project directory:

```powershell
# Use your installed Java 17 JDK (adjust only if your installation moves).
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version
.\user-service\mvnw.cmd -f .\user-service\pom.xml -B clean verify

# Start the local development database, then the application.
docker compose up -d --wait postgres
java -Duser.timezone=Asia/Kolkata -jar .\user-service\target\distributed-api-gateway-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

The Maven wrapper supplies Maven; no global mvn installation is needed. The first build may download Maven/dependencies and Docker test images. Local startup requires the ignored .secrets/postgres_password.txt used by compose.yaml. application-local.yaml imports that directory using configtree; keep the run working directory at the project root. Do not commit the password file.

IntelliJ can run DistributedApiGatewayApplication using temurin-17. Set the working directory explicitly to the repository root (the directory containing compose.yaml and .secrets), even when opening user-service as a separate IntelliJ project. Set VM options to -Duser.timezone=Asia/Kolkata and program arguments to --spring.profiles.active=local. The User service binds to 127.0.0.1:8081. Maven tests receive UTC through Surefire configuration. When running database tests directly with IntelliJ's JUnit runner, give that test run configuration VM options -Duser.timezone=UTC as well.

If 8081 is already occupied, stop your existing application yourself or choose another free port, for example 8084:

```powershell
java -Duser.timezone=Asia/Kolkata -jar .\user-service\target\distributed-api-gateway-0.0.1-SNAPSHOT.jar --spring.profiles.active=local --server.port=8084
```

After building, the packaged application can also run directly:

```powershell
java -Duser.timezone=Asia/Kolkata -jar .\user-service\target\distributed-api-gateway-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

## Test every endpoint with PowerShell

Leave the application running and open a second PowerShell terminal. These commands work with Windows PowerShell 5.1 and PowerShell 7. Use the application's port. PostgreSQL retains existing data across application restarts; use the returned id rather than assuming 1.

```powershell
$base = 'http://127.0.0.1:8081' # Adjust if you override server.port.

# GET collection: 200, possibly containing users from earlier runs.
Invoke-RestMethod -Method Get -Uri "$base/api/users"

# POST: 201 with JSON body and Location header.
$body = @{ name = 'Asha'; email = 'asha@example.com' } | ConvertTo-Json
$created = Invoke-WebRequest -UseBasicParsing -Method Post -Uri "$base/api/users" `
    -ContentType 'application/json' -Body $body
$created.StatusCode
$created.Headers['Location']
$user = $created.Content | ConvertFrom-Json
$userId = $user.id
$user

# GET by id: 200 with the created user.
Invoke-RestMethod -Method Get -Uri "$base/api/users/$userId"

# GET collection: includes the new user.
Invoke-RestMethod -Method Get -Uri "$base/api/users"

# PUT: 200, both name and email replaced, id unchanged.
$updatedBody = @{ name = 'Asha Rao'; email = 'rao@example.com' } | ConvertTo-Json
Invoke-RestMethod -Method Put -Uri "$base/api/users/$userId" `
    -ContentType 'application/json' -Body $updatedBody
Invoke-RestMethod -Method Get -Uri "$base/api/users/$userId"

# DELETE: 204 and no response body.
$deleted = Invoke-WebRequest -UseBasicParsing -Method Delete -Uri "$base/api/users/$userId"
$deleted.StatusCode
$deleted.Content
```

Expected failures throw in PowerShell. This helper prints the status and JSON error details without aborting the remaining examples:

```powershell
function Show-ApiError {
    param([scriptblock]$Request)
    try { & $Request }
    catch {
        if ($_.Exception.Response) { [int]$_.Exception.Response.StatusCode }
        if ($_.ErrorDetails.Message) { $_.ErrorDetails.Message }
        else { $_.Exception.Message }
    }
}

# Missing user after deletion: each is 404.
Show-ApiError { Invoke-RestMethod -Method Get -Uri "$base/api/users/$userId" }
Show-ApiError { Invoke-RestMethod -Method Put -Uri "$base/api/users/$userId" `
    -ContentType 'application/json' -Body $updatedBody }
Show-ApiError { Invoke-RestMethod -Method Delete -Uri "$base/api/users/$userId" }

# Invalid fields: 400 with errors.name and errors.email.
$invalid = @{ name = ''; email = 'not-an-email' } | ConvertTo-Json
Show-ApiError { Invoke-RestMethod -Method Post -Uri "$base/api/users" `
    -ContentType 'application/json' -Body $invalid }

# Invalid id: 400.
Show-ApiError { Invoke-RestMethod -Method Get -Uri "$base/api/users/0" }

# Malformed JSON: 400.
Show-ApiError { Invoke-RestMethod -Method Post -Uri "$base/api/users" `
    -ContentType 'application/json' -Body '{broken' }
```

## Contract

| Method and path | Success | Missing user |
| --- | --- | --- |
| GET /api/users | 200, JSON array sorted by id | Not applicable |
| GET /api/users/{id} | 200, user | 404 |
| POST /api/users | 201, user and Location | Not applicable |
| PUT /api/users/{id} | 200, user | 404; no upsert |
| DELETE /api/users/{id} | 204, empty body | 404 |

POST/PUT input: name (required, nonblank, <=100 characters) and email (required, nonblank, valid syntax, <=254 characters). Ids are positive Long values assigned by the server. Invalid input returns 400; unsupported HTTP methods/media types retain 405/415. Errors use application/problem+json; validation adds an errors map. Email uniqueness is not enforced yet.

The repository delegates to PostgreSQL through Spring Data JPA. Update/delete use transactional row locking; concurrent replacements remain last-writer-wins. The local profile uses Hibernate ddl-auto=update with Flyway disabled, as requested. The test profile uses create-drop only against disposable Testcontainers databases, with cleanup between repository tests.

No authentication is implemented yet; use synthetic data locally. See PROJECT_PLAN.md for the future architecture. The user has reported successful manual CRUD and application-restart persistence checks. Milestone 3 is approved; the existing application has been moved into user-service before adding the Product and Order applications.
