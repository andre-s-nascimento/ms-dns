# Estágio 1: Build
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

# Copia os arquivos de configuração do Gradle
COPY gradlew .
COPY gradle gradle
RUN chmod +x gradlew
COPY build.gradle .
COPY settings.gradle .

# Dá permissão e baixa as dependências (cache)
RUN ./gradlew dependencies --no-daemon --max-workers=1 "-Dorg.gradle.jvmargs=-Xmx512m"

# Copia o código fonte e gera o jar
COPY src src
RUN ./gradlew bootJar --no-daemon --max-workers=1 "-Dorg.gradle.jvmargs=-Xmx512m"

# Estágio 2: Runtime (Imagem final leve)
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copia apenas o JAR gerado no estágio anterior
COPY --from=build /app/build/libs/*.jar app.jar

# Expõe a porta do Spring
EXPOSE 8080

# Declaramos as variáveis sem valores fixos
# O Spring Boot lerá esses nomes do ambiente do sistema
ENV APP_SECRET_TOKEN=""
ENV NEXTDNS_API_KEY=""
ENV NEXTDNS_PROFILE_ID=""
ENV JAVA_OPTS="-Xms256m -Xmx512m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

