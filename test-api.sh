#!/bin/bash

echo "=================================================="
echo "Script de Teste da API de Usuários - MelodyHub"
echo "=================================================="
echo ""

# Cores para output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# URL base da API
BASE_URL="http://localhost:8080/api/users"

echo -e "${YELLOW}Aguardando a API iniciar...${NC}"
sleep 5

echo -e "\n${GREEN}1. Testando criação de usuário (POST)${NC}"
curl -X POST $BASE_URL \
  -H "Content-Type: application/json" \
  -d '{
    "name": "João Silva",
    "email": "joao@email.com"
  }' \
  -w "\n\nStatus: %{http_code}\n" \
  -s | jq '.'

sleep 1

echo -e "\n${GREEN}2. Criando segundo usuário${NC}"
curl -X POST $BASE_URL \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Maria Santos",
    "email": "maria@email.com"
  }' \
  -w "\n\nStatus: %{http_code}\n" \
  -s | jq '.'

sleep 1

echo -e "\n${GREEN}3. Tentando criar usuário com e-mail duplicado (deve falhar)${NC}"
curl -X POST $BASE_URL \
  -H "Content-Type: application/json" \
  -d '{
    "name": "João Duplicado",
    "email": "joao@email.com"
  }' \
  -w "\n\nStatus: %{http_code}\n" \
  -s | jq '.'

sleep 1

echo -e "\n${GREEN}4. Listando todos os usuários (GET)${NC}"
curl -X GET $BASE_URL \
  -H "Content-Type: application/json" \
  -w "\n\nStatus: %{http_code}\n" \
  -s | jq '.'

sleep 1

echo -e "\n${GREEN}5. Buscando usuário por e-mail${NC}"
curl -X GET $BASE_URL/email/joao@email.com \
  -H "Content-Type: application/json" \
  -w "\n\nStatus: %{http_code}\n" \
  -s | jq '.'

sleep 1

echo -e "\n${GREEN}6. Testando validações - nome vazio (deve falhar)${NC}"
curl -X POST $BASE_URL \
  -H "Content-Type: application/json" \
  -d '{
    "name": "",
    "email": "teste@email.com"
  }' \
  -w "\n\nStatus: %{http_code}\n" \
  -s | jq '.'

sleep 1

echo -e "\n${GREEN}7. Testando validações - e-mail inválido (deve falhar)${NC}"
curl -X POST $BASE_URL \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Teste",
    "email": "email-invalido"
  }' \
  -w "\n\nStatus: %{http_code}\n" \
  -s | jq '.'

echo -e "\n${YELLOW}=================================================="
echo "Testes concluídos!"
echo "==================================================${NC}\n"
