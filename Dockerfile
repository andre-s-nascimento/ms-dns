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
    ./gradlew dependencies --no-daemon --no-parallel || true

# 3️⃣ Copiar código fonte
COPY src src

# 4️⃣ Build final
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew bootJar --no-daemon --no-parallel

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

# Criar diretório para config (opcional)
RUN mkdir -p /app/config && chown appuser:appgroup /app/config

# Mudar para usuário não-root
USER appuser

EXPOSE 8080

# Healthcheck mais robusto
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health 2>/dev/null || \
      curl -f http://localhost:8080/gui 2>/dev/null || exit 1

# JVM otimizada para container
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=70 \
               -XX:InitialRAMPercentage=30 \
               -XX:+UseSerialGC \
               -XX:+HeapDumpOnOutOfMemoryError \
               -XX:HeapDumpPath=/tmp \
               -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]