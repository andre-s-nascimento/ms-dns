#!/bin/bash

# --- CONFIGURAÇÕES DO APP ---
APP_NAME="app-nextdns"
IMAGE_NAME="ms-dns"
PORT_INTERNA=8080
PORT_EXTERNA=81

echo "------------------------------------------"
echo "🛠️  Iniciando Deploy Automatizado: $APP_NAME"
echo "------------------------------------------"

# 1. Gerar o Jar (Se você estiver com o código na VM)
# ./gradlew bootJar
# 1. Carregar variáveis do arquivo .env local
if [ -f .env ]; then
    export $(echo $(cat .env | sed 's/#.*//g' | xargs) | envsubst)
    echo "🔐 Variáveis de ambiente carregadas do arquivo .env"
else
    echo "⚠️  ERRO: Arquivo .env não encontrado! Crie o arquivo com as chaves antes de continuar."
    exit 1
fi

# 2. Build da Imagem Docker
echo "📦 Passo 1: Construindo imagem Docker..."
DOCKER_BUILDKIT=1 docker build -t $IMAGE_NAME .
#docker build -t $IMAGE_NAME .

# VERIFICAÇÃO DE SUCESSO:
if [ $? -ne 0 ]; then
    echo "❌ ERRO CRÍTICO: O build do Docker falhou. O deploy foi interrompido para evitar erros."
    exit 1
fi

# 3. Limpeza de containers antigos
echo "🛑 Passo 2: Verificando containers anteriores..."
if [ "$(docker ps -aq -f name=app-nextdns)" ]; then
    echo "   -> Removendo versão antiga de 'app-nextdns'..."
    docker rm -f app-nextdns > /dev/null
    echo "   ✅ Container removido com sucesso."
else
    echo "   ✨ Nenhum container anterior encontrado. Seguindo..."
fi

# 4. Execução do novo container com injeção de segredos
echo "🏃 Passo 3: Subindo nova versão na porta $PORT_EXTERNA..."
docker run -d \
  --name $APP_NAME \
  --restart unless-stopped \
  -p $PORT_EXTERNA:$PORT_INTERNA \
  -e NEXTDNS_API_KEY=$NEXTDNS_API_KEY \
  -e NEXTDNS_PROFILE_ID=$NEXTDNS_PROFILE_ID \
  -e APP_SECRET_TOKEN=$APP_SECRET_TOKEN \
  -e JAVA_OPTS="-Xmx256m -Xms128m" \
  $IMAGE_NAME

echo "------------------------------------------"
echo "✅ Deploy Finalizado!"
echo "Acesse: https://adambravo79.ddns.net/nextdnsapp/gui?token=$APP_SECRET_TOKEN"
echo "------------------------------------------"

# Mostra os logs iniciais
docker logs -f $APP_NAME --tail 20