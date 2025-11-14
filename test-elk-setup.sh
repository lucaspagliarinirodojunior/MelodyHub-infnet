#!/bin/bash

echo "=========================================="
echo "MelodyHub - ELK Stack Setup Validator"
echo "=========================================="
echo ""

# Cores para output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Função para verificar se um arquivo existe
check_file() {
    if [ -f "$1" ]; then
        echo -e "${GREEN}✓${NC} $2 existe"
        return 0
    else
        echo -e "${RED}✗${NC} $2 NÃO ENCONTRADO"
        return 1
    fi
}

# Função para verificar se um diretório existe
check_dir() {
    if [ -d "$1" ]; then
        echo -e "${GREEN}✓${NC} Diretório $2 existe"
        return 0
    else
        echo -e "${RED}✗${NC} Diretório $2 NÃO ENCONTRADO"
        return 1
    fi
}

echo "1. Verificando arquivos de configuração..."
echo "-------------------------------------------"
check_file "docker-compose.yml" "docker-compose.yml"
check_file "build.gradle.kts" "build.gradle.kts"
check_file "src/main/resources/application.yml" "application.yml"
check_file "src/main/resources/logback-spring.xml" "logback-spring.xml"
check_file "logstash/pipeline/melodyhub.conf" "Logstash pipeline"
check_file "logstash/config/logstash.yml" "Logstash config"
check_file "ELK.md" "Documentação ELK"
check_file "QUICK_START_ELK.md" "Quick Start Guide"
echo ""

echo "2. Verificando componentes de observabilidade..."
echo "------------------------------------------------"
check_file "src/main/kotlin/edu/infnet/melodyhub/infrastructure/observability/MdcFilter.kt" "MdcFilter"
check_file "src/main/kotlin/edu/infnet/melodyhub/infrastructure/observability/RequestLoggingFilter.kt" "RequestLoggingFilter"
check_file "src/main/kotlin/edu/infnet/melodyhub/infrastructure/observability/UserContextEnricher.kt" "UserContextEnricher"
check_file "src/main/kotlin/edu/infnet/melodyhub/infrastructure/observability/DomainEventLogger.kt" "DomainEventLogger"
echo ""

echo "3. Verificando diretórios..."
echo "----------------------------"
check_dir "logstash" "logstash"
check_dir "logstash/pipeline" "logstash/pipeline"
check_dir "logstash/config" "logstash/config"
check_dir "src/main/kotlin/edu/infnet/melodyhub/infrastructure/observability" "observability"
echo ""

echo "4. Verificando docker-compose.yml..."
echo "------------------------------------"
if grep -q "elasticsearch:" docker-compose.yml; then
    echo -e "${GREEN}✓${NC} Elasticsearch configurado"
else
    echo -e "${RED}✗${NC} Elasticsearch NÃO configurado"
fi

if grep -q "logstash:" docker-compose.yml; then
    echo -e "${GREEN}✓${NC} Logstash configurado"
else
    echo -e "${RED}✗${NC} Logstash NÃO configurado"
fi

if grep -q "kibana:" docker-compose.yml; then
    echo -e "${GREEN}✓${NC} Kibana configurado"
else
    echo -e "${RED}✗${NC} Kibana NÃO configurado"
fi

if grep -q "app_logs:" docker-compose.yml; then
    echo -e "${GREEN}✓${NC} Volume app_logs configurado"
else
    echo -e "${RED}✗${NC} Volume app_logs NÃO configurado"
fi
echo ""

echo "5. Verificando dependências no build.gradle.kts..."
echo "---------------------------------------------------"
if grep -q "logstash-logback-encoder" build.gradle.kts; then
    echo -e "${GREEN}✓${NC} Logstash encoder adicionado"
else
    echo -e "${RED}✗${NC} Logstash encoder NÃO encontrado"
fi

if grep -q "spring-boot-starter-actuator" build.gradle.kts; then
    echo -e "${GREEN}✓${NC} Spring Boot Actuator adicionado"
else
    echo -e "${RED}✗${NC} Spring Boot Actuator NÃO encontrado"
fi

if grep -q "micrometer-registry-prometheus" build.gradle.kts; then
    echo -e "${GREEN}✓${NC} Prometheus registry adicionado"
else
    echo -e "${RED}✗${NC} Prometheus registry NÃO encontrado"
fi
echo ""

echo "6. Verificando application.yml..."
echo "---------------------------------"
if grep -q "management:" src/main/resources/application.yml; then
    echo -e "${GREEN}✓${NC} Actuator endpoints configurados"
else
    echo -e "${RED}✗${NC} Actuator endpoints NÃO configurados"
fi

if grep -q "logging:" src/main/resources/application.yml; then
    echo -e "${GREEN}✓${NC} Logging configurado"
else
    echo -e "${RED}✗${NC} Logging NÃO configurado"
fi
echo ""

echo "7. Contando linhas de código adicionadas..."
echo "-------------------------------------------"
echo "Componentes de observabilidade:"
wc -l src/main/kotlin/edu/infnet/melodyhub/infrastructure/observability/*.kt | tail -1
echo ""

echo "=========================================="
echo "Validação concluída!"
echo "=========================================="
echo ""
echo -e "${YELLOW}Para iniciar a stack ELK:${NC}"
echo "  docker-compose up --build"
echo ""
echo -e "${YELLOW}Para acessar Kibana:${NC}"
echo "  http://localhost:5601"
echo ""
echo -e "${YELLOW}Para ver logs da aplicação:${NC}"
echo "  docker-compose logs -f app"
echo ""
echo -e "${YELLOW}Para documentação completa:${NC}"
echo "  cat ELK.md"
echo ""
