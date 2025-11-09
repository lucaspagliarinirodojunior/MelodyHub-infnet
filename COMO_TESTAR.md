# Como Testar a API de Usuários

## Pré-requisitos

1. **Java 17+** instalado
2. **PostgreSQL** rodando (ou use o profile test com H2)
3. **curl** (para testes de API)
4. **jq** (opcional, para formatar JSON no terminal)

## Opção 1: Testar com H2 (Banco em Memória - Recomendado para testes rápidos)

### 1. Iniciar a aplicação com profile test

```bash
./gradlew bootRun --args='--spring.profiles.active=test'
```

### 2. Executar script de testes automatizado

Em outro terminal:

```bash
./test-api.sh
```

## Opção 2: Testar com PostgreSQL (Produção)

### 1. Iniciar PostgreSQL e criar banco

```bash
# Iniciar PostgreSQL
service postgresql start

# Criar banco de dados
sudo -u postgres psql -c "CREATE DATABASE melodyhub;"

# Definir senha do usuário postgres (se necessário)
sudo -u postgres psql -c "ALTER USER postgres PASSWORD 'postgres';"
```

### 2. Iniciar a aplicação

```bash
./gradlew bootRun
```

### 3. Executar testes

```bash
./test-api.sh
```

## Testes Manuais com cURL

### 1. Criar um usuário

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "João Silva",
    "email": "joao@email.com"
  }'
```

**Resposta esperada (201 Created):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "João Silva",
  "email": "joao@email.com",
  "createdAt": "2025-11-09T20:00:00",
  "updatedAt": "2025-11-09T20:00:00"
}
```

### 2. Listar todos os usuários

```bash
curl -X GET http://localhost:8080/api/users
```

### 3. Buscar usuário por ID

```bash
curl -X GET http://localhost:8080/api/users/{id}
```

### 4. Buscar usuário por e-mail

```bash
curl -X GET http://localhost:8080/api/users/email/joao@email.com
```

### 5. Tentar criar usuário com e-mail duplicado

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Outro João",
    "email": "joao@email.com"
  }'
```

**Resposta esperada (400 Bad Request):**
```json
{
  "message": "E-mail já cadastrado: joao@email.com"
}
```

### 6. Tentar criar usuário com dados inválidos

```bash
# Nome vazio
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "",
    "email": "teste@email.com"
  }'

# E-mail inválido
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Teste",
    "email": "email-invalido"
  }'
```

**Resposta esperada (400 Bad Request):**
```json
{
  "message": "Nome é obrigatório"
}
```

ou

```json
{
  "message": "E-mail deve ser válido"
}
```

### 7. Deletar usuário

```bash
curl -X DELETE http://localhost:8080/api/users/{id}
```

**Resposta esperada (204 No Content)**

## Verificar se a aplicação está rodando

```bash
curl http://localhost:8080/api/users
```

Se retornar uma lista (mesmo vazia `[]`), a API está funcionando!

## Console H2 (apenas com profile test)

Quando usar o profile test, você pode acessar o console do H2:

```
http://localhost:8080/h2-console
```

**Configurações de conexão:**
- JDBC URL: `jdbc:h2:mem:melodyhub`
- Username: `sa`
- Password: (deixe em branco)

## Troubleshooting

### Erro: "Address already in use"

A porta 8080 já está em uso. Pare outros processos ou altere a porta no `application.yml`:

```yaml
server:
  port: 8081
```

### Erro: Connection refused ao PostgreSQL

1. Verifique se o PostgreSQL está rodando: `pg_isready`
2. Verifique as credenciais em `application.yml`
3. Certifique-se de que o banco `melodyhub` foi criado

### Erro: "Table not found"

O Hibernate deve criar as tabelas automaticamente. Verifique se:
- `spring.jpa.hibernate.ddl-auto` está configurado como `update` ou `create-drop`
- As entities têm a anotação `@Entity` corretamente

## Estrutura de Validações Implementadas

✅ **Nome obrigatório** - não pode ser vazio
✅ **E-mail obrigatório** - não pode ser vazio
✅ **E-mail válido** - formato correto de e-mail
✅ **E-mail único** - não permite duplicação
✅ **Tratamento de erros** - mensagens claras para o usuário

## Exemplos de Respostas de Erro

### E-mail duplicado (400)
```json
{
  "message": "E-mail já cadastrado: joao@email.com"
}
```

### Validação falhou (400)
```json
{
  "message": "Nome é obrigatório"
}
```

### Usuário não encontrado (404)
```json
{
  "message": "Usuário não encontrado com ID: 550e8400-e29b-41d4-a716-446655440000"
}
```

## Logs da Aplicação

Os logs da aplicação mostrarão:
- Queries SQL executadas (quando `show-sql: true`)
- Erros de validação
- Stacktraces de exceções

Para visualizar os logs em tempo real:

```bash
tail -f logs/spring.log
```

## Próximos Passos

Após validar que a API está funcionando, você pode:

1. Implementar testes unitários e de integração
2. Adicionar Swagger/OpenAPI para documentação interativa
3. Implementar as próximas funcionalidades do MelodyHub
4. Adicionar autenticação e autorização
5. Implementar cache com Redis
