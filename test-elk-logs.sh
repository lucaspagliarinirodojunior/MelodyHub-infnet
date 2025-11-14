#!/bin/bash

echo "=========================================="
echo "MelodyHub - Gerador de Logs para ELK"
echo "=========================================="
echo ""

API_URL="http://localhost:8080"

# Cores
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}Este script irá gerar logs de teste para validar a Stack ELK${NC}"
echo ""

# Verificar se a API está rodando
echo "1. Verificando se a API está rodando..."
if curl -s -o /dev/null -w "%{http_code}" ${API_URL}/actuator/health | grep -q "200"; then
    echo -e "${GREEN}✓${NC} API está respondendo"
else
    echo -e "${RED}✗${NC} API não está acessível. Execute: docker-compose up -d"
    exit 1
fi
echo ""

# Criar usuário
echo "2. Criando usuário de teste..."
USER_RESPONSE=$(curl -s -X POST ${API_URL}/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test User ELK",
    "email": "elk-test@melodyhub.com",
    "password": "senha123"
  }')

if echo "$USER_RESPONSE" | grep -q "id"; then
    USER_ID=$(echo $USER_RESPONSE | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
    echo -e "${GREEN}✓${NC} Usuário criado: $USER_ID"
    echo -e "   ${YELLOW}→ Logs gerados:${NC} AuthService, UserService, HTTP requests"
else
    echo -e "${YELLOW}⚠${NC} Usuário já existe ou erro na criação (pode ser esperado)"
fi
echo ""

# Login (gera logs de autenticação)
echo "3. Fazendo login (gerando logs de autenticação)..."
LOGIN_RESPONSE=$(curl -s -X POST ${API_URL}/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "elk-test@melodyhub.com",
    "password": "senha123"
  }')

if echo "$LOGIN_RESPONSE" | grep -q "token"; then
    TOKEN=$(echo $LOGIN_RESPONSE | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
    USER_ID=$(echo $LOGIN_RESPONSE | grep -o '"userId":"[^"]*"' | cut -d'"' -f4)
    echo -e "${GREEN}✓${NC} Login realizado com sucesso"
    echo -e "   ${YELLOW}→ Logs gerados:${NC} AuthService (login attempt, success), MDC enrichment"
else
    echo -e "${RED}✗${NC} Falha no login"
    exit 1
fi
echo ""

# Verificar usuário autenticado
echo "4. Verificando usuário autenticado..."
ME_RESPONSE=$(curl -s -X GET ${API_URL}/api/auth/me \
  -H "Authorization: Bearer $TOKEN")

if echo "$ME_RESPONSE" | grep -q "email"; then
    echo -e "${GREEN}✓${NC} Usuário autenticado verificado"
    echo -e "   ${YELLOW}→ Logs gerados:${NC} AuthService (/me endpoint), HTTP filter logs"
else
    echo -e "${RED}✗${NC} Erro ao verificar usuário"
fi
echo ""

# Criar cartão de crédito
echo "5. Criando cartão de crédito..."
CARD_RESPONSE=$(curl -s -X POST ${API_URL}/api/credit-cards \
  -H "Content-Type: application/json" \
  -d "{
    \"userId\": \"$USER_ID\",
    \"cardNumber\": \"4111111111111111\",
    \"cardHolderName\": \"Test User ELK\",
    \"expirationDate\": \"12/2025\",
    \"cvv\": \"123\",
    \"isActive\": true
  }")

if echo "$CARD_RESPONSE" | grep -q "id"; then
    CARD_ID=$(echo $CARD_RESPONSE | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
    echo -e "${GREEN}✓${NC} Cartão criado: $CARD_ID"
    echo -e "   ${YELLOW}→ Logs gerados:${NC} CreditCardService, HTTP requests"
else
    echo -e "${YELLOW}⚠${NC} Erro ao criar cartão ou já existe"
    # Tentar pegar ID de cartão existente
    CARDS=$(curl -s -X GET ${API_URL}/api/credit-cards)
    CARD_ID=$(echo $CARDS | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
fi
echo ""

# Criar transação válida (gera logs de anti-fraud + domain events)
echo "6. Criando transação VÁLIDA (gerando logs de anti-fraud)..."
TRANSACTION_RESPONSE=$(curl -s -X POST ${API_URL}/api/transactions \
  -H "Content-Type: application/json" \
  -d "{
    \"userId\": \"$USER_ID\",
    \"subscriptionType\": \"BASIC\",
    \"creditCardId\": \"$CARD_ID\"
  }")

if echo "$TRANSACTION_RESPONSE" | grep -q "id"; then
    TRANS_STATUS=$(echo $TRANSACTION_RESPONSE | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
    echo -e "${GREEN}✓${NC} Transação criada: Status=$TRANS_STATUS"
    echo -e "   ${YELLOW}→ Logs gerados:${NC}"
    echo -e "     • TransactionService (creating, approved/rejected)"
    echo -e "     • AntiFraudService (10 regras de validação)"
    echo -e "     • DomainEventLogger (TransactionApproved/FraudDetected)"
    echo -e "     • DomainEventLogger (TransactionValidated)"
else
    echo -e "${YELLOW}⚠${NC} Erro ao criar transação"
fi
echo ""

# Tentar criar transação duplicada (fraude)
echo "7. Tentando criar transação DUPLICADA (teste de fraude)..."
sleep 1
FRAUD_RESPONSE=$(curl -s -X POST ${API_URL}/api/transactions \
  -H "Content-Type: application/json" \
  -d "{
    \"userId\": \"$USER_ID\",
    \"subscriptionType\": \"BASIC\",
    \"creditCardId\": \"$CARD_ID\"
  }")

if echo "$FRAUD_RESPONSE" | grep -q "REJECTED"; then
    echo -e "${GREEN}✓${NC} Transação rejeitada como esperado (já possui plano ativo)"
    echo -e "   ${YELLOW}→ Logs gerados:${NC}"
    echo -e "     • AntiFraudService (rule violation logged)"
    echo -e "     • DomainEventLogger (FraudDetectedEvent)"
else
    echo -e "${YELLOW}⚠${NC} Comportamento inesperado na validação de fraude"
fi
echo ""

# Login com senha errada (gera logs de falha)
echo "8. Tentando login com senha INCORRETA..."
FAIL_LOGIN=$(curl -s -X POST ${API_URL}/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "elk-test@melodyhub.com",
    "password": "senhaERRADA"
  }')

if echo "$FAIL_LOGIN" | grep -q "inválidas\|Invalid"; then
    echo -e "${GREEN}✓${NC} Login falhou como esperado"
    echo -e "   ${YELLOW}→ Logs gerados:${NC} AuthService (login failed: invalid password)"
else
    echo -e "${YELLOW}⚠${NC} Comportamento inesperado no login"
fi
echo ""

# Verificar endpoints do Actuator
echo "9. Verificando endpoints de monitoramento..."
echo "   /actuator/health"
curl -s ${API_URL}/actuator/health | grep -q "UP" && echo -e "   ${GREEN}✓${NC} Health OK" || echo -e "   ${RED}✗${NC} Health falhou"

echo "   /actuator/metrics"
curl -s ${API_URL}/actuator/metrics | grep -q "names" && echo -e "   ${GREEN}✓${NC} Metrics OK" || echo -e "   ${RED}✗${NC} Metrics falhou"

echo "   /actuator/prometheus"
curl -s ${API_URL}/actuator/prometheus | grep -q "jvm_" && echo -e "   ${GREEN}✓${NC} Prometheus OK" || echo -e "   ${RED}✗${NC} Prometheus falhou"
echo ""

echo "=========================================="
echo "Logs gerados com sucesso!"
echo "=========================================="
echo ""
echo -e "${BLUE}Próximos passos:${NC}"
echo ""
echo "1. Aguarde ~30 segundos para Logstash processar os logs"
echo ""
echo "2. Acesse Kibana:"
echo -e "   ${YELLOW}http://localhost:5601${NC}"
echo ""
echo "3. Configure o Index Pattern (primeira vez):"
echo "   - Management → Data Views → Create data view"
echo "   - Name: melodyhub-logs"
echo "   - Index pattern: melodyhub-*"
echo "   - Timestamp: @timestamp"
echo ""
echo "4. Veja os logs em Discover:"
echo "   - Menu → Analytics → Discover"
echo "   - Selecione 'melodyhub-logs'"
echo "   - Ajuste time range para 'Last 15 minutes'"
echo ""
echo "5. Queries úteis para testar:"
echo -e "   ${YELLOW}logger_name: *AuthService*${NC}           # Logs de autenticação"
echo -e "   ${YELLOW}logger_name: *AntiFraudService*${NC}     # Logs de anti-fraude"
echo -e "   ${YELLOW}tags: domain_event${NC}                  # Eventos de domínio"
echo -e "   ${YELLOW}level: ERROR OR level: WARN${NC}         # Alertas"
echo -e "   ${YELLOW}userId: \"$USER_ID\"${NC}  # Logs deste usuário"
echo ""
echo "6. Verificar se Elasticsearch recebeu os logs:"
echo -e "   ${YELLOW}curl http://localhost:9200/melodyhub-*/_count${NC}"
echo ""
