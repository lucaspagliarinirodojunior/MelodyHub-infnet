# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

MelodyHub is a music streaming API built with Spring Boot 3.3.0 and Kotlin. It's an academic project demonstrating Domain-Driven Design (DDD), Clean Architecture, and SOLID principles. The system manages users, subscriptions, transactions with anti-fraud validation, and music file storage.

## Architecture

### Layer Structure (DDD + Clean Architecture)

```
src/main/kotlin/edu/infnet/melodyhub/
├── domain/                    # Core business entities and repository interfaces
│   ├── user/                  # User aggregate (PostgreSQL)
│   ├── transaction/           # Transaction aggregate (PostgreSQL)
│   └── music/                 # Music document (MongoDB)
├── application/               # Use cases, services, and DTOs
│   ├── auth/                  # Authentication logic (JWT)
│   ├── user/                  # User management
│   ├── transaction/           # Transaction processing + anti-fraud
│   └── music/                 # Music file management
└── infrastructure/            # Technical implementations
    ├── config/                # SecurityConfig, MongoConfig
    ├── security/              # JwtService
    ├── web/                   # REST controllers
    ├── user/                  # UserRepositoryImpl (adapter)
    └── transaction/           # TransactionRepositoryImpl (adapter)
```

**Key Pattern**: Repository Pattern with adapters. Domain defines interfaces (`UserRepository`), infrastructure implements them (`UserRepositoryImpl` wraps `JpaUserRepository`).

### Dual Database Setup

- **PostgreSQL**: Users and transactions (relational data)
  - Connection: `jdbc:postgresql://postgres:5432/melodyhub`
  - Schema: Auto-managed by Hibernate (`ddl-auto: update`)

- **MongoDB**: Music files via GridFS (binary storage)
  - Connection: `mongodb://root:rootpassword@mongo:27017/melodyhub-files`
  - Collections: `music` (metadata), `fs.files` + `fs.chunks` (GridFS)

### Authentication & Authorization

- **JWT-based auth**: Token generated on login, validated via `JwtService`
- **Password security**: BCrypt hashing (handled by Spring Security's PasswordEncoder)
- **Token claims**: email (subject), userId, role
- **Token expiration**: 24 hours
- **Role-based access**: Enforced at controller level (not Spring Security filters)

**Roles**: `SEM_PLANO` (no plan) → `BASIC` → `PREMIUM` → `ADMIN`

**Music Access Rules**:
- `SEM_PLANO`: Stream MP3/AAC only
- `BASIC`: Stream all + download MP3/AAC
- `PREMIUM`: Stream all + download all
- `ADMIN`: Full access + upload permission

## Development Commands

### Using Docker Compose (Recommended)

**Development mode with hot reload**:
```bash
docker-compose up --build        # Start all services (app + PostgreSQL + MongoDB)
docker-compose logs -f app       # Follow application logs
docker-compose down              # Stop all services
```

The app automatically reloads when you edit `.kt` files in `src/main/` (volume-mounted).

**Check service health**:
```bash
docker-compose ps                # View running containers
docker exec -it melodyhub-postgres psql -U postgres -d melodyhub  # Access PostgreSQL
docker exec -it melodyhub-mongo mongosh -u root -p rootpassword    # Access MongoDB
```

### Using Gradle Directly (Without Docker)

**Prerequisites**: Java 17+, PostgreSQL, MongoDB running locally

```bash
./gradlew build                  # Build project
./gradlew test                   # Run tests
./gradlew bootRun                # Start application (requires local DBs)
./gradlew clean build            # Clean build
```

**Run with H2 in-memory database** (for quick testing without PostgreSQL):
```bash
./gradlew bootRun --args='--spring.profiles.active=test'
```

### Testing the API


**Manual API testing**:
```bash
# Create user
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name": "Test User", "email": "test@example.com", "password": "senha123"}'

# Login (get JWT token)
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "test@example.com", "password": "senha123"}'

# Use token for protected endpoints
curl -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer <token>"
```

Insomnia collection available: `insomnia-collection.json`

## Business Logic & Domain Rules

### User Management
- Email must be unique (enforced in `UserService.create()`)
- Passwords: Min 6 characters, BCrypt hashed before storage
- Default role: `SEM_PLANO` on registration
- Role changes: Via transaction approval (see below)

### Transaction Processing & Anti-Fraud

Located in: `application/transaction/TransactionService.kt` + `AntiFraudService.kt`

**Anti-fraud validation rules**:
1. Amount must be positive and ≤ R$ 100.00
2. Max 3 transactions per minute per user
3. Max 5 transactions per day per user

**Transaction state machine**:
- `PENDING` → `APPROVED`: User role upgraded to subscription type (BASIC/PREMIUM)
- `PENDING` → `REJECTED`: Fraud detected, reason stored in `fraudReason` field

**Important**: Only pending transactions can change state (enforced in domain entity methods).

### Music File Management

Located in: `application/music/MusicService.kt`

- **Allowed formats**: MP3 (audio/mpeg), AAC (audio/aac), FLAC (audio/flac)
- **Storage**: MongoDB GridFS (max 200MB per file)
- **Upload**: Requires ADMIN role
- **Download/Stream**: Role-based access (see matrix above)
- **File retrieval**: Query `music` collection for metadata → retrieve from GridFS using `fileId`

## API Endpoints

### Authentication
- `POST /api/auth/login` - Login with email/password, returns JWT
- `GET /api/auth/me` - Get current user info (requires token)

### Users
- `POST /api/users` - Create user (public)
- `GET /api/users` - List all users
- `GET /api/users/{id}` - Get user by ID
- `GET /api/users/email/{email}` - Get user by email
- `DELETE /api/users/{id}` - Delete user

### Transactions
- `POST /api/transactions` - Create transaction (validates fraud)
- `GET /api/transactions` - List all transactions
- `GET /api/transactions/{id}` - Get transaction by ID
- `GET /api/transactions/user/{userId}` - Get user's transactions

### Music
- `POST /music/upload` - Upload audio file (ADMIN only, requires JWT)
- `GET /music/download/{id}` - Download music file (requires JWT + role check)
- `GET /music/stream/{id}` - Stream music (requires JWT + role check)
- `GET /music/list` - List all music (requires JWT)

## Observability - ELK Stack

MelodyHub implementa observabilidade completa com Elasticsearch, Logstash e Kibana, respeitando DDD:

### Stack Components
- **Elasticsearch** (port 9200): Log storage and indexing
- **Logstash** (ports 5044, 9600): Log processing and enrichment
- **Kibana** (port 5601): Visualization and analysis dashboard

### Logging Architecture (DDD-Compliant)

**Domain Layer**: Pure, no logging (domain events are observed externally)
**Application Layer**: Business logic logging (use cases, services)
**Infrastructure Layer**: Technical logging (HTTP, MDC, event listeners)

### Key Components

1. **MdcFilter** (`infrastructure/observability/MdcFilter.kt`):
   - Adds trace ID to all requests (X-Trace-Id header)
   - Populates MDC with request context

2. **RequestLoggingFilter** (`infrastructure/observability/RequestLoggingFilter.kt`):
   - Logs HTTP requests/responses with latency
   - Skips actuator endpoints

3. **UserContextEnricher** (`infrastructure/observability/UserContextEnricher.kt`):
   - Enriches MDC with user, transaction, music, event contexts

4. **DomainEventLogger** (`infrastructure/observability/DomainEventLogger.kt`):
   - Listens to domain events via RabbitMQ
   - Creates structured logs without modifying domain logic

### Configuration

- **Logback**: `src/main/resources/logback-spring.xml`
  - Console appender (human-readable)
  - JSON file appender (ELK-ready)
  - Async wrapper for performance

- **Logstash Pipeline**: `logstash/pipeline/melodyhub.conf`
  - Parses JSON logs
  - Tags by type: domain_event, transaction, user_activity, music_activity
  - Indexes to `melodyhub-YYYY.MM.dd`

### Access Points

```bash
# Kibana Dashboard
http://localhost:5601

# Elasticsearch API
http://localhost:9200

# Spring Boot Actuator
http://localhost:8080/actuator/health
http://localhost:8080/actuator/metrics
http://localhost:8080/actuator/prometheus
```

### Usage

```bash
# Start ELK stack with application
docker-compose up -d

# View application logs
docker-compose logs -f app

# Check Elasticsearch indices
curl http://localhost:9200/_cat/indices?v

# Query logs
curl http://localhost:9200/melodyhub-*/_search?pretty
```

**Full documentation**: See `ELK.md` for detailed setup, queries, dashboards, and troubleshooting.

## Configuration Files

- `build.gradle.kts` - Gradle build configuration with Kotlin DSL
- `settings.gradle.kts` - Project name configuration
- `docker-compose.yml` - Multi-container setup (app, PostgreSQL, MongoDB, ELK Stack)
- `Dockerfile.dev` - Development container with hot reload
- `Dockerfile` - Production-optimized container (multi-stage build)
- `gradle.properties` - Gradle daemon configuration
- `src/main/resources/application.yml` - Spring Boot configuration
- `src/main/resources/logback-spring.xml` - Logback configuration for structured JSON logging
- `logstash/pipeline/melodyhub.conf` - Logstash pipeline configuration
- `logstash/config/logstash.yml` - Logstash service configuration
- `ELK.md` - Complete ELK Stack documentation

## Important Implementation Notes

### When Adding New Features

1. **New domain entities**: Add to `domain/` layer as JPA entities or MongoDB documents
2. **New use cases**: Create services in `application/` layer with DTOs
3. **New endpoints**: Add controllers in `infrastructure/web/`
4. **Repository pattern**: Define interface in `domain/`, implement adapter in `infrastructure/`

### Transaction Boundaries

- **Read operations**: Use `@Transactional(readOnly = true)` in service methods
- **Write operations**: Use `@Transactional` on service methods that modify data
- **Controllers**: Never add `@Transactional` (let service layer manage transactions)

### Validation

- Use Jakarta Validation annotations on DTOs (`@NotBlank`, `@Email`, etc.)
- Domain validation in entity methods (e.g., `Transaction.approve()`)
- Business rule validation in services (e.g., email uniqueness check)

### Error Handling

Controllers use try-catch with `ResponseEntity`:
- `IllegalArgumentException` → 400 BAD_REQUEST
- `NoSuchElementException` → 404 NOT_FOUND
- Return `ErrorResponse(message: String)` for errors

### Security Considerations

- JWT secret is hardcoded in `JwtService` - should use environment variables in production
- CSRF disabled (stateless API)
- Password encoding handled by Spring Security's `PasswordEncoder`
- Never log or expose hashed passwords

## Common Development Tasks

### Adding a New Domain Entity

1. Create entity in `domain/{entity}/` with JPA or MongoDB annotations
2. Define repository interface in same package
3. Create service in `application/{entity}/` with DTOs
4. Implement repository adapter in `infrastructure/{entity}/` (if using JPA)
5. Add controller in `infrastructure/web/`

### Modifying Anti-Fraud Rules

Edit `application/transaction/AntiFraudService.kt`:
- Add new validation methods following pattern: `private fun checkRule(): FraudCheckResult`
- Call from `validate()` method
- Return `FraudCheckResult(isValid, reason)`

### Adding New User Roles

1. Update `domain/user/UserRole.kt` enum
2. Add helper method in `User` entity (e.g., `fun hasNewRole()`)
3. Update access control logic in `MusicController.checkPermission()`
4. Update role assignment in `TransactionService.create()`

## Testing Strategy

- Unit tests location: `src/test/kotlin/`
- Integration tests: Use `@SpringBootTest` with test profile (H2 database)
- API tests: Use curl scripts (`test-api.sh`, `test-transacoes-manual.sh`)
- H2 console (test profile): http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:mem:melodyhub`
  - Username: `sa`
  - Password: (blank)

## Troubleshooting

**Port 8080 already in use**:
```bash
docker-compose down  # Stop all containers
lsof -ti:8080 | xargs kill -9  # Kill process on port 8080
```

**Database connection issues**:
```bash
docker-compose logs postgres  # Check PostgreSQL logs
docker-compose logs mongo     # Check MongoDB logs
docker-compose restart postgres  # Restart PostgreSQL
```

**Hot reload not working**:
- Ensure you're using `docker-compose up` (not production Dockerfile)
- Check that `src/main/` is volume-mounted in docker-compose.yml
- Verify Spring DevTools is enabled in `application.yml`

**Build failures**:
```bash
./gradlew clean build --refresh-dependencies  # Refresh dependencies
docker-compose build --no-cache  # Rebuild Docker image from scratch
```
