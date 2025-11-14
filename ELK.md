# Observabilidade com ELK Stack - MelodyHub

## Visão Geral

O MelodyHub implementa observabilidade completa utilizando a Stack ELK (Elasticsearch, Logstash, Kibana) seguindo rigorosamente os princípios de Domain-Driven Design (DDD). Esta implementação permite:

- **Monitoramento em tempo real** de eventos de domínio
- **Análise de segurança** através de logs estruturados de autenticação e anti-fraude
- **Rastreamento distribuído** com trace IDs em todas as requisições
- **Métricas de negócio** para análise de transações e comportamento de usuários

## Arquitetura de Logging (DDD)

### Separação por Camadas

A implementação respeita a arquitetura em camadas do DDD:

```
┌─────────────────────────────────────────────────────────┐
│ DOMAIN LAYER (Pura, sem logging direto)                │
│ - Entidades e Agregados                                 │
│ - Domain Events (observados pela infraestrutura)        │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│ APPLICATION LAYER (Logging de casos de uso)            │
│ - Services com logging de operações de negócio          │
│ - Anti-fraud, Auth, Transaction, User, Music            │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│ INFRASTRUCTURE LAYER (Logging técnico)                  │
│ - HTTP Request/Response Logging                         │
│ - MDC Context Enrichment                                │
│ - Domain Event Logging                                  │
└─────────────────────────────────────────────────────────┘
```

### Componentes de Infraestrutura

#### 1. MdcFilter
**Localização**: `infrastructure/observability/MdcFilter.kt`

Adiciona contexto a todos os logs via MDC (Mapped Diagnostic Context):
- `traceId`: Identificador único para rastreamento de requisições
- `requestUri` e `requestMethod`: Contexto HTTP
- `userId`, `userEmail`, `userRole`: Contexto do usuário autenticado
- Header `X-Trace-Id` em todas as respostas

#### 2. RequestLoggingFilter
**Localização**: `infrastructure/observability/RequestLoggingFilter.kt`

Registra todas as requisições HTTP:
- Método, URI, Content-Type
- Status code da resposta
- Tempo de processamento (latência)
- Ignora endpoints `/actuator/*` para reduzir ruído

#### 3. UserContextEnricher
**Localização**: `infrastructure/observability/UserContextEnricher.kt`

Utilitário para enriquecer MDC com contextos específicos:
- `enrichWithUserContext()`: Adiciona dados do usuário
- `enrichWithTransactionContext()`: Adiciona ID de transação
- `enrichWithMusicContext()`: Adiciona ID de música
- `enrichWithEventContext()`: Adiciona tipo de evento de domínio

#### 4. DomainEventLogger
**Localização**: `infrastructure/observability/DomainEventLogger.kt`

Listener que captura eventos de domínio via RabbitMQ e gera logs estruturados:
- `TransactionApprovedEvent`
- `FraudDetectedEvent`
- `UserSubscriptionUpgradedEvent`
- `TransactionValidatedEvent`

**Importante**: Segue DDD ao observar (não modificar) eventos de domínio.

## Stack ELK - Componentes

### Elasticsearch
**Porta**: 9200
**Função**: Armazenamento e indexação de logs

Configurado para:
- Modo single-node (desenvolvimento)
- Segurança desabilitada (simplificação)
- Heap size: 512MB (otimizado para ambiente local)

### Logstash
**Portas**: 5044 (input), 9600 (monitoring)
**Função**: Processamento e enriquecimento de logs

**Pipeline**: `/logstash/pipeline/melodyhub.conf`

Processamento aplicado:
1. Leitura de logs JSON da aplicação
2. Parsing de timestamps
3. Categorização por nível (ERROR, WARN → tag "alert")
4. Tags especiais:
   - `domain_event`: Eventos de domínio
   - `transaction`: Logs relacionados a transações
   - `user_activity`: Atividades de usuário
   - `music_activity`: Operações com músicas/playlists
5. Indexação em Elasticsearch com padrão `melodyhub-YYYY.MM.dd`

### Kibana
**Porta**: 5601
**Função**: Visualização e análise de logs

Interface web para:
- Pesquisa em tempo real
- Dashboards customizados
- Alertas e relatórios

## Configuração de Logs

### Logback (JSON Estruturado)
**Arquivo**: `src/main/resources/logback-spring.xml`

**Appenders**:
- `CONSOLE`: Logs legíveis para desenvolvimento
- `JSON_FILE`: Logs estruturados em JSON para ELK
- `ASYNC_JSON`: Wrapper assíncrono para performance

**Campos MDC incluídos automaticamente**:
```json
{
  "traceId": "uuid-da-requisicao",
  "userId": "uuid-do-usuario",
  "userEmail": "email@exemplo.com",
  "userRole": "PREMIUM",
  "transactionId": "uuid-da-transacao",
  "musicId": "objectid-da-musica",
  "playlistId": "uuid-da-playlist",
  "eventType": "TransactionApproved",
  "application": "melodyhub",
  "environment": "default"
}
```

**Política de rotação**:
- Tamanho máximo por arquivo: 100MB
- Retenção: 30 dias
- Tamanho total: 3GB

### Níveis de Log por Camada

```yaml
# Domain Events
edu.infnet.melodyhub.domain.events: INFO

# Application Services
edu.infnet.melodyhub.application: INFO

# Infrastructure
edu.infnet.melodyhub.infrastructure: INFO

# Security (mais detalhado)
edu.infnet.melodyhub.infrastructure.security: DEBUG
```

## Uso e Desenvolvimento

### Iniciando a Stack ELK

```bash
# Subir todos os serviços (app + databases + ELK)
docker-compose up -d

# Verificar status
docker-compose ps

# Logs da aplicação
docker-compose logs -f app

# Logs do Logstash (troubleshooting)
docker-compose logs -f logstash
```

### Acessando Kibana

1. Abra o navegador em: http://localhost:5601

2. **Primeira configuração**:
   - Navegue para: Management → Stack Management → Index Patterns
   - Clique em "Create index pattern"
   - Pattern: `melodyhub-*`
   - Time field: `@timestamp`
   - Clique em "Create index pattern"

3. **Visualizando logs**:
   - Menu lateral → Analytics → Discover
   - Selecione o index pattern `melodyhub-*`
   - Ajuste o time range (canto superior direito)

### Queries Úteis no Kibana

#### Filtrar por nível de log
```
level: "ERROR"
level: "WARN"
```

#### Eventos de domínio
```
tags: "domain_event"
eventType: "TransactionApproved"
eventType: "FraudDetected"
```

#### Transações de um usuário específico
```
userId: "uuid-do-usuario" AND tags: "transaction"
```

#### Detecção de fraudes
```
tags: "alert" AND logger_name: *AntiFraudService*
```

#### Atividades de autenticação
```
logger_name: *AuthService*
```

#### Latência de requisições
```
message: "HTTP Response" AND duration > 1000
```

### Adicionando Logs em Novos Services (DDD)

**Princípio**: Logs são uma preocupação técnica, não de domínio.

**❌ NÃO FAZER** (Domain Layer):
```kotlin
// domain/user/User.kt
class User {
    fun upgradeRole(newRole: UserRole) {
        logger.info("Upgrading role") // ❌ Domain não loga!
        this.role = newRole
    }
}
```

**✅ FAZER** (Application Layer):
```kotlin
// application/user/UserService.kt
@Service
class UserService(
    private val userRepository: UserRepository,
    private val userContextEnricher: UserContextEnricher
) {
    private val logger = LoggerFactory.getLogger(UserService::class.java)

    fun upgradeUserRole(userId: UUID, newRole: UserRole) {
        // Enriquecer contexto MDC
        userContextEnricher.enrichWithUserContext(
            userId.toString(),
            null,
            newRole.name
        )

        // Logar operação de negócio
        logger.info(
            "Upgrading user role: userId={}, newRole={}",
            userId,
            newRole
        )

        val user = userRepository.findById(userId)
            ?: throw IllegalArgumentException("User not found")

        user.upgradeRole(newRole) // Domain puro
        userRepository.save(user)

        logger.info("User role upgraded successfully: userId={}", userId)
    }
}
```

### Criando Dashboards no Kibana

#### Dashboard de Transações

1. **Visualização: Transações por Status**
   - Tipo: Pie Chart
   - Bucket: Terms aggregation em `status.keyword`

2. **Visualização: Fraudes ao Longo do Tempo**
   - Tipo: Line Chart
   - X-axis: Date Histogram em `@timestamp`
   - Y-axis: Count onde `tags: "domain_event" AND eventType: "FraudDetected"`

3. **Visualização: Top Razões de Rejeição**
   - Tipo: Data Table
   - Aggregation: Terms em `fraudReason.keyword`
   - Métrica: Count

#### Dashboard de Performance

1. **Visualização: Latência Média por Endpoint**
   - Tipo: Bar Chart
   - X-axis: Terms em `requestUri.keyword`
   - Y-axis: Average em `duration`

2. **Visualização: Requisições por Segundo**
   - Tipo: Line Chart
   - X-axis: Date Histogram (1m interval)
   - Y-axis: Count

## Monitoramento e Alertas

### Endpoints do Spring Boot Actuator

```bash
# Health check
curl http://localhost:8080/actuator/health

# Métricas Prometheus
curl http://localhost:8080/actuator/metrics

# Logs levels
curl http://localhost:8080/actuator/loggers
```

### Health Checks dos Serviços ELK

```bash
# Elasticsearch
curl http://localhost:9200/_cluster/health?pretty

# Logstash
curl http://localhost:9600/_node/stats?pretty

# Kibana
curl http://localhost:5601/api/status
```

## Troubleshooting

### Logs não aparecem no Kibana

1. **Verificar se os logs estão sendo gerados**:
   ```bash
   docker exec -it melodyhub-app ls -la /var/log/melodyhub/
   docker exec -it melodyhub-app cat /var/log/melodyhub/melodyhub.json
   ```

2. **Verificar se Logstash está processando**:
   ```bash
   docker-compose logs logstash | grep "Pipeline started"
   docker-compose logs logstash | grep ERROR
   ```

3. **Verificar se Elasticsearch está recebendo dados**:
   ```bash
   curl http://localhost:9200/_cat/indices?v
   curl http://localhost:9200/melodyhub-*/_count
   ```

4. **Recriar índices** (⚠️ deleta dados):
   ```bash
   curl -X DELETE http://localhost:9200/melodyhub-*
   docker-compose restart logstash
   ```

### Elasticsearch com pouca memória

Ajuste no `docker-compose.yml`:
```yaml
elasticsearch:
  environment:
    - "ES_JAVA_OPTS=-Xms1g -Xmx1g"  # Aumentar para 1GB
```

### Logstash não inicia

1. Verificar sintaxe do pipeline:
   ```bash
   docker-compose run --rm logstash \
     bin/logstash -f /usr/share/logstash/pipeline/melodyhub.conf --config.test_and_exit
   ```

## Boas Práticas Implementadas (DDD)

### ✅ Separação de Responsabilidades
- **Domain**: Pura, sem dependências de logging
- **Application**: Logs de casos de uso e regras de negócio
- **Infrastructure**: Logs técnicos (HTTP, eventos, contexto)

### ✅ Structured Logging
- Todos os logs em formato JSON
- Campos padronizados e tipados
- Contexto MDC automático

### ✅ Observabilidade de Eventos de Domínio
- Listener dedicado (`DomainEventLogger`)
- Não polui lógica de domínio
- Rastreamento completo do ciclo de vida

### ✅ Rastreamento Distribuído
- Trace ID em todas as requisições
- Propagado via headers HTTP
- Permite correlacionar logs end-to-end

### ✅ Performance
- Logging assíncrono (não bloqueia aplicação)
- Rotação automática de arquivos
- Níveis de log ajustáveis por camada

## Segurança

**⚠️ Importante para Produção**:

1. **Elasticsearch**: Habilitar autenticação (X-Pack Security)
2. **Kibana**: Configurar autenticação e RBAC
3. **Logstash**: Usar SSL/TLS para comunicação
4. **Logs**: Nunca logar senhas, tokens JWT completos ou dados sensíveis
5. **Retention**: Implementar política de retenção LGPD-compliant

No código atual:
- ✅ Senhas nunca são logadas (apenas hash BCrypt)
- ✅ Tokens JWT não são logados (apenas userId extraído)
- ✅ Dados de cartão de crédito não são expostos em logs

## Próximos Passos

1. **Alertas Proativos**: Configurar alertas no Kibana para:
   - Spike de fraudes detectadas
   - Latência acima de threshold
   - Erros 5xx em produção

2. **Métricas de Negócio**: Dashboards para:
   - Taxa de conversão (transações aprovadas/rejeitadas)
   - Distribuição de planos (BASIC vs PREMIUM)
   - Músicas mais acessadas

3. **Correlação com Traces**: Integrar com APM (Application Performance Monitoring)

4. **Machine Learning**: Usar Elasticsearch ML para detecção de anomalias em padrões de fraude

## Referências

- [Elastic Stack Documentation](https://www.elastic.co/guide/index.html)
- [Logstash Encoder for Logback](https://github.com/logfellow/logstash-logback-encoder)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [DDD and Logging Best Practices](https://www.domainlanguage.com/)
