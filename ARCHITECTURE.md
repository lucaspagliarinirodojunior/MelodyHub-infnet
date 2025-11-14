# MelodyHub - Arquitetura DDD e Observabilidade

API de streaming de música construída com **Spring Boot 3.3.0** e **Kotlin**, aplicando Domain-Driven Design (DDD), Clean Architecture e SOLID. Sistema acadêmico demonstrando implementação rigorosa de padrões táticos e estratégicos de DDD.

## Stack Tecnológica

- **Backend**: Spring Boot 3.3.0, Kotlin, JDK 17
- **Databases**: PostgreSQL (relacional), MongoDB (GridFS para arquivos)
- **Mensageria**: RabbitMQ (Domain Events)
- **Observabilidade**: ELK Stack (Elasticsearch, Logstash, Kibana)
- **Segurança**: JWT, BCrypt
- **Conteinerização**: Docker Compose

---

## Arquitetura em Camadas (DDD + Clean Architecture)

```
┌─────────────────────────────────────────────────────────────┐
│ INFRASTRUCTURE LAYER (Detalhes técnicos)                    │
│ • Controllers REST                                           │
│ • Repository Implementations (JPA/MongoDB adapters)         │
│ • Security (JWT, Filters)                                   │
│ • Observability (MDC, Logging Filters, Event Listeners)     │
│ • Event Publisher (RabbitMQ)                                │
└────────────────┬────────────────────────────────────────────┘
                 │ depende
                 ▼
┌─────────────────────────────────────────────────────────────┐
│ APPLICATION LAYER (Casos de uso)                            │
│ • Services (UserService, TransactionService, AuthService)   │
│ • DTOs (Request/Response)                                   │
│ • Domain Services (AntiFraudService)                        │
└────────────────┬────────────────────────────────────────────┘
                 │ depende
                 ▼
┌─────────────────────────────────────────────────────────────┐
│ DOMAIN LAYER (Regras de negócio puras)                     │
│ • Aggregates (User, Transaction, Music, Playlist)           │
│ • Value Objects (UserRole, SubscriptionType, etc)          │
│ • Repository Interfaces (inversão de dependência)           │
│ • Domain Events (TransactionApproved, FraudDetected, etc)  │
│ • AggregateRoot base class                                  │
└─────────────────────────────────────────────────────────────┘
```

**Princípio**: Dependência sempre aponta para dentro. Domain é puro, sem dependências externas.

---

## Domain Layer - Implementação DDD

### Aggregates e Aggregate Root

Cada Aggregate Root gerencia um cluster de entidades relacionadas e garante invariantes de negócio.

**Exemplo: Transaction Aggregate**

```kotlin
@Entity
@Table(name = "transactions")
class Transaction(
    @Id val id: UUID = UUID.randomUUID(),
    @Column(nullable = false) val userId: UUID,
    @Column(nullable = false) val amount: BigDecimal,
    @Column(nullable = false) @Enumerated(EnumType.STRING)
    val subscriptionType: SubscriptionType,
    @Column(nullable = false) val creditCardId: Long,
    @Column(nullable = false) @Enumerated(EnumType.STRING)
    var status: TransactionStatus = TransactionStatus.PENDING,
    @Column var fraudReason: String? = null,
    @Column(nullable = false) val createdAt: LocalDateTime = LocalDateTime.now(),
    @Column(nullable = false) var updatedAt: LocalDateTime = LocalDateTime.now()
) : AggregateRoot() {

    // ✅ Método de domínio: encapsula lógica e publica evento
    fun approve(newUserRole: UserRole) {
        require(status == TransactionStatus.PENDING) {
            "Only pending transactions can be approved"
        }
        status = TransactionStatus.APPROVED
        updatedAt = LocalDateTime.now()

        // Aggregate registra seu próprio evento
        registerEvent(
            TransactionApprovedEvent(
                transactionId = id,
                userId = userId,
                subscriptionType = subscriptionType,
                newUserRole = newUserRole
            )
        )
    }

    fun reject(reason: String) {
        require(status == TransactionStatus.PENDING) {
            "Only pending transactions can be rejected"
        }
        status = TransactionStatus.REJECTED
        fraudReason = reason
        updatedAt = LocalDateTime.now()

        registerEvent(
            FraudDetectedEvent(
                transactionId = id,
                userId = userId,
                fraudReason = reason,
                violatedRules = listOf(reason)
            )
        )
    }
}
```

**Características**:
- Métodos `approve()` e `reject()` protegem invariantes (só transações PENDING podem mudar de estado)
- Eventos de domínio são registrados internamente via `AggregateRoot.registerEvent()`
- Lógica de negócio encapsulada no aggregate, não vazada para services

### AggregateRoot Base Class

```kotlin
abstract class AggregateRoot {
    @Transient
    private val domainEvents = mutableListOf<DomainEvent>()

    protected fun registerEvent(event: DomainEvent) {
        domainEvents.add(event)
    }

    fun getAndClearEvents(): List<DomainEvent> {
        val events = domainEvents.toList()
        domainEvents.clear()
        return events
    }
}
```

**Pattern**: Aggregate coleta eventos durante operações de negócio. Application Layer publica os eventos **após** persistir com sucesso.

### Value Objects

```kotlin
enum class SubscriptionType(val monthlyPrice: BigDecimal) {
    BASIC(BigDecimal("9.90")),
    PREMIUM(BigDecimal("19.90"))
}

enum class UserRole {
    SEM_PLANO,  // Usuário sem plano
    BASIC,      // Plano básico
    PREMIUM,    // Plano premium
    ADMIN       // Administrador (upload de músicas)
}

enum class TransactionStatus {
    PENDING,
    APPROVED,
    REJECTED
}
```

**Vantagem**: Encapsula regras (ex: `SubscriptionType` conhece seu preço), type-safe, imutável.

### Repository Pattern (Inversão de Dependência)

**Interface no Domain:**

```kotlin
// domain/transaction/TransactionRepository.kt
interface TransactionRepository {
    fun save(transaction: Transaction): Transaction
    fun findById(id: UUID): Transaction?
    fun findAll(): List<Transaction>
    fun findByUserId(userId: UUID): List<Transaction>
    fun countByUserIdAndCreatedAtAfter(userId: UUID, after: LocalDateTime): Long
    fun findApprovedByUserId(userId: UUID): List<Transaction>
}
```

**Implementação na Infrastructure:**

```kotlin
// infrastructure/transaction/JpaTransactionRepository.kt
interface JpaTransactionRepository : JpaRepository<Transaction, UUID> {
    fun findByUserId(userId: UUID): List<Transaction>
    fun countByUserIdAndCreatedAtAfter(userId: UUID, after: LocalDateTime): Long
    @Query("SELECT t FROM Transaction t WHERE t.userId = :userId AND t.status = 'APPROVED'")
    fun findApprovedByUserId(@Param("userId") userId: UUID): List<Transaction>
}

// infrastructure/transaction/TransactionRepositoryImpl.kt
@Component
class TransactionRepositoryImpl(
    private val jpaRepository: JpaTransactionRepository
) : TransactionRepository {
    override fun save(transaction: Transaction) = jpaRepository.save(transaction)
    override fun findById(id: UUID) = jpaRepository.findById(id).orElse(null)
    override fun findAll() = jpaRepository.findAll()
    override fun findByUserId(userId: UUID) = jpaRepository.findByUserId(userId)
    // ... outras implementações
}
```

**Benefício**: Domain não conhece JPA. Fácil trocar persistência (ex: MongoDB) sem afetar domain.

---

## Application Layer - Casos de Uso

### Application Services

Orquestram operações de domínio, validam regras de negócio, coordenam aggregates.

**Exemplo: TransactionService**

```kotlin
@Service
class TransactionService(
    private val transactionRepository: TransactionRepository,
    private val antiFraudService: AntiFraudService,
    private val eventPublisher: DomainEventPublisher,
    private val userContextEnricher: UserContextEnricher
) {
    private val logger = LoggerFactory.getLogger(TransactionService::class.java)

    @Transactional
    fun createTransaction(request: CreateTransactionRequest): TransactionResponse {
        // 1. Logging de operação de negócio
        logger.info("Creating transaction: userId={}, subscriptionType={}",
            request.userId, request.subscriptionType)

        // 2. Enriquecer contexto MDC para logs
        userContextEnricher.enrichWithUserContext(request.userId.toString(), null, null)

        // 3. Criar aggregate
        val transaction = Transaction(
            userId = request.userId,
            amount = request.subscriptionType.monthlyPrice,
            subscriptionType = request.subscriptionType,
            creditCardId = request.creditCardId
        )

        // 4. Validar com Domain Service
        val fraudCheckResult = antiFraudService.validateTransaction(transaction)

        // 5. Aggregate decide seu estado baseado na validação
        if (!fraudCheckResult.isValid) {
            logger.warn("Transaction rejected: reason={}", fraudCheckResult.reason)
            transaction.reject(fraudCheckResult.reason ?: "Fraud validation failed")
        } else {
            val newRole = calculateNewRole(request.subscriptionType)
            transaction.approve(newRole)
        }

        // 6. Coletar eventos antes de salvar
        val eventsToPublish = transaction.getEvents().toList()

        // 7. Persistir
        val savedTransaction = transactionRepository.save(transaction)

        // 8. Registrar evento adicional (após ter ID)
        savedTransaction.recordValidation(fraudCheckResult.isValid, fraudCheckResult.reason)
        val validationEvents = savedTransaction.getAndClearEvents()

        // 9. Publicar eventos via RabbitMQ
        (eventsToPublish + validationEvents).forEach { event ->
            eventPublisher.publish(event)
        }

        return TransactionResponse.from(savedTransaction)
    }
}
```

**Padrão**: Service coordena, Aggregate decide. Eventos publicados após commit bem-sucedido.

### Domain Services

Lógica de domínio que não pertence a um único aggregate.

**Exemplo: AntiFraudService (10 regras de validação)**

```kotlin
@Service
class AntiFraudService(
    private val transactionRepository: TransactionRepository,
    private val creditCardRepository: CreditCardRepository
) {
    private val logger = LoggerFactory.getLogger(AntiFraudService::class.java)

    fun validateTransaction(transaction: Transaction): FraudCheckResult {
        // Regra 1: Valor positivo
        if (transaction.amount <= BigDecimal.ZERO) {
            return FraudCheckResult(false, "Valor deve ser positivo")
        }

        // Regra 2: Limite máximo (R$ 100)
        if (transaction.amount > BigDecimal("100.00")) {
            return FraudCheckResult(false, "Valor excede limite de R$ 100,00")
        }

        // Regra 3: Alta frequência (> 3 em 2 minutos)
        val twoMinutesAgo = LocalDateTime.now().minusMinutes(2)
        val recentCount = transactionRepository
            .countByUserIdAndCreatedAtAfter(transaction.userId, twoMinutesAgo)

        if (recentCount >= 3) {
            logger.warn("High frequency detected: count={}", recentCount)
            return FraudCheckResult(false, "Alta frequência: mais de 3 em 2 minutos")
        }

        // Regra 4-10: Duplicação, limite diário, validação de cartão, etc...

        return FraudCheckResult(true, null)
    }
}

data class FraudCheckResult(val isValid: Boolean, val reason: String?)
```

---

## Domain Events - Comunicação Assíncrona

### Bounded Contexts e Context Map

```
┌─────────────┐
│   Account   │ (User aggregate)
└──────┬──────┘
       │ supplies
       ├──────────────────────┐
       ▼                      ▼
┌──────────────┐      ┌──────────────┐
│   Payment    │      │   Catalog    │
│ (Transaction)│      │   (Music)    │
└──────┬───────┘      └──────────────┘
       │
       │ partnership
       ▼
┌──────────────┐
│  AntiFraud   │
│  (Service)   │
└──────────────┘
```

### Domain Events via RabbitMQ

**Exchange**: `melodyhub.events` (Topic)

**Routing Keys**:
- `transaction.approved` → TransactionApprovedEvent
- `fraud.detected` → FraudDetectedEvent
- `user.subscription.upgraded` → UserSubscriptionUpgradedEvent
- `transaction.validated` → TransactionValidatedEvent

**Fluxo de Evento**:

```
1. Transaction.approve() registra TransactionApprovedEvent
2. TransactionService persiste Transaction
3. TransactionService coleta eventos via getAndClearEvents()
4. DomainEventPublisher publica no RabbitMQ
5. DomainEventLogger (listener) recebe e loga para ELK
6. Outros consumidores podem reagir (ex: enviar email, atualizar CRM)
```

**Benefícios**:
- Desacoplamento temporal entre contextos
- Auditoria completa (todos eventos persistidos)
- Extensibilidade (novos consumidores sem mudar código)
- Rastreabilidade (trace ID propagado)

---

## Observabilidade - Sistema de Logs (ELK Stack)

### Arquitetura de Logging por Camada DDD

```
DOMAIN LAYER
├─ Sem logging direto
└─ Publica Domain Events

APPLICATION LAYER
├─ Logs de casos de uso
├─ AuthService: login attempts (success/failure)
├─ TransactionService: transaction creation
├─ AntiFraudService: fraud detection (each rule)
└─ UserService: user operations

INFRASTRUCTURE LAYER
├─ MdcFilter: trace ID, request context
├─ RequestLoggingFilter: HTTP req/res, latency
├─ DomainEventLogger: escuta eventos e loga
└─ UserContextEnricher: enriquece MDC
```

### Componentes de Observabilidade

#### 1. MdcFilter - Rastreamento Distribuído

```kotlin
@Component
@Order(1)
class MdcFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            // Gera trace ID único
            val traceId = UUID.randomUUID().toString()
            MDC.put("traceId", traceId)

            // Contexto HTTP
            MDC.put("requestUri", request.requestURI)
            MDC.put("requestMethod", request.method)

            // Propaga trace ID via header
            response.addHeader("X-Trace-Id", traceId)

            filterChain.doFilter(request, response)
        } finally {
            MDC.clear()  // Evita memory leak em thread pools
        }
    }
}
```

#### 2. UserContextEnricher - Contexto de Negócio

```kotlin
@Component
class UserContextEnricher {
    fun enrichWithUserContext(userId: String?, email: String?, role: String?) {
        userId?.let { MDC.put("userId", it) }
        email?.let { MDC.put("userEmail", it) }
        role?.let { MDC.put("userRole", it) }
    }

    fun enrichWithTransactionContext(transactionId: String) {
        MDC.put("transactionId", transactionId)
    }

    fun enrichWithEventContext(eventType: String) {
        MDC.put("eventType", eventType)
    }
}
```

**Uso no Application Service**:

```kotlin
@Service
class TransactionService(
    private val userContextEnricher: UserContextEnricher
) {
    fun createTransaction(request: CreateTransactionRequest): TransactionResponse {
        // Enriquece MDC - todos os logs seguintes terão userId
        userContextEnricher.enrichWithUserContext(request.userId.toString(), null, null)

        logger.info("Creating transaction")  // Log terá userId automaticamente

        val transaction = Transaction(...)
        val saved = transactionRepository.save(transaction)

        // Adiciona transactionId ao contexto
        userContextEnricher.enrichWithTransactionContext(saved.id.toString())

        logger.info("Transaction saved")  // Log terá userId + transactionId
    }
}
```

#### 3. DomainEventLogger - Observando Eventos de Domínio

```kotlin
@Component
class DomainEventLogger(
    private val userContextEnricher: UserContextEnricher
) {
    private val logger = LoggerFactory.getLogger(DomainEventLogger::class.java)

    @RabbitListener(queues = ["transaction.approved.queue"])
    fun onTransactionApproved(event: TransactionApprovedEvent) {
        userContextEnricher.enrichWithEventContext("TransactionApproved")
        userContextEnricher.enrichWithTransactionContext(event.transactionId.toString())

        logger.info(
            "Domain Event: TransactionApproved - transactionId={}, userId={}, newRole={}",
            event.transactionId,
            event.userId,
            event.newUserRole
        )
    }

    @RabbitListener(queues = ["fraud.detected.queue"])
    fun onFraudDetected(event: FraudDetectedEvent) {
        userContextEnricher.enrichWithEventContext("FraudDetected")

        logger.warn(
            "Domain Event: FraudDetected - transactionId={}, reason={}",
            event.transactionId,
            event.fraudReason
        )
    }
}
```

**Princípio DDD**: Domain Layer não loga. Infrastructure **observa** eventos via listener.

### Logback - JSON Estruturado

**Configuração**: `logback-spring.xml`

```xml
<appender name="JSON_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>/var/log/melodyhub/melodyhub.json</file>
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
        <includeMdcKeyName>traceId</includeMdcKeyName>
        <includeMdcKeyName>userId</includeMdcKeyName>
        <includeMdcKeyName>userEmail</includeMdcKeyName>
        <includeMdcKeyName>userRole</includeMdcKeyName>
        <includeMdcKeyName>transactionId</includeMdcKeyName>
        <includeMdcKeyName>eventType</includeMdcKeyName>
    </encoder>
</appender>

<!-- Async wrapper para performance -->
<appender name="ASYNC_JSON" class="ch.qos.logback.classic.AsyncAppender">
    <appender-ref ref="JSON_FILE"/>
    <queueSize>512</queueSize>
    <neverBlock>true</neverBlock>
</appender>
```

**Resultado** (log JSON):

```json
{
  "@timestamp": "2025-11-14T10:30:00.123Z",
  "level": "INFO",
  "logger_name": "TransactionService",
  "message": "Creating transaction",
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "userId": "660e8400-e29b-41d4-a716-446655440001",
  "userRole": "SEM_PLANO",
  "transactionId": "770e8400-e29b-41d4-a716-446655440002",
  "eventType": "TransactionApproved",
  "application": "melodyhub",
  "environment": "production"
}
```

### Pipeline Logstash

**Configuração**: `logstash/pipeline/melodyhub.conf`

```
input {
  file {
    path => "/var/log/melodyhub/melodyhub.json"
    codec => "json"
    start_position => "beginning"
  }
}

filter {
  # Tag eventos de domínio
  if [eventType] {
    mutate { add_tag => ["domain_event"] }
  }

  # Tag transações
  if [transactionId] {
    mutate { add_tag => ["transaction"] }
  }

  # Tag alertas
  if [level] == "ERROR" or [level] == "WARN" {
    mutate { add_tag => ["alert"] }
  }
}

output {
  elasticsearch {
    hosts => ["elasticsearch:9200"]
    index => "melodyhub-%{+YYYY.MM.dd}"
  }
}
```

### Queries no Kibana

**Eventos de domínio**:
```
tags: "domain_event"
```

**Fraudes detectadas**:
```
eventType: "FraudDetected" OR (tags: "alert" AND logger_name: *AntiFraudService*)
```

**Rastrear requisição completa por trace ID**:
```
traceId: "550e8400-e29b-41d4-a716-446655440000"
```

**Transações de um usuário**:
```
userId: "660e8400-e29b-41d4-a716-446655440001" AND tags: "transaction"
```

**Requisições lentas (> 1s)**:
```
message: "HTTP Response" AND duration > 1000
```

---

## Boas Práticas DDD Implementadas

### ✅ Rich Domain Model

Aggregates contêm lógica de negócio, não são anêmicos:

```kotlin
class Transaction {
    fun approve(newUserRole: UserRole) {
        require(status == PENDING) { "Only pending can be approved" }
        // Lógica + evento
    }
}
```

### ✅ Ubiquitous Language

Nomes refletem linguagem do domínio:
- `Transaction.approve()` não `Transaction.setStatusApproved()`
- `AntiFraudService.validateTransaction()` não `AntiFraudService.check()`
- `UserRole.SEM_PLANO` não `UserRole.FREE`

### ✅ Invariantes Protegidos

Apenas métodos de negócio modificam estado:

```kotlin
// ❌ Não expõe setter
// transaction.status = APPROVED

// ✅ Usa método de negócio
transaction.approve(newRole)
```

### ✅ Bounded Contexts Separados

- **Account Context**: User aggregate
- **Payment Context**: Transaction, CreditCard aggregates
- **Catalog Context**: Music aggregate
- **AntiFraud Context**: Domain Service

### ✅ Anti-Corruption Layer

DTOs protegem domain de dependências externas:

```kotlin
// Request DTO (camada de aplicação)
data class CreateTransactionRequest(
    val userId: UUID,
    val subscriptionType: SubscriptionType,
    val creditCardId: Long
)

// Response DTO (camada de aplicação)
data class TransactionResponse(
    val id: UUID,
    val userId: UUID,
    val status: String,
    // ...
) {
    companion object {
        fun from(transaction: Transaction) = TransactionResponse(
            id = transaction.id,
            userId = transaction.userId,
            status = transaction.status.name,
            // ...
        )
    }
}
```

### ✅ Eventos de Domínio para Comunicação

Contextos se comunicam via eventos, não chamadas diretas:

```
Payment Context publica TransactionApprovedEvent
    ↓
Account Context consome evento e atualiza User.role
```

---

## Estrutura de Diretórios

```
src/main/kotlin/edu/infnet/melodyhub/
│
├── domain/                           # DOMAIN LAYER
│   ├── shared/
│   │   ├── AggregateRoot.kt         # Base para aggregates
│   │   └── DomainEvent.kt           # Interface de eventos
│   ├── events/
│   │   ├── TransactionApprovedEvent.kt
│   │   ├── FraudDetectedEvent.kt
│   │   └── UserSubscriptionUpgradedEvent.kt
│   ├── user/
│   │   ├── User.kt                  # Aggregate Root
│   │   ├── UserRole.kt              # Value Object
│   │   └── UserRepository.kt        # Interface
│   ├── transaction/
│   │   ├── Transaction.kt           # Aggregate Root
│   │   ├── SubscriptionType.kt      # Value Object
│   │   └── TransactionRepository.kt
│   ├── music/
│   │   ├── Music.kt                 # Aggregate Root (MongoDB)
│   │   └── MusicRepository.kt
│   └── playlist/
│       ├── Playlist.kt
│       └── PlaylistRepository.kt
│
├── application/                      # APPLICATION LAYER
│   ├── user/
│   │   ├── UserService.kt
│   │   └── dto/
│   │       ├── CreateUserRequest.kt
│   │       └── UserResponse.kt
│   ├── auth/
│   │   ├── AuthService.kt
│   │   └── dto/
│   ├── transaction/
│   │   ├── TransactionService.kt
│   │   ├── AntiFraudService.kt      # Domain Service
│   │   └── dto/
│   └── music/
│       ├── MusicService.kt
│       └── dto/
│
└── infrastructure/                   # INFRASTRUCTURE LAYER
    ├── web/                         # Controllers REST
    │   ├── UserController.kt
    │   ├── AuthController.kt
    │   ├── TransactionController.kt
    │   └── MusicController.kt
    ├── user/
    │   ├── JpaUserRepository.kt     # Spring Data JPA
    │   └── UserRepositoryImpl.kt    # Adapter
    ├── transaction/
    │   ├── JpaTransactionRepository.kt
    │   └── TransactionRepositoryImpl.kt
    ├── security/
    │   ├── JwtService.kt
    │   └── SecurityConfig.kt
    ├── config/
    │   ├── MongoConfig.kt
    │   └── RabbitConfig.kt
    ├── events/
    │   └── DomainEventPublisher.kt  # RabbitMQ publisher
    └── observability/               # Logging & Monitoring
        ├── MdcFilter.kt
        ├── RequestLoggingFilter.kt
        ├── UserContextEnricher.kt
        └── DomainEventLogger.kt     # Event listener
```

---

## Bancos de Dados

### PostgreSQL (Relacional)

**Entidades**:
- `users` (User aggregate)
- `transactions` (Transaction aggregate)
- `credit_cards` (CreditCard aggregate)
- `playlists` (Playlist aggregate)
- `playlist_music` (join table)

**Schema Management**: Hibernate DDL auto-update

### MongoDB (Documentos)

**Collections**:
- `music` (Music aggregate - metadados)
- `fs.files` + `fs.chunks` (GridFS - arquivos binários de áudio)

**GridFS**: Armazenamento de arquivos > 16MB (MP3, FLAC, AAC)

---

## Autenticação e Autorização

### JWT Flow

```
1. POST /api/auth/login
   → AuthService valida credenciais (BCrypt)
   → Gera JWT com claims: { sub: email, userId, role }

2. Cliente envia JWT no header: Authorization: Bearer <token>

3. JwtService valida token e extrai claims

4. Controllers verificam role para operações sensíveis
```

### Controle de Acesso (Music)

```kotlin
// infrastructure/web/MusicController.kt
private fun checkPermission(user: User, operation: String, format: String) {
    when (operation) {
        "STREAM" -> {
            // SEM_PLANO: apenas MP3/AAC
            if (user.role == UserRole.SEM_PLANO && format == "FLAC") {
                throw IllegalAccessException("FLAC streaming requires BASIC plan")
            }
        }
        "DOWNLOAD" -> {
            // Requer BASIC ou superior
            if (user.role == UserRole.SEM_PLANO) {
                throw IllegalAccessException("Downloads require BASIC plan")
            }
            // BASIC: apenas MP3/AAC, PREMIUM: todos
            if (user.role == UserRole.BASIC && format == "FLAC") {
                throw IllegalAccessException("FLAC downloads require PREMIUM plan")
            }
        }
    }
}
```

---

## Docker Compose - Ambiente Completo

```yaml
version: '3.8'

services:
  app:
    build:
      context: .
      dockerfile: Dockerfile
    ports:
      - "8080:8080"
    depends_on:
      - postgres
      - mongo
      - rabbitmq
      - elasticsearch
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/melodyhub
      SPRING_DATA_MONGODB_URI: mongodb://root:rootpassword@mongo:27017/melodyhub-files
      SPRING_RABBITMQ_HOST: rabbitmq

  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: melodyhub
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres

  mongo:
    image: mongo:7
    environment:
      MONGO_INITDB_ROOT_USERNAME: root
      MONGO_INITDB_ROOT_PASSWORD: rootpassword

  rabbitmq:
    image: rabbitmq:3.13-management-alpine
    ports:
      - "15672:15672"  # Management UI
    environment:
      RABBITMQ_DEFAULT_USER: melodyhub
      RABBITMQ_DEFAULT_PASS: melodyhub123

  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.11.0
    ports:
      - "9200:9200"
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
      - "ES_JAVA_OPTS=-Xms512m -Xmx512m"

  logstash:
    image: docker.elastic.co/logstash/logstash:8.11.0
    volumes:
      - ./logstash/pipeline:/usr/share/logstash/pipeline
      - app-logs:/var/log/melodyhub
    depends_on:
      - elasticsearch

  kibana:
    image: docker.elastic.co/kibana/kibana:8.11.0
    ports:
      - "5601:5601"
    depends_on:
      - elasticsearch

volumes:
  app-logs:
```

---

## Comandos Úteis

```bash
# Iniciar ambiente completo
docker-compose up --build

# Logs da aplicação
docker-compose logs -f app

# Acessar Kibana
open http://localhost:5601

# Verificar índices Elasticsearch
curl http://localhost:9200/_cat/indices?v

# Criar usuário
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Test","email":"test@test.com","password":"senha123"}'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"senha123"}'

# Criar transação (com token JWT)
curl -X POST http://localhost:8080/api/transactions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{"userId":"<UUID>","subscriptionType":"PREMIUM","creditCardId":1}'
```

---

## Principais Padrões DDD Aplicados

| Padrão | Implementação | Localização |
|--------|---------------|-------------|
| **Aggregate** | User, Transaction, Music, Playlist | `domain/` |
| **Aggregate Root** | Base class com eventos | `domain/shared/AggregateRoot.kt` |
| **Value Object** | UserRole, SubscriptionType, TransactionStatus | `domain/` enums |
| **Repository** | Interfaces no domain, adapters na infra | `domain/*/Repository.kt` |
| **Domain Service** | AntiFraudService (lógica que não pertence a um aggregate) | `application/transaction/` |
| **Domain Event** | TransactionApproved, FraudDetected, etc | `domain/events/` |
| **Factory** | `from()` methods em DTOs | `application/*/dto/` |
| **Anti-Corruption Layer** | DTOs separando domain de API | `application/*/dto/` |
| **Bounded Context** | Account, Payment, Catalog, AntiFraud | Organização em packages |

---

## Segurança

**Implementado**:
- Senhas hasheadas com BCrypt (nunca em texto plano)
- JWT com expiração de 24h
- Validação de propriedade (cartão pertence ao usuário)
- Logs não expõem dados sensíveis (sem senhas, tokens completos)

**Produção** (recomendações):
- Externalizar JWT secret (variável de ambiente)
- HTTPS obrigatório
- Rate limiting
- Elasticsearch com autenticação (X-Pack)
- LGPD compliance (retenção de logs, anonimização)
