# syntax=docker/dockerfile:1.4

# ==============================
# 🏗️ BUILD (low memory friendly)
# ==============================
FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /app

# Gradle config pra pouca memória
ENV GRADLE_OPTS="-Xmx256m -XX:MaxMetaspaceSize=128m -Dorg.gradle.daemon=false"

# 1️⃣ Arquivos de build
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle gradle.properties ./

RUN chmod +x gradlew

# 2️⃣ Baixa dependências (com cache)
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew build -x test \
    --no-daemon \
    --max-workers=1 \
    --no-parallel || true

# 3️⃣ Código fonte
COPY src src

# 4️⃣ Build final (controlando memória)
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew bootJar \
    --no-daemon \
    --max-workers=1 \
    --no-parallel

# ==============================
# 🚀 RUNTIME (leve)
# ==============================
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

# Ajustado pra pouca RAM
ENV JAVA_OPTS="-Xms64m -Xmx256m -Xss256k -XX:+UseSerialGC"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]