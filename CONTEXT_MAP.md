# Context Map - MelodyHub

## Bounded Contexts

Este documento descreve o mapeamento entre os diferentes contextos delimitados (Bounded Contexts) do sistema MelodyHub, seguindo os princípios de Domain-Driven Design (DDD).

## Contextos Identificados

### 1. **Account Context** (`domain/user`)
**Responsabilidade**: Gerenciar identidade, autenticação e perfis de usuários.

**Entidades**:
- `User` - Agregado raiz
- `UserRole` - Value Object

**Serviços**:
- `UserService` - Gerenciamento de contas
- `AuthService` - Autenticação e geração de tokens JWT

**Repositório**:
- `UserRepository`

---

### 2. **Payment Context** (`domain/transaction` + `domain/creditcard`)
**Responsabilidade**: Processar pagamentos e assinaturas, gerenciar métodos de pagamento.

**Entidades**:
- `Transaction` - Agregado raiz (transações de assinatura)
- `CreditCard` - Agregado raiz (métodos de pagamento)
- `SubscriptionType` - Value Object
- `TransactionStatus` - Value Object
- `CreditCardStatus` - Value Object

**Serviços**:
- `TransactionService` - Processamento de transações
- `CreditCardService` - Gerenciamento de cartões

**Repositórios**:
- `TransactionRepository`
- `CreditCardRepository`

---

### 3. **AntiFraud Context** (`application/transaction/AntiFraudService`)
**Responsabilidade**: Validar transações contra regras de fraude.

**Serviços de Domínio**:
- `AntiFraudService` - Validação de fraude (10 regras)

**Domain Events** (implementados com RabbitMQ):
- `TransactionValidatedEvent` - Emitido após validação
- `FraudDetectedEvent` - Emitido quando fraude é detectada
- `TransactionApprovedEvent` - Emitido quando transação é aprovada

---

### 4. **Catalog Context** (`domain/music`)
**Responsabilidade**: Gerenciar catálogo de músicas e arquivos de áudio.

**Entidades**:
- `Music` - Agregado raiz (documento MongoDB)

**Serviços**:
- `MusicService` - Upload, download e streaming de músicas

**Repositório**:
- `MusicRepository` - MongoDB + GridFS

---

### 5. **Playlist Context** (`domain/playlist`)
**Responsabilidade**: Gerenciar playlists e favoritos dos usuários.

**Entidades**:
- `Playlist` - Agregado raiz
- `PlaylistMusic` - Entidade relacionada (tabela de junção)

**Serviços**:
- `PlaylistService` - Criação e gerenciamento de playlists
- `FavoritesService` - Gerenciamento de favoritos

**Repositórios**:
- `PlaylistRepository`
- `PlaylistMusicRepository`

---

## Relacionamentos entre Contextos (Context Map)

### Account ↔ Payment (Customer-Supplier)
**Tipo**: Customer-Supplier
- **Payment (Customer)** depende de **Account (Supplier)**
- Payment precisa validar se o usuário existe antes de processar transação
- Account fornece dados de usuário (User ID, Role) para Payment
- **Anti-Corruption Layer**: DTOs (CreateTransactionRequest) protegem o domínio

```
[Account Context] --supplies--> [Payment Context]
     User                          Transaction
                                   validates userId
```

### Payment ↔ AntiFraud (Partnership)
**Tipo**: Partnership (colaboração bidirecional)
- **Payment** solicita validação de **AntiFraud**
- **AntiFraud** retorna resultado da validação (aprovado/rejeitado)
- **AntiFraud** emite eventos de domínio sobre o resultado
- **Domain Events**: `TransactionValidatedEvent`, `FraudDetectedEvent`

```
[Payment Context] <--validates--> [AntiFraud Context]
   Transaction                     AntiFraudService
                                   emits: Domain Events
```

### Account ↔ Playlist (Customer-Supplier)
**Tipo**: Customer-Supplier
- **Playlist (Customer)** depende de **Account (Supplier)**
- Playlist valida se usuário existe ao criar playlist
- Account é dono das playlists (userId FK)

```
[Account Context] --supplies--> [Playlist Context]
     User                         Playlist
                                  owns playlists
```

### Account ↔ Catalog (Conformist)
**Tipo**: Conformist
- **Catalog** referencia usuários para controle de acesso
- Catalog usa Role do Account para autorização (download/stream)
- Catalog se conforma às regras de Role definidas em Account

```
[Account Context] --defines--> [Catalog Context]
   UserRole                     Music access rules
   (SEM_PLANO, BASIC,          (stream/download permissions)
    PREMIUM, ADMIN)
```

### Catalog ↔ Playlist (Customer-Supplier)
**Tipo**: Customer-Supplier
- **Playlist (Customer)** referencia **Catalog (Supplier)**
- Playlist adiciona músicas do catálogo (music ID FK)
- Catalog fornece metadados de músicas

```
[Catalog Context] --supplies--> [Playlist Context]
     Music                       PlaylistMusic
                                 references musicId
```

### Payment → Account (Side Effect)
**Tipo**: Downstream update
- Após aprovação de transação, **Payment** atualiza Role em **Account**
- Fluxo: Transaction.approve() → User.updateRole()
- Upgrade de plano: SEM_PLANO → BASIC/PREMIUM

```
[Payment Context] --updates--> [Account Context]
   Transaction.approve()        User.updateRole()
   (subscription)               (SEM_PLANO → BASIC/PREMIUM)
```

---

## Mapa Visual (Context Map Diagram)

```
┌─────────────────┐
│ Account Context │ (Core Domain)
│   - User        │
│   - UserRole    │
└────────┬────────┘
         │ supplies
         ├──────────────────────┐
         │                      │
         ▼                      ▼
┌────────────────┐      ┌──────────────┐
│ Payment Context│      │   Playlist   │
│  - Transaction │      │   Context    │
│  - CreditCard  │      │  - Playlist  │
└────┬───────────┘      └──────┬───────┘
     │                         │
     │ partnership             │ references
     │                         │
     ▼                         ▼
┌────────────────┐      ┌──────────────┐
│   AntiFraud    │      │   Catalog    │
│    Context     │      │   Context    │
│ (Domain Events)│      │   - Music    │
└────────────────┘      └──────────────┘
         │                      ▲
         │ emits events         │ supplies
         │ via RabbitMQ         │
         └──────────────────────┘
```

---

## Domain Events (RabbitMQ)

### Exchange: `melodyhub.events`
**Tipo**: Topic Exchange

### Routing Keys:
- `antifraud.transaction.validated` - Transação validada
- `antifraud.fraud.detected` - Fraude detectada
- `antifraud.transaction.approved` - Transação aprovada
- `antifraud.transaction.rejected` - Transação rejeitada

### Eventos Publicados:

#### 1. `TransactionValidatedEvent`
```kotlin
data class TransactionValidatedEvent(
    val transactionId: UUID,
    val userId: UUID,
    val amount: BigDecimal,
    val subscriptionType: SubscriptionType,
    val isValid: Boolean,
    val fraudReason: String?,
    val timestamp: LocalDateTime
)
```

#### 2. `FraudDetectedEvent`
```kotlin
data class FraudDetectedEvent(
    val transactionId: UUID,
    val userId: UUID,
    val fraudReason: String,
    val violatedRules: List<String>,
    val timestamp: LocalDateTime
)
```

#### 3. `TransactionApprovedEvent`
```kotlin
data class TransactionApprovedEvent(
    val transactionId: UUID,
    val userId: UUID,
    val subscriptionType: SubscriptionType,
    val newUserRole: UserRole,
    val timestamp: LocalDateTime
)
```

---

## Padrões DDD Aplicados

### 1. **Aggregates**
- Cada contexto tem seus agregados bem definidos
- User, Transaction, CreditCard, Playlist, Music são agregados raiz
- Consistência transacional dentro do agregado

### 2. **Repository Pattern**
- Interface no domínio, implementação na infraestrutura
- Um repositório por agregado raiz

### 3. **Domain Services**
- AntiFraudService - lógica de validação complexa
- AuthService - geração de tokens JWT

### 4. **Domain Events**
- AntiFraud emite eventos sobre validações
- Comunicação assíncrona via RabbitMQ
- Desacoplamento entre contextos

### 5. **Anti-Corruption Layer**
- DTOs protegem o domínio de dependências externas
- Conversão explícita entre camadas (Request → Entity → Response)

### 6. **Shared Kernel**
- UserRole é compartilhado entre Account e Catalog
- Mínimo de compartilhamento, apenas o necessário

---

## Decisões de Design

### Por que Partnership entre Payment e AntiFraud?
- AntiFraud é um serviço crítico que Payment depende completamente
- Colaboração bidirecional: Payment pede validação, AntiFraud responde e emite eventos
- Eventos permitem que outros contextos reajam às decisões de fraude

### Por que Domain Events via RabbitMQ?
- Desacoplamento temporal: Payment não precisa esperar processamento síncrono
- Extensibilidade: Novos consumidores podem reagir aos eventos
- Auditoria: Log de todas as validações de fraude
- Escalabilidade: Processamento assíncrono de eventos

### Por que Customer-Supplier para Account?
- Account é o contexto central (Core Domain)
- Outros contextos dependem da identidade do usuário
- Account define o contrato (User API), outros se adaptam

---

## Integração Assíncrona (RabbitMQ)

### Fluxo de Evento:

```
1. TransactionService cria transação
2. AntiFraudService valida transação
3. AntiFraudService publica evento:
   - Se válido: TransactionApprovedEvent
   - Se fraude: FraudDetectedEvent
4. Consumidores podem reagir:
   - Logging Service (auditoria)
   - Notification Service (alertas)
   - Analytics Service (métricas)
```

### Benefícios:
- ✅ Desacoplamento entre contextos
- ✅ Comunicação assíncrona e não-bloqueante
- ✅ Rastreamento de eventos de negócio
- ✅ Base para Event Sourcing futuro
- ✅ Escalabilidade horizontal

---

## Próximos Passos

1. ✅ Documentar Context Map
2. ⏳ Implementar Domain Events (classes)
3. ⏳ Adicionar RabbitMQ ao projeto
4. ⏳ Implementar Event Publisher
5. ⏳ Configurar exchanges e queues
6. ⏳ Testar publicação de eventos
