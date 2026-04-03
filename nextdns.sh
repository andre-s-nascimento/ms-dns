#!/bin/bash

APP_NAME="app-nextdns"
IMAGE_NAME="ms-dns"
PORT_INTERNA=8080
PORT_EXTERNA=81
TOKEN_ACESSO="G25K03g01@"

echo "------------------------------------------"
echo "🛠️  Iniciando Deploy Automatizado: $APP_NAME"
echo "------------------------------------------"

# 1. Gerar o Jar (Se você estiver com o código na VM)
# ./gradlew bootJar

# 2. Build da Imagem Docker
echo "📦 Passo 1: Construindo imagem Docker..."
docker build -t $IMAGE_NAME .

# 3. Limpeza de containers antigos
echo "🛑 Passo 2: Removendo container anterior (se existir)..."
docker rm -f $APP_NAME || true

# 4. Execução do novo container
echo "🏃 Passo 3: Subindo nova versão na porta $PORT_EXTERNA..."
docker run -d \
  --name $APP_NAME \
  --restart unless-stopped \
  -p $PORT_EXTERNA:$PORT_INTERNA \
  -e AUTH_SECRET_TOKEN=$TOKEN_ACESSO \
  -e JAVA_OPTS="-Xmx256m -Xms128m" \
  $IMAGE_NAME

echo "------------------------------------------"
echo "✅ Deploy Finalizado!"
echo "Acesse: https://adambravo79.ddns.net/nextdnsapp/gui?token=$TOKEN_ACESSO"
echo "------------------------------------------"

# Mostra os logs iniciais para conferir o startup
docker logs -f $APP_NAME --tail 20