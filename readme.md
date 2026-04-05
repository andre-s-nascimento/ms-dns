# NextDNS Parental Control

Aplicação Spring Boot reativa para controle parental via API do NextDNS. Permite bloquear/liberar o YouTube e configurar acessos temporários com timer automático.

## Sumário

- [Visão Geral](#visão-geral)
- [Funcionalidades](#funcionalidades)
- [Tecnologias](#tecnologias)
- [Pré-requisitos](#pré-requisitos)
- [Deploy Rápido](#deploy-rápido)
- [Docker Commands](#docker-commands)
- [Configuração Apache2](#configuração-apache2)
- [Estrutura do Projeto](#estrutura-do-projeto)
- [Endpoints da API](#endpoints-da-api)
- [Monitoramento](#monitoramento)
- [Segurança](#segurança)
- [Troubleshooting](#troubleshooting)
- [Performance](#performance)
- [CI/CD](#cicd-opcional)
- [Licença](#licença)

---

## Visão Geral

Aplicação Spring Boot reativa para controle parental via API do NextDNS. Permite bloquear/liberar o YouTube e configurar acessos temporários com timer automático.

Desenvolvida para rodar em container Docker com suporte a proxy reverso (Apache2/Nginx) e SSL.

---

## Funcionalidades

- 🔒 **Bloqueio imediato** do YouTube
- 🔓 **Liberação geral** do acesso
- ⏱️ **Acesso temporário** com timer automático (1 minuto até 4 horas)
- 🎯 **Interface web** responsiva (Bootstrap)
- 🔐 **Autenticação via token** na URL
- 🐳 **Container Docker** otimizado
- 📊 **Logs detalhados** com IP do cliente
- 🔄 **Proxy reverso** compatível (Apache/Nginx)

---

## Tecnologias

| Tecnologia | Versão |
|------------|--------|
| Java | 21 |
| Spring Boot | 4.0 (WebFlux) |
| Thymeleaf | 3.1+ |
| Docker | 20.10+ |
| NextDNS API | v1 |

---

## Pré-requisitos

- [ ] Docker 20.10 ou superior
- [ ] Conta NextDNS com Profile ID
- [ ] Apache2/Nginx (opcional, para proxy reverso)
- [ ] Certificado SSL (para HTTPS)

---

## Deploy Rápido

### 1. Clone o repositório

```bash
git clone https://github.com/adambravo79/nextdns-parental.git
cd nextdns-parental
```

### 2. Configure as variáveis de ambiente

```bash
cp .env.model .env
nano .env
```

Preencha com seus dados:

```bash
NEXTDNS_API_KEY=sua_chave_api
NEXTDNS_PROFILE_ID=seu_profile_id
APP_SECRET_TOKEN=seu_token_secreto
```

### 3. Execute o deploy

```bash
chmod +x deploy.sh
./deploy.sh deploy
```

### 4. Acesse a aplicação

```text
https://seu-dominio.com/nextdnsapp/gui?token=SEU_TOKEN_SECRETO
```

## 🐳 Docker Commands

### Gerenciamento via script (recomendado)

O script `deploy.sh` fornece uma interface simples para gerenciar o container:

| Comando | Descrição |
|---------|-----------|
| `./deploy.sh deploy` | Build e deploy completo (padrão) |
| `./deploy.sh restart` | Reiniciar container |
| `./deploy.sh stop` | Parar container |
| `./deploy.sh logs` | Ver logs em tempo real |
| `./deploy.sh status` | Status do container |
| `./deploy.sh backup` | Backup dos logs |
| `./deploy.sh shell` | Acessar shell do container |

### Exemplos de uso do script

```bash
# Primeiro deploy
./deploy.sh deploy

# Verificar se está rodando
./deploy.sh status

# Acompanhar logs
./deploy.sh logs

# Fazer backup antes de atualizar
./deploy.sh backup

# Atualizar para nova versão
./deploy.sh restart

# Entrar no container para debug
./deploy.sh shell
```

## Docker puro (sem script)

### Build da imagem

```bash
docker build -t ms-dns .
```

### Rodar container

```bash
docker run -d \
  --name app-nextdns \
  --restart unless-stopped \
  -p 81:8080 \
  -e NEXTDNS_API_KEY="sua_chave_api" \
  -e NEXTDNS_PROFILE_ID="seu_profile_id" \
  -e APP_SECRET_TOKEN="seu_token_secreto" \
  --memory="512m" \
  --cpus="0.5" \
  ms-dns
```

### Verificar se o container está rodando

```bash
docker ps | grep app-nextdns
```

### Ver logs

```bash
# Últimas 50 linhas
docker logs app-nextdns --tail 50

# Logs em tempo real
docker logs -f app-nextdns

# Parar o container
docker stop app-nextdns

# Remover o container
docker rm app-nextdns

# Parar e remover (forçado)
docker rm -f app-nextdns

# Acessar shell dentro do container
docker exec -it app-nextdns sh

#Testar endpoint localmente
docker exec app-nextdns curl -s http://localhost:8080/gui?token=SEU_TOKEN
```

## Variáveis de ambiente disponíveis

| Variável | Obrigatória | Descrição | Exemplo |
|----------|-------------|-----------|---------|
| NEXTDNS_API_KEY |✅| Chave da API NextDNS	| 9e0a61afeeb2...|
| NEXTDNS_PROFILE_ID|✅|ID do perfil NextDNS | 123567 |
| APP_SECRET_TOKEN |✅|Token de autenticação | A12B34C56 |
|SERVER_SERVLET_CONTEXT_PATH|❌|Prefixo da aplicação | (deixar vazio) ``|
| SERVER_PORT |❌| Porta interna (padrão: 8080) |	8080|
| JAVA_OPTS |❌ | Opções da JVM	| -Xmx256m -Xms128m |

## Limites de recursos recomendados

```bash
--memory="512m"      # Memória máxima
--memory-swap="1g"   # Memória + swap
--cpus="0.5"         # Limite de CPU (meio core)
--cpuset-cpus="0"    # Fixar em CPU específica (opcional)
```

## Healthcheck

O container possui healthcheck configurado. Para verificar o status:

```bash
docker inspect --format='{{.State.Health.Status}}' app-nextdns
```

Possíveis retornos:

- starting - Inicializando (primeiros 60 segundos)

- healthy - Funcionando normalmente

- unhealthy - Falha nos testes

## Redes Docker

Para comunicação entre containers (ex: com Prometheus):

```bash
# Criar rede
docker network create app-network

# Rodar container na rede
docker run -d --network app-network --name app-nextdns ...

# Conectar container existente
docker network connect app-network app-nextdns
```

## Logs e debugging

### Ver logs com timestamps

```bash
docker logs app-nextdns --timestamps
```

### Ver apenas erros

```bash
docker logs app-nextdns 2>&1 | grep -i error
```

### Ver requisições à API

```bash
docker logs app-nextdns 2>&1 | grep -i "requisição"
```

### Salvar logs para análise

```bash
docker logs app-nextdns > logs_$(date +%Y%m%d).txt 2>&1
```

## Backup e restauração

### Backup dos logs

```bash
docker logs app-nextdns > backup/container_$(date +%Y%m%d_%H%M%S).log
```

### Backup da imagem Docker

```bash
docker save ms-dns -o ms-dns_backup.tar
gzip ms-dns_backup.tar
```

### Restaurar imagem

```bash
gunzip ms-dns_backup.tar.gz
docker load -i ms-dns_backup.tar
```

## Atualização sem downtime (opcional)

Para produção, usar rolling update:

```bash
# Build nova imagem
docker build -t ms-dns:latest .

# Rodar novo container em porta diferente
docker run -d --name app-nextdns-new -p 82:8080 ms-dns:latest

# Testar novo container
curl http://localhost:82/gui?token=SEU_TOKEN

# Trocar portas (se usando Apache)
# Ou simplesmente parar o antigo e iniciar o novo
docker stop app-nextdns
docker rename app-nextdns app-nextdns-old
docker rename app-nextdns-new app-nextdns

# Remover o antigo após confirmar
docker rm app-nextdns-old
```

## Comandos úteis adicionais

```bash
# Ver uso de recursos
docker stats app-nextdns

# Ver detalhes do container
docker inspect app-nextdns

# Ver processos dentro do container
docker exec app-nextdns ps aux

# Copiar arquivos do container
docker cp app-nextdns:/app/app.jar ./app_backup.jar

# Ver variáveis de ambiente
docker exec app-nextdns env | sort

# Reiniciar política do container
docker update --restart=always app-nextdns
```
## 🔧 Configuração Apache2 (Proxy Reverso)

### VirtualHost para HTTPS

```apache
<VirtualHost *:443>
    ServerName adambravo79.ddns.net
    
    # SSL (Let's Encrypt)
    SSLEngine on
    SSLCertificateFile /etc/letsencrypt/live/seu-dominio/fullchain.pem
    SSLCertificateKeyFile /etc/letsencrypt/live/seu-dominio/privkey.pem
    
    # Proxy para o container
    <Location /nextdnsapp>
        # Validação do token
        RewriteCond %{QUERY_STRING} !token=SEU_TOKEN_SECRETO(@|%40) [NC]
        RewriteRule ^ - [F,L]
        
        ProxyPass http://127.0.0.1:81/ nocanon
        ProxyPassReverse http://127.0.0.1:81/
        
        RequestHeader set X-Forwarded-Proto "https"
        RequestHeader set X-Forwarded-Prefix "/nextdnsapp"
        ProxyPreserveHost On
    </Location>
</VirtualHost>
```

### Habilitar módulos e reiniciar

```bash
sudo a2enmod proxy proxy_http headers rewrite ssl
sudo systemctl restart apache2
```

## 📁 Estrutura do Projeto

```text
nextdns-parental/
├── src/
│   ├── main/
│   │   ├── java/net/ddns/adambravo79/nextdns/
│   │   │   ├── NextDnsApplication.java
│   │   │   ├── client/
│   │   │   │   └── NextDnsClient.java
│   │   │   ├── config/
│   │   │   │   ├── SchedulerConfig.java
│   │   │   │   └── WebClientConfig.java
│   │   │   ├── controller/
│   │   │   │   ├── api/
│   │   │   │   │   └── ParentalController.java
│   │   │   │   └── web/
│   │   │   │       └── ParentalGuiController.java
│   │   │   ├── security/
│   │   │   │   └── TokenValidator.java
│   │   │   └── service/
│   │   │       └── NextDnsService.java
│   │   └── resources/
│   │       ├── application.yml
│   │       └── templates/
│   │           └── index.html
├── Dockerfile
├── deploy.sh
├── .env.example
├── build.gradle
├── settings.gradle
└── README.md
```

## 🔐 Endpoints da API

### Bloquear YouTube

```bash
POST /api/parental/youtube/bloquear
Header: X-Auth-Token: SEU_TOKEN
```

### Liberar YouTube

```bash
DELETE /api/parental/youtube/liberar
Header: X-Auth-Token: SEU_TOKEN
```

## 📊 Monitoramento

### Verificar saúde do container

```bash
docker inspect --format='{{.State.Health.Status}}' app-nextdns
```

### Logs com IP do cliente

A aplicação registra todas as requisições com o IP real do cliente:

```text
[189.45.123.45] - Requisição de BLOQUEIO às 2026-04-05T01:30:00
[189.45.123.45] - ACESSO TEMPORÁRIO iniciado por 30 min às 2026-04-05T01:35:00
```

## 🛡️ Segurança

- ✅ Token obrigatório em todas as requisições

- ✅ Usuário não-root no container

- ✅ Limites de recursos (memória/CPU)

- ✅ Headers de segurança (HSTS, X-Frame-Options, etc.)

- ✅ Validação de IP via X-Forwarded-For

- ✅ Rate limiting no Apache

- ✅ Bloqueio de métodos HTTP não permitidos

## 🐛 Troubleshooting

### Container não inicia

```bash
# Ver logs
docker logs app-nextdns --tail 50

# Verificar variáveis de ambiente
docker exec app-nextdns env | grep NEXTDNS
```

### Erro 404 - Rota não encontrada

``` bash
# Verificar se o context-path está vazio
docker exec app-nextdns curl -s http://localhost:8080/gui?token=SEU_TOKEN

# Testar rotas disponíveis
docker exec app-nextdns curl -s http://localhost:8080/actuator/mappings
```

### Proxy Apache retornando 403/404
```bash
# Verificar configuração do token no Apache
sudo grep -A 5 "Location /nextdnsapp" /etc/apache2/sites-available/000-default-le-ssl.conf

# Testar bypass do Apache
curl -s http://localhost:81/gui?token=SEU_TOKEN
```

### Container com alto uso de memória

```bash
# Ver estatísticas
docker stats app-nextdns

# Ajustar limites no docker run
--memory="256m" --memory-swap="512m" --cpus="0.5"
```

## Performance

| Métrica |	Valor |
|---|---|
|Tempo de build	| 2-3 minutos (com cache) |
|Tempo de inicialização	|15-20 segundos|
|Uso de memória	|128-256MB|
|Uso de CPU	0|.5 core (configurável)|
|Tempo de resposta	|<100ms (bloqueio/liberação)|

## CI/CD (Opcional)

Exemplo de pipeline GitHub Actions:

```yaml
name: Deploy to OCI

on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Build Docker image
        run: docker build -t ms-dns .
      
      - name: Deploy to server
        uses: appleboy/ssh-action@v0.1.5
        with:
          host: ${{ secrets.SERVER_IP }}
          username: ${{ secrets.SERVER_USER }}
          key: ${{ secrets.SSH_KEY }}
          script: |
            cd /home/ubuntu/nextdns-parental
            git pull
            ./deploy.sh deploy
```

## 📝 Licença

MIT License - veja o arquivo LICENSE para detalhes.

## 👨‍💻 Autor

Andre Nascimento

GitHub: @adambravo79

Projeto: nextdns-parental

## ⚠️ Notas importantes

- Token de autenticação: Mantenha o APP_SECRET_TOKEN seguro e nunca o compartilhe
- Rate limiting: Configure limites no Apache para evitar abuso
- Backup: Os logs são salvos em ~/nextdns-logs/ pelo script deploy.sh backup
- Atualizações: Use ./deploy.sh deploy para rebuildar com novas alterações

## 🎯 Roadmap

- Dashboard com estatísticas de uso
- Múltiplos serviços bloqueáveis (além do YouTube)
- Agendamento de horários (ex: bloquear 22h-06h)
- WebSocket para timer em tempo real
- Integração com Prometheus + Grafana
- Autenticação via OAuth2

### Desenvolvido com ☕ e Spring WebFlux
