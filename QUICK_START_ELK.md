# Quick Start - ELK Stack Observability

## 🚀 Start da Stack Completa

```bash
# Buildar e subir todos os serviços (app + DBs + ELK)
docker-compose up --build

# Ou em modo detached
docker-compose up -d --build
```

## 📊 Acessando os Serviços

| Serviço | URL | Descrição |
|---------|-----|-----------|
| **Aplicação** | http://localhost:8080 | API MelodyHub |
| **Kibana** | http://localhost:5601 | Dashboard de logs |
| **Elasticsearch** | http://localhost:9200 | API de busca |
| **Actuator** | http://localhost:8080/actuator/health | Health check |
| **Prometheus** | http://localhost:8080/actuator/prometheus | Métricas |

## 🔍 Primeiro Acesso ao Kibana

1. Acesse http://localhost:5601

2. Aguarde Kibana inicializar (pode levar 1-2 minutos)

3. Configure o Index Pattern:
   - Menu hamburguer → Management → Stack Management
   - Data Views → Create data view
   - Name: `melodyhub-logs`
   - Index pattern: `melodyhub-*`
   - Timestamp field: `@timestamp`
   - Save

4. Visualize os logs:
   - Menu hamburguer → Analytics → Discover
   - Selecione o data view `melodyhub-logs`
   - Ajuste o time range (últimas 15 minutos ou mais)

## 📝 Gerando Logs de Teste

```bash
# Criar usuário
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test User",
    "email": "test@example.com",
    "password": "senha123"
  }'

# Login (gera logs de autenticação)
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "senha123"
  }'

# Criar cartão de crédito (substitua USER_ID)
curl -X POST http://localhost:8080/api/credit-cards \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "USER_ID_AQUI",
    "cardNumber": "4111111111111111",
    "cardHolderName": "Test User",
    "expirationDate": "12/2025",
    "cvv": "123",
    "isActive": true
  }'

# Criar transação (gera logs de anti-fraud e domain events)
# Substitua USER_ID e CREDIT_CARD_ID
curl -X POST http://localhost:8080/api/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "USER_ID_AQUI",
    "subscriptionType": "BASIC",
    "creditCardId": "CARD_ID_AQUI"
  }'
```

## 🔎 Queries Úteis no Kibana

### Ver apenas erros
```
level: "ERROR"
```

### Eventos de domínio
```
tags: "domain_event"
```

### Transações aprovadas
```
eventType: "TransactionApproved"
```

### Fraudes detectadas
```
eventType: "FraudDetected" OR tags: "alert"
```

### Logs de autenticação
```
logger_name: *AuthService*
```

### Requisições HTTP lentas (> 1 segundo)
```
message: "HTTP Response" AND duration > 1000
```

### Logs de um usuário específico
```
userId: "uuid-aqui"
```

## 🛠️ Troubleshooting

### Logs não aparecem no Kibana

```bash
# 1. Verificar se app está gerando logs
docker exec -it melodyhub-app ls -la /var/log/melodyhub/

# 2. Ver conteúdo do log JSON
docker exec -it melodyhub-app tail -f /var/log/melodyhub/melodyhub.json

# 3. Verificar se Logstash está rodando
docker-compose logs logstash

# 4. Verificar se Elasticsearch tem índices
curl http://localhost:9200/_cat/indices?v

# 5. Contar documentos nos índices
curl http://localhost:9200/melodyhub-*/_count
```

### Elasticsearch não inicia

```bash
# Verificar logs
docker-compose logs elasticsearch

# Aumentar memória (se necessário)
# Editar docker-compose.yml:
# ES_JAVA_OPTS=-Xms1g -Xmx1g

# Reiniciar
docker-compose restart elasticsearch
```

### Logstash não processa logs

```bash
# Ver logs do Logstash
docker-compose logs -f logstash

# Testar pipeline (validação de sintaxe)
docker-compose run --rm logstash \
  bin/logstash -f /usr/share/logstash/pipeline/melodyhub.conf \
  --config.test_and_exit

# Reiniciar
docker-compose restart logstash
```

## 🏗️ Arquitetura Implementada (DDD)

```
┌─────────────────────────────────────────┐
│ DOMAIN LAYER                            │
│ - User, Transaction, Music (entidades)  │
│ - Domain Events (puros, sem logging)    │
└─────────────────────────────────────────┘
                  ↓
┌─────────────────────────────────────────┐
│ APPLICATION LAYER                       │
│ - AuthService (login logs)              │
│ - TransactionService (business logs)    │
│ - AntiFraudService (fraud detection)    │
└─────────────────────────────────────────┘
                  ↓
┌─────────────────────────────────────────┐
│ INFRASTRUCTURE LAYER                    │
│ ┌─────────────────────────────────────┐ │
│ │ Observability Components            │ │
│ │ - MdcFilter (trace ID)              │ │
│ │ - RequestLoggingFilter (HTTP logs)  │ │
│ │ - UserContextEnricher (MDC)         │ │
│ │ - DomainEventLogger (events)        │ │
│ └─────────────────────────────────────┘ │
└─────────────────────────────────────────┘
                  ↓
         ┌────────────────┐
         │  Logback JSON  │
         │  /var/log/     │
         └────────────────┘
                  ↓
         ┌────────────────┐
         │   Logstash     │
         │  (processing)  │
         └────────────────┘
                  ↓
         ┌────────────────┐
         │ Elasticsearch  │
         │   (storage)    │
         └────────────────┘
                  ↓
         ┌────────────────┐
         │    Kibana      │
         │ (visualization)│
         └────────────────┘
```

## 📚 Logs Capturados

### Por Camada (DDD)

1. **Domain Events** (via RabbitMQ listener):
   - TransactionApprovedEvent
   - FraudDetectedEvent
   - UserSubscriptionUpgradedEvent
   - TransactionValidatedEvent

2. **Application Services**:
   - Login attempts (success/failure)
   - Transaction creation
   - Anti-fraud validation (all rules)
   - User operations

3. **Infrastructure**:
   - HTTP requests/responses
   - Request latency
   - Trace IDs
   - User context

### Campos MDC Disponíveis

Todos os logs incluem automaticamente:
- `traceId`: Identificador único da requisição
- `userId`: UUID do usuário (quando disponível)
- `userEmail`: Email do usuário
- `userRole`: Role do usuário (SEM_PLANO, BASIC, PREMIUM, ADMIN)
- `transactionId`: UUID da transação
- `musicId`: ID da música
- `playlistId`: UUID da playlist
- `eventType`: Tipo do evento de domínio
- `application`: "melodyhub"
- `environment`: Profile do Spring (default, test, prod)

## 📈 Próximos Passos

1. **Criar Dashboards Personalizados**:
   - Dashboard de transações (aprovadas vs rejeitadas)
   - Dashboard de fraudes (razões mais comuns)
   - Dashboard de performance (latência por endpoint)

2. **Configurar Alertas**:
   - Spike de fraudes
   - Latência alta
   - Erros 5xx

3. **Explorar Kibana**:
   - Visualizações (pie charts, line charts, tables)
   - Saved searches
   - Canvas (relatórios customizados)

## 📖 Documentação Completa

Para mais detalhes, consulte:
- **ELK.md**: Documentação completa da Stack ELK
- **CLAUDE.md**: Guia do projeto (seção Observability)

## ✅ Checklist de Validação

- [ ] Elasticsearch rodando (http://localhost:9200)
- [ ] Kibana acessível (http://localhost:5601)
- [ ] Index pattern criado no Kibana
- [ ] Logs aparecendo no Discover
- [ ] Trace ID presente nos logs
- [ ] Domain events sendo logados
- [ ] Anti-fraud logs funcionando
- [ ] HTTP request/response logs ativos
