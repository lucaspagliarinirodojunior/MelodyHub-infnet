# API de Transações - MelodyHub

## Parte 2: Funcionalidade Implementada

✅ **Autorização de transação**: permitir pagamentos de assinatura, validando regras de antifraude (simuladas)

## Endpoints Disponíveis

### 1. Criar Nova Transação (Autorização de Pagamento)
**POST** `/api/transactions`

**Request Body:**
```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "subscriptionType": "BASIC"
}
```

**Tipos de Assinatura Disponíveis:**
- `BASIC` - R$ 9,90/mês
- `PREMIUM` - R$ 19,90/mês
- `FAMILY` - R$ 29,90/mês

**Response (201 Created) - Transação Aprovada:**
```json
{
  "id": "770e8400-e29b-41d4-a716-446655440003",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "amount": 9.90,
  "subscriptionType": "BASIC",
  "status": "APPROVED",
  "fraudReason": null,
  "createdAt": "2025-11-09T21:00:00",
  "updatedAt": "2025-11-09T21:00:00"
}
```

**Response (201 Created) - Transação Rejeitada por Antifraude:**
```json
{
  "id": "770e8400-e29b-41d4-a716-446655440004",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "amount": 29.90,
  "subscriptionType": "FAMILY",
  "status": "REJECTED",
  "fraudReason": "Múltiplas transações detectadas em curto período (mais de 3 em 1 minuto)",
  "createdAt": "2025-11-09T21:00:10",
  "updatedAt": "2025-11-09T21:00:10"
}
```

**Validações:**
- User ID é obrigatório
- Tipo de assinatura é obrigatório
- Usuário deve existir no sistema

**Regras de Antifraude (Simuladas):**
1. ✅ **Valor Positivo**: O valor da transação deve ser maior que zero
2. ✅ **Limite Máximo**: Transações acima de R$ 100,00 são rejeitadas (limite para demonstração)
3. ✅ **Taxa de Transações**: Máximo de 3 transações por minuto por usuário
4. ✅ **Limite Diário**: Máximo de 5 transações por dia por usuário

**Erros Possíveis:**
- `400 Bad Request`: Quando dados são inválidos
- `400 Bad Request`: Quando usuário não existe

---

### 2. Buscar Transação por ID
**GET** `/api/transactions/{id}`

**Response (200 OK):**
```json
{
  "id": "770e8400-e29b-41d4-a716-446655440003",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "amount": 9.90,
  "subscriptionType": "BASIC",
  "status": "APPROVED",
  "fraudReason": null,
  "createdAt": "2025-11-09T21:00:00",
  "updatedAt": "2025-11-09T21:00:00"
}
```

**Erros Possíveis:**
- `400 Bad Request`: Quando transação não existe

---

### 3. Buscar Transações por Usuário
**GET** `/api/transactions/user/{userId}`

**Exemplo:** `/api/transactions/user/550e8400-e29b-41d4-a716-446655440000`

**Response (200 OK):**
```json
[
  {
    "id": "770e8400-e29b-41d4-a716-446655440003",
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "amount": 9.90,
    "subscriptionType": "BASIC",
    "status": "APPROVED",
    "fraudReason": null,
    "createdAt": "2025-11-09T21:00:00",
    "updatedAt": "2025-11-09T21:00:00"
  },
  {
    "id": "770e8400-e29b-41d4-a716-446655440004",
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "amount": 19.90,
    "subscriptionType": "PREMIUM",
    "status": "REJECTED",
    "fraudReason": "Múltiplas transações detectadas em curto período",
    "createdAt": "2025-11-09T21:00:05",
    "updatedAt": "2025-11-09T21:00:05"
  }
]
```

---

### 4. Listar Todas as Transações
**GET** `/api/transactions`

**Response (200 OK):**
```json
[
  {
    "id": "770e8400-e29b-41d4-a716-446655440003",
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "amount": 9.90,
    "subscriptionType": "BASIC",
    "status": "APPROVED",
    "fraudReason": null,
    "createdAt": "2025-11-09T21:00:00",
    "updatedAt": "2025-11-09T21:00:00"
  },
  {
    "id": "770e8400-e29b-41d4-a716-446655440005",
    "userId": "660e8400-e29b-41d4-a716-446655440001",
    "amount": 29.90,
    "subscriptionType": "FAMILY",
    "status": "APPROVED",
    "fraudReason": null,
    "createdAt": "2025-11-09T21:05:00",
    "updatedAt": "2025-11-09T21:05:00"
  }
]
```

---

## Arquitetura Implementada

### Domain Driven Design (DDD)

A funcionalidade de transações segue os mesmos princípios de DDD aplicados na funcionalidade de usuários:

#### 📁 Domain (Domínio)
- **`Transaction.kt`**: Entidade de domínio rica
  - Comportamentos: `approve()`, `reject(reason)`, `isApproved()`, `isRejected()`
  - Validações de estado (apenas transações pendentes podem ser aprovadas/rejeitadas)
  - Enum `SubscriptionType` com valores das assinaturas
  - Enum `TransactionStatus` (PENDING, APPROVED, REJECTED)

- **`TransactionRepository.kt`**: Interface de repositório (inversão de dependência)

#### 📁 Application (Camada de Aplicação)
- **`TransactionService.kt`**: Serviço de domínio com lógica de negócio
  - Validação de existência do usuário
  - Orquestração do processo de autorização
  - Integração com serviço de antifraude

- **`AntiFraudService.kt`**: Serviço especializado em regras de antifraude
  - Validação de valor positivo
  - Validação de limite máximo
  - Validação de taxa de transações
  - Validação de limite diário
  - Retorna resultado com motivo de rejeição

- **DTOs**:
  - `CreateTransactionRequest`: Entrada de dados
  - `TransactionResponse`: Saída de dados

#### 📁 Infrastructure (Infraestrutura)
- **`JpaTransactionRepository.kt`**: Interface Spring Data JPA
- **`TransactionRepositoryImpl.kt`**: Implementação do repositório do domínio
- **`TransactionController.kt`**: Controller REST

### Princípios SOLID Aplicados

1. **Single Responsibility**:
   - `Transaction`: Gerencia estado da transação
   - `TransactionService`: Orquestra criação de transações
   - `AntiFraudService`: Valida regras de antifraude

2. **Open/Closed**: Novas regras de antifraude podem ser adicionadas sem modificar código existente

3. **Liskov Substitution**: `TransactionRepositoryImpl` substitui `TransactionRepository`

4. **Interface Segregation**: Interfaces específicas e coesas

5. **Dependency Inversion**: Dependência de abstrações (`TransactionRepository`, `UserRepository`)

### Clean Code

- Nomes reveladores de intenção (`approve()`, `reject()`, `validateTransaction()`)
- Métodos pequenos e focados
- Separação de responsabilidades (negócio vs antifraude)
- Tratamento adequado de erros
- Validações claras e explícitas
- Código legível e bem organizado

### Domain Services

O `AntiFraudService` é um exemplo de Domain Service, implementando lógica de negócio que:
- Não pertence a uma única entidade
- Coordena múltiplas validações
- Pode ser reutilizado em diferentes contextos
- Mantém o domínio rico e expressivo

---

## Cenários de Teste

### Cenário 1: Transação Aprovada com Sucesso
```bash
# 1. Criar usuário
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "João Silva",
    "email": "joao@email.com"
  }'

# Resposta: guarde o "id" do usuário

# 2. Criar transação (use o ID do usuário)
curl -X POST http://localhost:8080/api/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "SEU_USER_ID_AQUI",
    "subscriptionType": "BASIC"
  }'

# Resultado esperado: status "APPROVED"
```

### Cenário 2: Transação Rejeitada - Múltiplas em Curto Período
```bash
# Execute 4 transações rapidamente (menos de 1 minuto entre elas)
# A 4ª transação será rejeitada
for i in {1..4}; do
  curl -X POST http://localhost:8080/api/transactions \
    -H "Content-Type: application/json" \
    -d '{
      "userId": "SEU_USER_ID_AQUI",
      "subscriptionType": "BASIC"
    }'
  echo ""
done

# Resultado esperado: 3 aprovadas, 1 rejeitada com motivo "Múltiplas transações..."
```

### Cenário 3: Transação Rejeitada - Usuário Inexistente
```bash
curl -X POST http://localhost:8080/api/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "00000000-0000-0000-0000-000000000000",
    "subscriptionType": "PREMIUM"
  }'

# Resultado esperado: erro 400 "User not found"
```

### Cenário 4: Consultar Transações de um Usuário
```bash
curl http://localhost:8080/api/transactions/user/SEU_USER_ID_AQUI
```

---

## Estrutura de Arquivos

```
src/main/kotlin/edu/infnet/melodyhub/
├── domain/
│   └── transaction/
│       ├── Transaction.kt              # Entidade de domínio
│       ├── SubscriptionType.kt         # Enum (parte de Transaction.kt)
│       ├── TransactionStatus.kt        # Enum (parte de Transaction.kt)
│       └── TransactionRepository.kt    # Interface do repositório
├── application/
│   └── transaction/
│       ├── TransactionService.kt       # Serviço de domínio
│       ├── AntiFraudService.kt         # Serviço de antifraude (parte de TransactionService.kt)
│       └── dto/
│           ├── CreateTransactionRequest.kt   # DTO de entrada
│           └── TransactionResponse.kt        # DTO de saída
└── infrastructure/
    ├── transaction/
    │   ├── JpaTransactionRepository.kt       # Spring Data JPA
    │   └── TransactionRepositoryImpl.kt      # Implementação do repositório
    └── web/
        └── TransactionController.kt          # REST Controller
```

---

## Observações Importantes

### Simulação de Antifraude

As regras de antifraude implementadas são **simuladas** e **simplificadas** para fins educacionais:

- ✅ Demonstram o conceito de validação de regras de negócio
- ✅ Mostram separação de responsabilidades (Domain Service)
- ✅ Validam cenários básicos de fraude
- ❌ NÃO são regras reais de produção
- ❌ NÃO utilizam machine learning ou algoritmos avançados
- ❌ NÃO integram com serviços externos de antifraude

Em um sistema real, o serviço de antifraude seria muito mais complexo, incluindo:
- Análise de padrões de comportamento
- Validação de geolocalização
- Verificação de dispositivo
- Score de risco
- Integração com bureaus de crédito
- Machine learning para detecção de anomalias

### Próximos Passos (Não Implementados)

Para tornar este sistema mais robusto, seria necessário:
1. Adicionar autenticação e autorização (JWT, OAuth2)
2. Implementar processamento assíncrono de transações
3. Adicionar eventos de domínio (DDD)
4. Implementar padrão Saga para transações distribuídas
5. Adicionar auditoria completa de transações
6. Implementar cache para consultas frequentes
7. Adicionar métricas e monitoramento
