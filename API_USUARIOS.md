# API de Usuários - MelodyHub

## Parte 1: Funcionalidade Implementada

✅ **Criação de conta**: cadastro de novos usuários com nome e e-mail

## Endpoints Disponíveis

### 1. Criar Novo Usuário
**POST** `/api/users`

**Request Body:**
```json
{
  "name": "João Silva",
  "email": "joao@email.com"
}
```

**Response (201 Created):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "João Silva",
  "email": "joao@email.com",
  "createdAt": "2025-11-09T20:00:00",
  "updatedAt": "2025-11-09T20:00:00"
}
```

**Validações:**
- Nome é obrigatório (não pode ser vazio)
- E-mail é obrigatório (não pode ser vazio)
- E-mail deve ser válido (formato correto)
- E-mail não pode estar duplicado no sistema

**Erros Possíveis:**
- `400 Bad Request`: Quando dados são inválidos
- `400 Bad Request`: Quando e-mail já está cadastrado

---

### 2. Buscar Usuário por ID
**GET** `/api/users/{id}`

**Response (200 OK):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "João Silva",
  "email": "joao@email.com",
  "createdAt": "2025-11-09T20:00:00",
  "updatedAt": "2025-11-09T20:00:00"
}
```

**Erros Possíveis:**
- `404 Not Found`: Quando usuário não existe

---

### 3. Buscar Usuário por E-mail
**GET** `/api/users/email/{email}`

**Exemplo:** `/api/users/email/joao@email.com`

**Response (200 OK):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "João Silva",
  "email": "joao@email.com",
  "createdAt": "2025-11-09T20:00:00",
  "updatedAt": "2025-11-09T20:00:00"
}
```

**Erros Possíveis:**
- `404 Not Found`: Quando usuário não existe

---

### 4. Listar Todos os Usuários
**GET** `/api/users`

**Response (200 OK):**
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "João Silva",
    "email": "joao@email.com",
    "createdAt": "2025-11-09T20:00:00",
    "updatedAt": "2025-11-09T20:00:00"
  },
  {
    "id": "660e8400-e29b-41d4-a716-446655440001",
    "name": "Maria Santos",
    "email": "maria@email.com",
    "createdAt": "2025-11-09T20:05:00",
    "updatedAt": "2025-11-09T20:05:00"
  }
]
```

---

### 5. Deletar Usuário
**DELETE** `/api/users/{id}`

**Response (204 No Content)**

**Erros Possíveis:**
- `404 Not Found`: Quando usuário não existe

---

## Arquitetura Implementada

### Domain Driven Design (DDD)

O projeto foi estruturado seguindo os princípios de DDD:

#### 📁 Domain (Domínio)
- **`User.kt`**: Entidade de domínio com comportamentos ricos
  - Validações no domínio
  - Métodos de negócio (`updateName`, `updateEmail`)
  - Encapsulamento adequado

- **`UserRepository.kt`**: Interface de repositório (inversão de dependência)

#### 📁 Application (Camada de Aplicação)
- **`UserService.kt`**: Serviço de domínio com regras de negócio
  - Validação de e-mail duplicado
  - Orquestração de operações
  - Transações

- **DTOs**:
  - `CreateUserRequest`: Entrada de dados
  - `UserResponse`: Saída de dados

#### 📁 Infrastructure (Infraestrutura)
- **`JpaUserRepository.kt`**: Interface Spring Data JPA
- **`UserRepositoryImpl.kt`**: Implementação do repositório do domínio
- **`UserController.kt`**: Controller REST

### Princípios SOLID Aplicados

1. **Single Responsibility**: Cada classe tem uma única responsabilidade
2. **Open/Closed**: Aberto para extensão, fechado para modificação
3. **Liskov Substitution**: UserRepositoryImpl substitui UserRepository
4. **Interface Segregation**: Interfaces específicas e coesas
5. **Dependency Inversion**: Dependência de abstrações (UserRepository interface)

### Clean Code

- Nomes reveladores de intenção
- Métodos pequenos e focados
- Tratamento adequado de erros
- Validações claras
- Código legível e bem organizado

---

## Como Executar

### Pré-requisitos
- Java 17+
- PostgreSQL rodando na porta 5432
- Banco de dados `melodyhub` criado

### Configuração do Banco de Dados

1. Inicie o PostgreSQL
2. Crie o banco de dados:
```sql
CREATE DATABASE melodyhub;
```

3. (Opcional) Ajuste as credenciais em `src/main/resources/application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/melodyhub
    username: postgres
    password: postgres
```

### Executar a Aplicação

```bash
./gradlew bootRun
```

A aplicação estará disponível em: `http://localhost:8080`

---

## Exemplo de Uso com cURL

### Criar Usuário
```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "João Silva",
    "email": "joao@email.com"
  }'
```

### Listar Usuários
```bash
curl http://localhost:8080/api/users
```

### Buscar por ID
```bash
curl http://localhost:8080/api/users/{id}
```

### Buscar por E-mail
```bash
curl http://localhost:8080/api/users/email/joao@email.com
```

---

## Estrutura de Arquivos

```
src/main/kotlin/edu/infnet/melodyhub/
├── MelodyHubApplication.kt
├── domain/
│   └── user/
│       ├── User.kt                    # Entidade de domínio
│       └── UserRepository.kt          # Interface do repositório
├── application/
│   └── user/
│       ├── UserService.kt             # Serviço de domínio
│       └── dto/
│           ├── CreateUserRequest.kt   # DTO de entrada
│           └── UserResponse.kt        # DTO de saída
└── infrastructure/
    ├── user/
    │   ├── JpaUserRepository.kt       # Spring Data JPA
    │   └── UserRepositoryImpl.kt      # Implementação do repositório
    └── web/
        └── UserController.kt          # REST Controller
```
