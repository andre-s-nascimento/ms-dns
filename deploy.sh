#!/bin/bash

# ==============================
# 🔧 CONFIGURAÇÕES
# ==============================
APP_NAME="app-nextdns"
IMAGE_NAME="ms-dns"
PORT_INTERNA=8080
PORT_EXTERNA=81
DATA_PATH="/home/ubuntu/ms-dns/nextdns/data"

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# ==============================
# 📋 FUNÇÕES AUXILIARES
# ==============================
log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

check_docker() {
    if ! command -v docker &> /dev/null; then
        log_error "Docker não está instalado"
        exit 1
    fi
}

load_env() {
    if [ -f .env ]; then
        set -a
        source .env
        set +a
        log_info "Variáveis carregadas do .env"
    else
        log_error "Arquivo .env não encontrado!"
        log_info "Crie um .env com:"
        echo "NEXTDNS_API_KEY=sua_chave"
        echo "NEXTDNS_PROFILE_ID=seu_id"
        echo "APP_SECRET_TOKEN=seu_token"
        exit 1
    fi
}

validate_env() {
    local missing=0
    [ -z "$GOOGLE_ID" ] && log_error "GOOGLE_ID não definida" && missing=1
    [ -z "$GOOGLE_SECRET" ] && log_error "GOOGLE_SECRET não definida" && missing=1
    [ -z "$YAHOO_ID" ] && log_error "YAHOO_ID não definida" && missing=1
    [ -z "$YAHOO_SECRET" ] && log_error "YAHOO_SECRET não definida" && missing=1

    if [ $missing -eq 1 ]; then
        log_warn "Verifique se o seu .env contém as novas chaves OAuth2"
        exit 1
    fi
}

build_image() {
    log_info "Construindo imagem Docker: $IMAGE_NAME"
    
    if DOCKER_BUILDKIT=1 docker build -t "$IMAGE_NAME" . ; then
        log_info "Imagem construída com sucesso"
    else
        log_error "Falha no build da imagem"
        exit 1
    fi
}

stop_container() {
    if docker ps -q -f name="$APP_NAME" | grep -q .; then
        log_info "Parando container: $APP_NAME"
        docker stop "$APP_NAME" > /dev/null 2>&1
    fi
    
    if docker ps -aq -f name="$APP_NAME" | grep -q .; then
        log_info "Removendo container antigo: $APP_NAME"
        docker rm "$APP_NAME" > /dev/null 2>&1
    fi
}

run_container() {
    log_info "Iniciando novo container na porta $PORT_EXTERNA:$PORT_INTERNA"

    log_info "Verificando infraestrutura do Kafka..."
    
    # 1. Detectar a rede do Kafka
    KAFKA_NET=$(docker inspect kafka-kraft -f '{{range $k,$v := .NetworkSettings.Networks}}{{$k}}{{end}}' 2>/dev/null)
    
    if [ -z "$KAFKA_NET" ]; then
        log_error "Container 'kafka-kraft' não encontrado. Suba o Kafka primeiro!"
        exit 1
    fi

    log_info "Usando rede: $KAFKA_NET"
    
    # Define um caminho absoluto fixo para os dados
    DATA_PATH="/home/ubuntu/ms-dns/nextdns/data"
    mkdir -p "$DATA_PATH"
    # Permissão para o SQLite (o usuário 'appuser' do container costuma ser 1000)
    sudo chown -R 1000:1000 "$DATA_PATH"

    docker run -d \
        --name "$APP_NAME" \
        --restart unless-stopped \
        --network "$KAFKA_NET" \
        -p "$PORT_EXTERNA:$PORT_INTERNA" \
        -e GOOGLE_ID="$GOOGLE_ID" \
        -e GOOGLE_SECRET="$GOOGLE_SECRET" \
        -e YAHOO_ID="$YAHOO_ID" \
        -e YAHOO_SECRET="$YAHOO_SECRET" \
        -e NEXTDNS_API_KEY="$NEXTDNS_API_KEY" \
        -e NEXTDNS_PROFILE_ID="$NEXTDNS_PROFILE_ID" \
        -e SPRING_KAFKA_BOOTSTRAP_SERVERS="kafka-kraft:29092" \
        -e SPRING_DATASOURCE_URL="jdbc:sqlite:/app/data/nextdns_data.db" \
        -v "$DATA_PATH:/app/data" \
        --memory="512m" \
        --cpus="0.5" \
        -e JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75 -XX:+UseSerialGC -Xss256k" \
        "$IMAGE_NAME"

    [ $? -eq 0 ] && log_info "Container iniciado com sucesso" || exit 1
}

wait_healthy() {
    log_info "Aguardando container ficar saudável (isso pode levar 2 min)..."
    local attempt=0
    # Aumentamos para 60 tentativas (120 segundos)
    while [ $attempt -lt 60 ]; do 
        status=$(docker inspect --format='{{.State.Health.Status}}' "$APP_NAME" 2>/dev/null)
        
        if [ "$status" = "healthy" ]; then
            log_info "Online e Saudável!"; return 0
        fi
        
        # Se o status for 'unhealthy', algo deu errado no boot
        if [ "$status" = "unhealthy" ]; then
            log_error "Container falhou no healthcheck interno do Docker."
            show_logs
            exit 1
        fi
        
        echo -n "."
        sleep 2
        attempt=$((attempt + 1))
    done
    log_warn "Timeout no healthcheck - verifique se o Kafka está lento."
}

show_status() {
    echo -e "\n${GREEN}==========================================${NC}"
    log_info "DEPLOY FINALIZADO!"
    echo -e "${GREEN}==========================================${NC}\n"
    
    docker ps --filter "name=$APP_NAME" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
    
    echo -e "\n🔗 URL de acesso: ${GREEN}https://adambravo79.ddns.net/controle-parental/gui${NC}\n"
    
    log_info "Testando healthcheck interno..."
    # Testamos o Actuator na porta interna mapeada (81)
    if curl -s "http://localhost:81/controle-parental/actuator/health" | grep -q "UP"; then
        log_info "✅ Spring Boot Actuator: ONLINE"
    else
        log_warn "⚠️ Actuator não respondeu - verifique 'docker logs $APP_NAME'"
    fi
}

show_logs() {
    echo ""
    log_info "Últimos logs do container:"
    echo "----------------------------------------"
    docker logs "$APP_NAME" --tail 20
    echo "----------------------------------------"
}

backup_logs() {
    local backup_dir="$HOME/nextdns-logs"
    mkdir -p "$backup_dir"
    local timestamp=$(date +%Y%m%d_%H%M%S)
    
    docker logs "$APP_NAME" > "$backup_dir/container_$timestamp.log" 2>&1
    log_info "Logs salvos em: $backup_dir/container_$timestamp.log"
}

# ==============================
# 🚀 MAIN EXECUTION
# ==============================
main() {
    echo "=========================================="
    echo "🚀 NextDNS Deploy Automatizado"
    echo "=========================================="
    
    check_docker
    load_env
    validate_env
    
    case "${1:-deploy}" in
        deploy)
            build_image
            stop_container
            run_container
            wait_healthy
            show_status
            show_logs
            ;;
        restart)
            stop_container
            run_container
            wait_healthy
            show_status
            ;;
        stop)
            stop_container
            log_info "Container parado"
            ;;
        logs)
            show_logs
            docker logs -f "$APP_NAME"
            ;;
        status)
            show_status
            ;;
        backup)
            backup_logs
            ;;
        shell)
            docker exec -it "$APP_NAME" sh
            ;;
        *)
            echo "Uso: $0 {deploy|restart|stop|logs|status|backup|shell}"
            echo ""
            echo "Comandos:"
            echo "  deploy  - Build e deploy completo (padrão)"
            echo "  restart - Apenas reinicia o container"
            echo "  stop    - Para o container"
            echo "  logs    - Mostra logs em tempo real"
            echo "  status  - Mostra status do container"
            echo "  backup  - Salva logs para backup"
            echo "  shell   - Acessa shell do container"
            exit 1
            ;;
    esac
}

main "$@"