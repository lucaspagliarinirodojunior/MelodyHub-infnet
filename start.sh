#!/bin/sh

# Inicia o processo de build contínuo em background
# Isso monitora mudanças e recompila automaticamente
echo "🔄 Iniciando build contínuo em background..."
(gradle -t :bootJar --no-daemon) &

# Aguarda um pouco para o primeiro build completar
sleep 5

# Inicia a aplicação Spring Boot
# O DevTools vai detectar mudanças nos arquivos .class e fazer reload
echo "🚀 Iniciando aplicação Spring Boot com DevTools..."
gradle bootRun --no-daemon -PskipDownload=true
