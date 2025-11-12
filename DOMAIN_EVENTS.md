# Domain Events - RabbitMQ Integration

## Visão Geral

O sistema MelodyHub utiliza Domain Events via RabbitMQ para comunicação assíncrona entre bounded contexts, seguindo princípios de Domain-Driven Design (DDD).

## Arquitetura de Eventos

### Exchange e Routing

```
Exchange: melodyhub.events (Topic Exchange)

Routing Keys:
├── antifraud.transaction.validated  (todas as validações)
├── antifraud.fraud.detected         (fraudes detectadas)
├── antifraud.transaction.approved   (transações aprovadas)
└── antifraud.transaction.rejected   (transações rejeitadas)
```

### Queues Configuradas

1. **antifraud.fraud.detection**
   - Propósito: Processar alertas de fraude
   - Binding: `antifraud.fraud.detected`
   - TTL: 24 horas
   - Uso: Auditoria, alertas, bloqueio de usuários

2. **antifraud.transaction.audit**
   - Propósito: Auditoria completa de transações
   - Binding: `antifraud.transaction.*` (todas as transações)
   - TTL: 30 dias
   - Uso: Compliance, analytics, relatórios

## Eventos Publicados

### 1. TransactionValidatedEvent

Publicado sempre que uma transação é validada (aprovada ou rejeitada).

**Routing Key**: `antifraud.transaction.validated`

**Estrutura**:
```json
{
  "eventId": "uuid",
  "occurredOn": "2025-11-12T10:30:00",
  "eventType": "antifraud.transaction.validated",
  "transactionId": "uuid",
  "userId": "uuid",
  "amount": 19.90,
  "subscriptionType": "PREMIUM",
  "isValid": true,
  "fraudReason": null
}
```

**Quando é emitido**:
- Após validação completa do AntiFraud
- Independente do resultado (aprovado/rejeitado)

**Consumidores típicos**:
- Sistema de auditoria
- Analytics/métricas
- Data warehouse

---

### 2. FraudDetectedEvent

Publicado quando fraude é detectada em uma transação.

**Routing Key**: `antifraud.fraud.detected`

**Estrutura**:
```json
{
  "eventId": "uuid",
  "occurredOn": "2025-11-12T10:30:00",
  "eventType": "antifraud.fraud.detected",
  "transactionId": "uuid",
  "userId": "uuid",
  "fraudReason": "Alta frequência detectada: mais de 3 transações em 2 minutos",
  "violatedRules": [
    "Alta frequência detectada: mais de 3 transações em 2 minutos"
  ]
}
```

**Quando é emitido**:
- Quando qualquer das 10 regras de antifraude falha
- Antes de salvar a transação com status REJECTED

**Consumidores típicos**:
- Sistema de alertas (email/SMS para equipe de segurança)
- Sistema de bloqueio automático
- Dashboard de fraudes em tempo real
- Sistema de machine learning para análise de padrões

---

### 3. TransactionApprovedEvent

Publicado quando uma transação é aprovada e o usuário tem seu plano atualizado.

**Routing Key**: `antifraud.transaction.approved`

**Estrutura**:
```json
{
  "eventId": "uuid",
  "occurredOn": "2025-11-12T10:30:00",
  "eventType": "antifraud.transaction.approved",
  "transactionId": "uuid",
  "userId": "uuid",
  "subscriptionType": "PREMIUM",
  "newUserRole": "PREMIUM"
}
```

**Quando é emitido**:
- Após transação ser aprovada pelo AntiFraud
- Após o usuário ter seu role atualizado
- Antes de salvar a transação

**Consumidores típicos**:
- Sistema de notificações (email de boas-vindas)
- Sistema de CRM (atualizar perfil do cliente)
- Sistema de onboarding (ativar features do plano)
- Sistema de analytics (conversão de vendas)

---

## Fluxo de Publicação

```
┌─────────────────────────────────────────────────────────────────┐
│                     TransactionService                           │
│                                                                   │
│  1. Cria Transaction                                             │
│  2. Chama AntiFraudService.validateTransaction()                 │
│                                                                   │
│  3a. Se FRAUDE:                                                  │
│      - transaction.reject()                                      │
│      - publishFraudDetectedEvent()        ──┐                    │
│                                              │                    │
│  3b. Se APROVADO:                            │                    │
│      - transaction.approve()                 │                    │
│      - user.updateRole()                     │                    │
│      - publishTransactionApprovedEvent()  ──┤                    │
│                                              │                    │
│  4. transactionRepository.save()             │                    │
│  5. publishTransactionValidatedEvent()    ──┤                    │
└──────────────────────────────────────────────┼───────────────────┘
                                               │
                                               ▼
                                    ┌──────────────────┐
                                    │ DomainEventPublisher│
                                    │  (RabbitTemplate)  │
                                    └─────────┬──────────┘
                                              │
                                              ▼
                                    ┌──────────────────┐
                                    │  RabbitMQ        │
                                    │  Exchange:       │
                                    │  melodyhub.events│
                                    └─────────┬────────┘
                                              │
                    ┌─────────────────────────┼─────────────────────┐
                    │                         │                     │
                    ▼                         ▼                     ▼
        ┌───────────────────┐   ┌───────────────────┐   ┌─────────────────┐
        │ fraud.detection   │   │ transaction.audit │   │ Outros          │
        │ Queue             │   │ Queue             │   │ Consumidores    │
        └───────────────────┘   └───────────────────┘   └─────────────────┘
```

## Como Consumir Eventos

### Opção 1: Spring AMQP Listener (dentro da aplicação)

```kotlin
@Component
class MyEventListener {

    @RabbitListener(queues = ["antifraud.fraud.detection"])
    fun handleFraudDetected(event: FraudDetectedEvent) {
        // Processar evento de fraude
        println("Fraude detectada: ${event.fraudReason}")
    }
}
```

### Opção 2: Microsserviço Separado

Crie um novo serviço Spring Boot que consome do mesmo exchange:

```kotlin
@Configuration
class RabbitConfig {
    @Bean
    fun myQueue() = Queue("meu-servico.frauds")

    @Bean
    fun binding(myQueue: Queue, exchange: TopicExchange) =
        BindingBuilder.bind(myQueue)
            .to(exchange)
            .with("antifraud.fraud.*")
}

@Component
class FraudProcessor {
    @RabbitListener(queues = ["meu-servico.frauds"])
    fun process(event: FraudDetectedEvent) {
        // Seu processamento aqui
    }
}
```

### Opção 3: Consumer em outra linguagem

Qualquer linguagem com suporte a AMQP pode consumir:

**Python**:
```python
import pika
import json

connection = pika.BlockingConnection(
    pika.ConnectionParameters('localhost')
)
channel = connection.channel()

channel.queue_declare(queue='my_fraud_queue')
channel.queue_bind(
    exchange='melodyhub.events',
    queue='my_fraud_queue',
    routing_key='antifraud.fraud.detected'
)

def callback(ch, method, properties, body):
    event = json.loads(body)
    print(f"Fraude: {event['fraudReason']}")

channel.basic_consume(
    queue='my_fraud_queue',
    on_message_callback=callback,
    auto_ack=True
)

channel.start_consuming()
```

## Configuração

### application.yml

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: melodyhub
    password: melodyhub123
    listener:
      simple:
        acknowledge-mode: auto
        retry:
          enabled: true
          initial-interval: 1000
          max-attempts: 3
```

### docker-compose.yml

```yaml
rabbitmq:
  image: rabbitmq:3.13-management-alpine
  ports:
    - "5672:5672"   # AMQP
    - "15672:15672" # Management UI
  environment:
    RABBITMQ_DEFAULT_USER: melodyhub
    RABBITMQ_DEFAULT_PASS: melodyhub123
```

## Management UI

Acesse: http://localhost:15672
- Username: `melodyhub`
- Password: `melodyhub123`

### O que você pode fazer:
- Ver exchanges e queues
- Monitorar mensagens em tempo real
- Ver bindings
- Publicar mensagens manualmente
- Ver estatísticas de consumo

## Testes

### Testar Publicação de Eventos

1. Crie uma transação via API:
```bash
curl -X POST http://localhost:8080/api/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "uuid-do-usuario",
    "subscriptionType": "PREMIUM",
    "creditCardId": 1
  }'
```

2. Verifique os logs da aplicação:
```bash
docker-compose logs -f app | grep "Publishing domain event"
```

3. Verifique o RabbitMQ Management UI:
   - Abra http://localhost:15672
   - Vá em "Queues"
   - Veja as mensagens chegando

4. Verifique os logs do listener:
```bash
docker-compose logs -f app | grep "FRAUD DETECTED\|TRANSACTION VALIDATED"
```

### Simular Fraude

Tente criar múltiplas transações rapidamente (viola regra de alta frequência):

```bash
#!/bin/bash
for i in {1..5}; do
  curl -X POST http://localhost:8080/api/transactions \
    -H "Content-Type: application/json" \
    -d '{
      "userId": "uuid-do-usuario",
      "subscriptionType": "PREMIUM",
      "creditCardId": 1
    }'
  sleep 10
done
```

Você verá eventos `FraudDetectedEvent` sendo publicados após a 3ª transação!

## Benefícios da Arquitetura

### 1. Desacoplamento
- AntiFraud não precisa conhecer os consumidores
- Novos consumidores podem ser adicionados sem modificar código

### 2. Escalabilidade
- Múltiplos consumidores podem processar eventos em paralelo
- Dead letter queues para mensagens com falha

### 3. Auditoria
- Todos os eventos são persistidos no RabbitMQ
- Trail completo de decisões de antifraude

### 4. Extensibilidade
- Fácil adicionar novos tipos de eventos
- Fácil adicionar novos consumidores

### 5. Resiliência
- Retry automático de mensagens
- Persistência de mensagens em disco
- Processamento assíncrono não bloqueia transações

## Context Mapping

Os eventos conectam os seguintes bounded contexts:

```
┌─────────────┐
│   Payment   │ (publica eventos)
└──────┬──────┘
       │
       ▼
┌─────────────┐
│  AntiFraud  │ (valida e publica eventos)
└──────┬──────┘
       │
       ├──> [Audit Context] (consome transaction.audit)
       ├──> [Alert Context] (consome fraud.detected)
       ├──> [CRM Context] (consome transaction.approved)
       └──> [Analytics Context] (consome todos)
```

## Roadmap Futuro

1. **Event Sourcing**: Armazenar todos os eventos em banco de eventos
2. **CQRS**: Separar leitura e escrita usando eventos
3. **Saga Pattern**: Orquestrar transações distribuídas
4. **Dead Letter Queue**: Tratar mensagens com falha
5. **Event Replay**: Reprocessar eventos históricos
6. **Event Versioning**: Suportar múltiplas versões de eventos

## Referências

- [Spring AMQP Documentation](https://spring.io/projects/spring-amqp)
- [RabbitMQ Tutorials](https://www.rabbitmq.com/getstarted.html)
- [Domain Events Pattern - Martin Fowler](https://martinfowler.com/eaaDev/DomainEvent.html)
- [CONTEXT_MAP.md](./CONTEXT_MAP.md) - Mapeamento completo dos bounded contexts
