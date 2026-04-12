# syntax=docker/dockerfile:1.4

# ==============================
# 🏗️ BUILD STAGE (otimizado)
# ==============================
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

# Otimizações para build em ambiente com pouca memória
ENV GRADLE_OPTS="-Xmx256m -XX:MaxMetaspaceSize=128m -Dorg.gradle.daemon=false"
ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=70"

# 1️⃣ Copiar apenas arquivos de build primeiro (melhor cache)
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle gradle.properties ./

RUN chmod +x gradlew

# 2️⃣ Baixar dependências (cacheável)
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew dependencies --no-daemon --no-configuration-cache || true

# 3️⃣ Copiar código fonte
COPY src src

# 4️⃣ Build final do JAR
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew bootJar --no-daemon --no-configuration-cache

# ==============================
# 🚀 RUNTIME STAGE (leve e segura)
# ==============================
FROM eclipse-temurin:21-jre-alpine

# Instalar curl para healthcheck (alpine precisa)
RUN apk add --no-cache curl

# Criar usuário não-root para segurança
RUN addgroup -g 1000 -S appgroup && \
    adduser -u 1000 -S appuser -G appgroup

WORKDIR /app

# Copiar JAR do estágio de build
COPY --from=build --chown=appuser:appgroup /app/build/libs/*.jar app.jar

RUN mkdir -p /app/data && chown appuser:appgroup /app/data

# Criar diretório para config (opcional)
RUN mkdir -p /app/config && chown appuser:appgroup /app/config

# Mudar para usuário não-root
USER appuser

EXPOSE 8080

# Healthcheck mais robusto
HEALTHCHECK --interval=30s --timeout=10s --start-period=120s --retries=5 \
  CMD curl -f http://localhost:8080/controle-parental/actuator/health || exit 1

# O JAVA_OPTS será complementado pelo deploy.sh
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]