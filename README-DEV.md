# MelodyHub - Desenvolvimento

## Autenticação com Senha

Agora os usuários são criados com senha! A senha é hasheada usando BCrypt antes de ser salva no banco.

### Exemplo de criação de usuário:

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "João Silva",
    "email": "joao@example.com",
    "password": "senha123"
  }'
```

**Requisitos de senha:**
- Mínimo de 6 caracteres
- A senha é armazenada hasheada (não pode ser recuperada em texto plano)

---

## Hot Reload com Docker

### Modo de Desenvolvimento (com hot reload):

```bash
# Iniciar ambiente de desenvolvimento
docker-compose -f docker-compose.dev.yml up --build

# Parar
docker-compose -f docker-compose.dev.yml down
```

**Vantagens do modo dev:**
- ✅ Hot reload automático quando você edita arquivos `.kt`
- ✅ Spring DevTools ativado
- ✅ Código fonte montado como volume
- ✅ Não precisa rebuild da imagem a cada mudança
- ✅ Cache do Gradle mantido entre restarts

### Modo de Produção (build otimizado):

```bash
# Iniciar ambiente de produção
docker-compose up --build

# Parar
docker-compose down
```

**Diferenças:**
- Build em 2 estágios (menor imagem final)
- Sem hot reload
- Ideal para deploy

---

## Dica

Depois de iniciar o `docker-compose.dev.yml`, qualquer mudança em arquivos `.kt` dentro de `src/` vai fazer o Gradle recompilar automaticamente e o Spring DevTools vai reiniciar a aplicação!
