# MelodyHub - Arquitetura DDD e Observabilidade

API de streaming de música construída com **Spring Boot 3.3.0** e **Kotlin**, aplicando Domain-Driven Design (DDD), Clean Architecture e SOLID.

## Stack Tecnológica

```mermaid
graph LR
    A[Spring Boot 3.3.0] --> B[Kotlin + JDK 17]
    A --> C[PostgreSQL]
    A --> D[MongoDB GridFS]
    A --> E[RabbitMQ]
    A --> F[ELK Stack]

    F --> F1[Elasticsearch]
    F --> F2[Logstash]
    F --> F3[Kibana]

    style A fill:#6db33f
    style E fill:#ff6600
    style F fill:#005571
```

---

## Arquitetura em Camadas (Clean Architecture + DDD)

```mermaid
graph TB
    subgraph Infrastructure["🔧 INFRASTRUCTURE LAYER"]
        I1[REST Controllers]
        I2[JPA/MongoDB Repositories]
        I3[JWT Security]
        I4[RabbitMQ Publisher]
        I5[Observability Filters]
    end

    subgraph Application["💼 APPLICATION LAYER"]
        A1[UserService]
        A2[TransactionService]
        A3[AntiFraudService]
        A4[AuthService]
        A5[DTOs]
    end

    subgraph Domain["🎯 DOMAIN LAYER - Puro"]
        D1[User Aggregate]
        D2[Transaction Aggregate]
        D3[Music Aggregate]
        D4[Playlist Aggregate]
        D5[Domain Events]
        D6[Repository Interfaces]
    end

    Infrastructure -->|depende| Application
    Application -->|depende| Domain

    style Domain fill:#4CAF50
    style Application fill:#2196F3
    style Infrastructure fill:#FF9800
```

**Princípio**: Dependências sempre apontam para dentro. Domain é puro, sem frameworks.

---

## Event Storm - Identificação de Domínios

```mermaid
graph TD
    subgraph ES["Event Storm - Fluxo de Negócio"]
        E1[Usuário se Registra] -->|UserCreated| E2[Usuário Faz Login]
        E2 -->|UserAuthenticated| E3[Adiciona Cartão]
        E3 -->|CreditCardAdded| E4[Cria Transação]
        E4 -->|TransactionCreated| E5{AntiFraud Valida}

        E5 -->|Aprovado| E6[TransactionApproved]
        E5 -->|Rejeitado| E7[FraudDetected]

        E6 -->|Upgrade Role| E8[UserSubscriptionUpgraded]
        E8 --> E9[Usuário Acessa Músicas]
        E9 -->|MusicStreamed| E10[Cria Playlists]
        E10 -->|PlaylistCreated| E11[Adiciona Favoritos]
    end

    style E5 fill:#ff9800
    style E6 fill:#4caf50
    style E7 fill:#f44336
```

### Domínios Identificados (5 Bounded Contexts)

```mermaid
graph LR
    subgraph Account["👤 ACCOUNT CONTEXT"]
        U[User Aggregate]
        UR[UserRole VO]
    end

    subgraph Payment["💳 PAYMENT CONTEXT"]
        T[Transaction Aggregate]
        CC[CreditCard Aggregate]
        ST[SubscriptionType VO]
    end

    subgraph AntiFraud["🛡️ ANTIFRAUD CONTEXT"]
        AF[AntiFraudService]
        FR[10 Regras de Validação]
    end

    subgraph Catalog["🎵 CATALOG CONTEXT"]
        M[Music Aggregate]
        MR[MusicRepository]
    end

    subgraph Playlist["📝 PLAYLIST CONTEXT"]
        P[Playlist Aggregate]
        PM[PlaylistMusic Entity]
    end

    Payment -->|valida com| AntiFraud
    Payment -->|atualiza role| Account
    Account -->|controla acesso| Catalog
    Catalog -->|fornece músicas| Playlist

    style Account fill:#2196f3
    style Payment fill:#ff9800
    style AntiFraud fill:#f44336
    style Catalog fill:#4caf50
    style Playlist fill:#9c27b0
```

---

## Context Map - Relacionamentos entre Contextos

```mermaid
graph TB
    Account[Account Context<br/>Core Domain]

    Account -->|Customer-Supplier| Payment[Payment Context]
    Account -->|Customer-Supplier| Playlist[Playlist Context]
    Account -->|Conformist| Catalog[Catalog Context]

    Payment <-->|Partnership| AntiFraud[AntiFraud Context]
    Payment -.->|Side Effect<br/>atualiza role| Account

    Catalog -->|Customer-Supplier| Playlist

    AntiFraud -.->|Domain Events<br/>via RabbitMQ| EventBus((🐰))

    style Account fill:#2196f3,color:#fff
    style Payment fill:#ff9800,color:#fff
    style AntiFraud fill:#f44336,color:#fff
    style EventBus fill:#ff6600,color:#fff
```

**Padrões Aplicados**:
- **Customer-Supplier**: Account fornece dados para Payment/Playlist
- **Partnership**: Payment e AntiFraud colaboram bidirecionalmente
- **Conformist**: Catalog se conforma às regras de UserRole do Account
- **Domain Events**: Comunicação assíncrona via RabbitMQ

---

## Aggregate Root - Domain Events

```mermaid
sequenceDiagram
    participant S as TransactionService
    participant T as Transaction Aggregate
    participant E as Event Collection
    participant P as DomainEventPublisher
    participant R as RabbitMQ

    S->>T: createTransaction()
    T->>T: validateInvariants()

    alt Aprovado
        T->>T: approve(newRole)
        T->>E: registerEvent(TransactionApprovedEvent)
    else Rejeitado
        T->>T: reject(reason)
        T->>E: registerEvent(FraudDetectedEvent)
    end

    S->>T: save(transaction)
    S->>T: getAndClearEvents()
    T-->>S: List<DomainEvent>

    S->>P: publish(events)
    P->>R: send to exchange

    Note over T,E: Aggregate coleta eventos<br/>sem publicar diretamente
    Note over S,P: Service publica após<br/>commit bem-sucedido
```

### Exemplo de Aggregate Root

```kotlin
@Entity
class Transaction(
    val userId: UUID,
    val amount: BigDecimal,
    var status: TransactionStatus = PENDING
) : AggregateRoot() {

    // ✅ Método de domínio rico: protege invariantes + registra evento
    fun approve(newUserRole: UserRole) {
        require(status == PENDING) { "Only pending can be approved" }

        status = APPROVED
        registerEvent(
            TransactionApprovedEvent(
                transactionId = id,
                userId = userId,
                newUserRole = newUserRole
            )
        )
    }
}
```

**Base Class**:
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

---

## Domain Events via RabbitMQ

```mermaid
graph LR
    subgraph Aggregates
        T[Transaction] -->|approve| E1[TransactionApprovedEvent]
        T -->|reject| E2[FraudDetectedEvent]
        U[User] -->|upgradeSubscription| E3[UserSubscriptionUpgradedEvent]
    end

    subgraph Publisher
        P[DomainEventPublisher]
    end

    subgraph RabbitMQ
        EX[melodyhub.events<br/>Topic Exchange]

        Q1[transaction.approved.queue]
        Q2[fraud.detected.queue]
        Q3[user.subscription.upgraded.queue]
        Q4[transaction.validated.queue]
    end

    subgraph Consumers
        L[DomainEventLogger]
        A[Audit Service]
        N[Notification Service]
    end

    E1 --> P
    E2 --> P
    E3 --> P

    P --> EX

    EX -->|transaction.approved| Q1
    EX -->|fraud.detected| Q2
    EX -->|user.subscription.upgraded| Q3
    EX -->|transaction.validated| Q4

    Q1 --> L
    Q2 --> L
    Q3 --> L
    Q4 --> L

    Q2 --> A
    Q1 --> N

    style EX fill:#ff6600,color:#fff
    style L fill:#005571,color:#fff
```

**Routing Keys**:
- `transaction.approved` → TransactionApprovedEvent
- `fraud.detected` → FraudDetectedEvent
- `user.subscription.upgraded` → UserSubscriptionUpgradedEvent
- `transaction.validated` → TransactionValidatedEvent

---

## Anti-Fraud Service - 10 Regras de Validação

```mermaid
graph TD
    Start([Nova Transação]) --> R1{Valor > 0?}
    R1 -->|Não| Reject1[❌ Rejeitar:<br/>Valor inválido]
    R1 -->|Sim| R2{Valor ≤ R$ 100?}

    R2 -->|Não| Reject2[❌ Rejeitar:<br/>Excede limite]
    R2 -->|Sim| R3{Alta frequência?<br/>> 3 em 2min}

    R3 -->|Sim| Reject3[❌ Rejeitar:<br/>Alta frequência]
    R3 -->|Não| R4{Duplicada?<br/>Mesmo valor em 2min}

    R4 -->|Sim| Reject4[❌ Rejeitar:<br/>Transação duplicada]
    R4 -->|Não| R5{Limite diário?<br/>> 5 hoje}

    R5 -->|Sim| Reject5[❌ Rejeitar:<br/>Limite diário excedido]
    R5 -->|Não| R6{Cartão existe?}

    R6 -->|Não| Reject6[❌ Rejeitar:<br/>Cartão não encontrado]
    R6 -->|Sim| R7{Cartão ativo?}

    R7 -->|Não| Reject7[❌ Rejeitar:<br/>Cartão inativo]
    R7 -->|Sim| R8{Cartão expirado?}

    R8 -->|Sim| Reject8[❌ Rejeitar:<br/>Cartão expirado]
    R8 -->|Não| R9{Cartão do usuário?}

    R9 -->|Não| Reject9[❌ Rejeitar:<br/>Cartão não pertence]
    R9 -->|Sim| R10{Já tem plano ativo?}

    R10 -->|Sim| Reject10[❌ Rejeitar:<br/>Plano já ativo]
    R10 -->|Não| Approve[✅ Aprovar Transação]

    style Approve fill:#4caf50,color:#fff
    style Reject1 fill:#f44336,color:#fff
    style Reject2 fill:#f44336,color:#fff
    style Reject3 fill:#f44336,color:#fff
    style Reject4 fill:#f44336,color:#fff
    style Reject5 fill:#f44336,color:#fff
    style Reject6 fill:#f44336,color:#fff
    style Reject7 fill:#f44336,color:#fff
    style Reject8 fill:#f44336,color:#fff
    style Reject9 fill:#f44336,color:#fff
    style Reject10 fill:#f44336,color:#fff
```

---

## Observabilidade - Sistema de Logs por Camada DDD

```mermaid
graph TB
    subgraph Domain["🎯 DOMAIN LAYER"]
        D1[User.upgradeSubscription]
        D2[Transaction.approve]
        D3[Transaction.reject]
        D1 -.->|registra| E1[Domain Events]
        D2 -.->|registra| E1
        D3 -.->|registra| E1
    end

    subgraph Application["💼 APPLICATION LAYER"]
        A1[TransactionService:<br/>logger.info creation]
        A2[AntiFraudService:<br/>logger.warn violations]
        A3[AuthService:<br/>logger.info login attempts]
        A4[UserContextEnricher:<br/>enrichWithUserContext]
    end

    subgraph Infrastructure["🔧 INFRASTRUCTURE LAYER"]
        I1[MdcFilter:<br/>traceId, requestUri]
        I2[RequestLoggingFilter:<br/>HTTP req/res, latency]
        I3[DomainEventLogger:<br/>@RabbitListener]
    end

    subgraph Logback["📝 LOGBACK"]
        L1[JSON Encoder]
        L2[Async Appender]
        L3[/var/log/melodyhub/melodyhub.json]
    end

    subgraph ELK["📊 ELK STACK"]
        E2[Logstash:<br/>parse, tag, enrich]
        E3[Elasticsearch:<br/>index melodyhub-*]
        E4[Kibana:<br/>visualize, search]
    end

    E1 -->|publicado via RabbitMQ| I3
    A1 --> L1
    A2 --> L1
    A3 --> L1
    I1 --> L1
    I2 --> L1
    I3 --> L1

    L1 --> L2
    L2 --> L3
    L3 --> E2
    E2 --> E3
    E3 --> E4

    style Domain fill:#4CAF50,color:#fff
    style Application fill:#2196F3,color:#fff
    style Infrastructure fill:#FF9800,color:#fff
    style ELK fill:#005571,color:#fff
```

### Componentes de Observabilidade

```mermaid
sequenceDiagram
    participant C as Client
    participant M as MdcFilter
    participant R as RequestLoggingFilter
    participant S as TransactionService
    participant E as UserContextEnricher
    participant L as Logger

    C->>M: HTTP Request
    M->>M: Generate traceId
    M->>M: MDC.put("traceId", uuid)
    M->>M: MDC.put("requestUri", uri)

    M->>R: doFilter()
    R->>R: Start timer

    R->>S: createTransaction()
    S->>E: enrichWithUserContext(userId)
    E->>E: MDC.put("userId", id)

    S->>L: logger.info("Creating transaction")
    Note over L: Log inclui automaticamente:<br/>traceId, userId, requestUri

    S-->>R: TransactionResponse
    R->>R: Stop timer, calc duration
    R->>L: logger.info("HTTP Response", duration)

    R-->>C: Response + X-Trace-Id header
```

### MDC Context - Campos Automáticos

Todos os logs incluem via MDC (Mapped Diagnostic Context):

```json
{
  "traceId": "550e8400-...",
  "userId": "660e8400-...",
  "userEmail": "test@test.com",
  "userRole": "PREMIUM",
  "transactionId": "770e8400-...",
  "musicId": "ObjectId(...)",
  "playlistId": "880e8400-...",
  "eventType": "TransactionApproved",
  "requestUri": "/api/transactions",
  "requestMethod": "POST"
}
```

---

## Repository Pattern - Inversão de Dependência

```mermaid
classDiagram
    class TransactionRepository {
        <<interface>>
        +save(Transaction) Transaction
        +findById(UUID) Transaction?
        +findByUserId(UUID) List~Transaction~
    }

    class TransactionRepositoryImpl {
        -jpaRepository: JpaTransactionRepository
        +save(Transaction) Transaction
        +findById(UUID) Transaction?
        +findByUserId(UUID) List~Transaction~
    }

    class JpaTransactionRepository {
        <<Spring Data JPA>>
        extends JpaRepository
    }

    class TransactionService {
        -repository: TransactionRepository
        +createTransaction() TransactionResponse
    }

    TransactionRepository <|.. TransactionRepositoryImpl : implements
    TransactionRepositoryImpl --> JpaTransactionRepository : uses
    TransactionService --> TransactionRepository : depends on interface

    note for TransactionRepository "Domain Layer\n(interface)"
    note for TransactionService "Application Layer\n(usa abstração)"
    note for TransactionRepositoryImpl "Infrastructure Layer\n(adapter)"
```

**Benefício**: Domain não conhece JPA. Fácil trocar para MongoDB, Redis, etc.

---

## Estrutura de Diretórios (DDD)

```
src/main/kotlin/edu/infnet/melodyhub/
│
├── domain/                           # 🎯 DOMAIN LAYER
│   ├── shared/
│   │   ├── AggregateRoot.kt         # Base class
│   │   └── DomainEvent.kt
│   ├── events/
│   │   ├── TransactionApprovedEvent.kt
│   │   ├── FraudDetectedEvent.kt
│   │   └── UserSubscriptionUpgradedEvent.kt
│   ├── user/
│   │   ├── User.kt                  # Aggregate Root
│   │   ├── UserRole.kt              # Value Object
│   │   └── UserRepository.kt        # Interface
│   ├── transaction/
│   │   ├── Transaction.kt
│   │   ├── SubscriptionType.kt
│   │   └── TransactionRepository.kt
│   ├── music/
│   │   ├── Music.kt
│   │   └── MusicRepository.kt
│   └── playlist/
│       ├── Playlist.kt
│       └── PlaylistRepository.kt
│
├── application/                      # 💼 APPLICATION LAYER
│   ├── user/
│   │   ├── UserService.kt
│   │   └── dto/
│   ├── transaction/
│   │   ├── TransactionService.kt
│   │   ├── AntiFraudService.kt      # Domain Service
│   │   └── dto/
│   ├── auth/
│   │   ├── AuthService.kt
│   │   └── dto/
│   └── music/
│       ├── MusicService.kt
│       └── dto/
│
└── infrastructure/                   # 🔧 INFRASTRUCTURE LAYER
    ├── web/                         # Controllers REST
    │   ├── UserController.kt
    │   ├── TransactionController.kt
    │   └── MusicController.kt
    ├── user/
    │   ├── JpaUserRepository.kt     # Spring Data
    │   └── UserRepositoryImpl.kt    # Adapter
    ├── security/
    │   ├── JwtService.kt
    │   └── SecurityConfig.kt
    ├── events/
    │   └── DomainEventPublisher.kt  # RabbitMQ
    └── observability/
        ├── MdcFilter.kt
        ├── RequestLoggingFilter.kt
        ├── UserContextEnricher.kt
        └── DomainEventLogger.kt
```

---

## Bancos de Dados - Dual Database

```mermaid
graph LR
    subgraph App[Application]
        S[Services]
    end

    subgraph PG[PostgreSQL - Relacional]
        T1[(users)]
        T2[(transactions)]
        T3[(credit_cards)]
        T4[(playlists)]
        T5[(playlist_music)]
    end

    subgraph Mongo[MongoDB - Documentos]
        C1[(music)]
        C2[(fs.files)]
        C3[(fs.chunks)]
    end

    S -->|JPA| PG
    S -->|GridFS| Mongo

    style PG fill:#336791,color:#fff
    style Mongo fill:#4db33d,color:#fff
```

**PostgreSQL**: Dados relacionais (usuários, transações, playlists)
**MongoDB GridFS**: Arquivos de áudio > 16MB (MP3, FLAC, AAC)

---

## Autenticação JWT - Flow

```mermaid
sequenceDiagram
    participant U as User
    participant A as AuthController
    participant S as AuthService
    participant J as JwtService
    participant DB as Database

    U->>A: POST /api/auth/login<br/>{email, password}
    A->>S: authenticate(request)
    S->>DB: findByEmail(email)
    DB-->>S: User

    S->>S: BCrypt.matches(password, user.password)

    alt Senha correta
        S->>J: generateToken(user)
        J->>J: Create JWT with claims:<br/>{sub: email, userId, role}
        J-->>S: token (String)
        S-->>A: LoginResponse(token, user)
        A-->>U: 200 OK + JWT
    else Senha incorreta
        S-->>A: throw InvalidCredentials
        A-->>U: 401 Unauthorized
    end

    Note over U,DB: Token expira em 24h<br/>Claims: email, userId, role
```

**Claims JWT**:
```json
{
  "sub": "user@example.com",
  "userId": "550e8400-...",
  "role": "PREMIUM",
  "iat": 1700000000,
  "exp": 1700086400
}
```

---

## Controle de Acesso - Music Permissions

```mermaid
graph TD
    U[Usuário] -->|Request| C{Operação?}

    C -->|STREAM| S1{Role?}
    C -->|DOWNLOAD| D1{Role?}

    S1 -->|SEM_PLANO| S2{Formato?}
    S1 -->|BASIC+| Allow1[✅ Permite todos formatos]
    S1 -->|PREMIUM+| Allow1
    S1 -->|ADMIN| Allow1

    S2 -->|MP3/AAC| Allow2[✅ Permite]
    S2 -->|FLAC| Deny1[❌ Requer BASIC]

    D1 -->|SEM_PLANO| Deny2[❌ Requer BASIC]
    D1 -->|BASIC| D2{Formato?}
    D1 -->|PREMIUM| Allow3[✅ Permite todos]
    D1 -->|ADMIN| Allow3

    D2 -->|MP3/AAC| Allow4[✅ Permite]
    D2 -->|FLAC| Deny3[❌ Requer PREMIUM]

    style Allow1 fill:#4caf50,color:#fff
    style Allow2 fill:#4caf50,color:#fff
    style Allow3 fill:#4caf50,color:#fff
    style Allow4 fill:#4caf50,color:#fff
    style Deny1 fill:#f44336,color:#fff
    style Deny2 fill:#f44336,color:#fff
    style Deny3 fill:#f44336,color:#fff
```

| Role | Stream MP3/AAC | Stream FLAC | Download MP3/AAC | Download FLAC | Upload |
|------|----------------|-------------|------------------|---------------|--------|
| SEM_PLANO | ✅ | ❌ | ❌ | ❌ | ❌ |
| BASIC | ✅ | ✅ | ✅ | ❌ | ❌ |
| PREMIUM | ✅ | ✅ | ✅ | ✅ | ❌ |
| ADMIN | ✅ | ✅ | ✅ | ✅ | ✅ |

---

## Docker Compose - Ambiente Completo

```mermaid
graph TB
    subgraph Docker[Docker Compose]
        App[melodyhub-app<br/>:8080]
        PG[postgres:16<br/>:5432]
        Mongo[mongo:7<br/>:27017]
        Rabbit[rabbitmq:3.13<br/>:5672, :15672]

        ES[elasticsearch:8.11<br/>:9200]
        LS[logstash:8.11<br/>:5044]
        KB[kibana:8.11<br/>:5601]
    end

    App -->|JDBC| PG
    App -->|GridFS| Mongo
    App -->|AMQP| Rabbit
    App -->|Logs| LS

    LS --> ES
    KB --> ES

    style App fill:#6db33f,color:#fff
    style Rabbit fill:#ff6600,color:#fff
    style ES fill:#005571,color:#fff
```

**Services**:
- `app`: Aplicação Spring Boot (porta 8080)
- `postgres`: Banco relacional (porta 5432)
- `mongo`: Banco de documentos (porta 27017)
- `rabbitmq`: Mensageria (portas 5672 AMQP, 15672 Management UI)
- `elasticsearch`: Indexação de logs (porta 9200)
- `logstash`: Processamento de logs (porta 5044)
- `kibana`: Visualização de logs (porta 5601)

---

## Padrões DDD Implementados

| Padrão | Implementação | Exemplo |
|--------|---------------|---------|
| **Aggregate** | User, Transaction, Music, Playlist | `Transaction` com métodos `approve()`, `reject()` |
| **Aggregate Root** | Base class com eventos | `AggregateRoot.registerEvent()` |
| **Value Object** | Enums imutáveis | `UserRole`, `SubscriptionType`, `TransactionStatus` |
| **Repository** | Interface no domain, adapter na infra | `TransactionRepository` (interface) → `TransactionRepositoryImpl` |
| **Domain Service** | Lógica que não pertence a um aggregate | `AntiFraudService` (10 regras de validação) |
| **Domain Event** | Eventos de negócio | `TransactionApprovedEvent`, `FraudDetectedEvent` |
| **Factory** | Métodos de criação | `TransactionResponse.from(transaction)` |
| **Anti-Corruption Layer** | DTOs separando domain de API | `CreateTransactionRequest` → `Transaction` → `TransactionResponse` |
| **Ubiquitous Language** | Nomes do domínio | `approve()`, `reject()`, `validateTransaction()`, não `setStatus()` |

---

## Testes - Cobertura > 80%

**29 arquivos de teste** cobrindo todas as camadas:

```
src/test/kotlin/
├── domain/
│   ├── AggregateRootTest.kt
│   ├── TransactionTest.kt
│   ├── UserTest.kt
│   ├── PlaylistTest.kt
│   └── DomainEventsTest.kt
├── application/
│   ├── TransactionServiceTest.kt
│   ├── AntiFraudServiceTest.kt    # Testa 10 regras
│   ├── AuthServiceTest.kt
│   └── UserServiceTest.kt
└── infrastructure/
    ├── TransactionControllerTest.kt
    ├── UserControllerTest.kt
    ├── JwtServiceTest.kt
    └── ...
```

**Ferramentas**:
- JUnit 5
- Mockito Kotlin
- JaCoCo (cobertura)

**Comando**:
```bash
./gradlew test jacocoTestReport
# Relatório: build/reports/jacoco/test/html/index.html
```

---

## Comandos Úteis

```bash
# Iniciar ambiente completo
docker-compose up --build

# Logs da aplicação
docker-compose logs -f app

# Acessar serviços
open http://localhost:8080        # API
open http://localhost:5601        # Kibana
open http://localhost:15672       # RabbitMQ Management

# Criar usuário
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Test","email":"test@test.com","password":"senha123"}'

# Login (obter JWT)
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"senha123"}'

# Rodar testes
./gradlew test

# Gerar relatório de cobertura
./gradlew jacocoTestReport
```

---

## Queries no Kibana

**Eventos de domínio**:
```
tags: "domain_event"
```

**Fraudes detectadas**:
```
eventType: "FraudDetected"
```

**Rastrear requisição por trace ID**:
```
traceId: "550e8400-e29b-41d4-a716-446655440000"
```

**Transações de um usuário**:
```
userId: "660e8400-..." AND tags: "transaction"
```

**Requisições lentas (> 1 segundo)**:
```
message: "HTTP Response" AND duration > 1000
```

---

## Boas Práticas DDD

### ✅ Rich Domain Model
Aggregates contêm lógica, não são anêmicos:
```kotlin
transaction.approve(newRole)  // ✅ Método de negócio
// transaction.status = APPROVED  ❌ Não expõe setter
```

### ✅ Ubiquitous Language
Nomes refletem linguagem do domínio:
- `approve()` não `setStatusApproved()`
- `validateTransaction()` não `check()`

### ✅ Invariantes Protegidos
```kotlin
fun approve(newUserRole: UserRole) {
    require(status == PENDING) { "Only pending can be approved" }
    // ...
}
```

### ✅ Domain Puro
Domain Layer não depende de frameworks:
- Sem Spring annotations em Aggregates
- Sem logging direto
- Repository como interface

### ✅ Eventos para Comunicação
Contextos se comunicam via eventos, não chamadas diretas:
```
Payment publica TransactionApprovedEvent
    ↓ RabbitMQ
Account consome e atualiza User.role
```
