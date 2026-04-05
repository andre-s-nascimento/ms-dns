#!/bin/bash

# ==============================
# 🔧 CONFIGURAÇÕES
# ==============================
APP_NAME="app-nextdns"
IMAGE_NAME="ms-dns"
PORT_INTERNA=8080
PORT_EXTERNA=81
COMPOSE_PROJECT="nextdns"

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
    [ -z "$NEXTDNS_API_KEY" ] && log_error "NEXTDNS_API_KEY não definida" && missing=1
    [ -z "$NEXTDNS_PROFILE_ID" ] && log_error "NEXTDNS_PROFILE_ID não definido" && missing=1
    [ -z "$APP_SECRET_TOKEN" ] && log_error "APP_SECRET_TOKEN não definido" && missing=1
    
    if [ $missing -eq 1 ]; then
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
    
    # Criar rede se não existir
    docker network inspect app-network >/dev/null 2>&1 || \
        docker network create app-network
    
    docker run -d \
        --name "$APP_NAME" \
        --restart unless-stopped \
        --network app-network \
        -p "$PORT_EXTERNA:$PORT_INTERNA" \
        -e NEXTDNS_API_KEY="$NEXTDNS_API_KEY" \
        -e NEXTDNS_PROFILE_ID="$NEXTDNS_PROFILE_ID" \
        -e APP_SECRET_TOKEN="$APP_SECRET_TOKEN" \
        -e SERVER_SERVLET_CONTEXT_PATH="" \
        --memory="512m" \
        --memory-swap="1g" \
        --cpus="0.5" \
        --health-cmd="curl -f http://localhost:$PORT_INTERNA/gui || exit 1" \
        --health-interval=30s \
        --health-timeout=5s \
        --health-retries=3 \
        --health-start-period=60s \
        "$IMAGE_NAME"
    
    if [ $? -eq 0 ]; then
        log_info "Container iniciado com sucesso"
    else
        log_error "Falha ao iniciar container"
        exit 1
    fi
}

wait_healthy() {
    log_info "Aguardando container ficar saudável..."
    local max_attempts=30
    local attempt=0
    
    while [ $attempt -lt $max_attempts ]; do
        local status=$(docker inspect --format='{{.State.Health.Status}}' "$APP_NAME" 2>/dev/null)
        
        if [ "$status" = "healthy" ]; then
            log_info "Container está saudável!"
            return 0
        elif [ "$status" = "unhealthy" ]; then
            log_error "Container está unhealthy!"
            return 1
        fi
        
        sleep 2
        attempt=$((attempt + 1))
        echo -n "."
    done
    
    echo ""
    log_warn "Timeout aguardando healthcheck - continuando mesmo assim"
    return 0
}

show_status() {
    echo ""
    echo "=========================================="
    log_info "DEPLOY FINALIZADO!"
    echo "=========================================="
    echo ""
    
    # Mostrar status do container
    docker ps --filter "name=$APP_NAME" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
    
    echo ""
    echo "🔗 URL de acesso:"
    echo -e "${GREEN}https://adambravo79.ddns.net/nextdnsapp/gui?token=$APP_SECRET_TOKEN${NC}"
    echo ""
    
    # Teste rápido
    log_info "Testando conectividade..."
    if curl -s -o /dev/null -w "%{http_code}" "http://localhost:$PORT_EXTERNA/gui?token=$APP_SECRET_TOKEN" | grep -q "200\|302"; then
        log_info "✅ Teste local OK"
    else
        log_warn "⚠️ Teste local falhou - verifique os logs"
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