# ---------- Build stage ----------
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
# скачиваем зависимости отдельно, чтобы кэшировалось
RUN mvn -q -e -DskipTests dependency:go-offline

COPY src ./src
# Собираем JAR (без тестов)
RUN mvn -q -DskipTests package

# Находим собранный JAR
RUN ls -la target
# ---------- Runtime stage ----------
FROM eclipse-temurin:21-jre
WORKDIR /opt/app

# Папка для внешнего конфига + H2
RUN mkdir -p /config /data/h2

# Копируем jar из build-стадии
ARG JAR=target/*.jar
COPY --from=build /app/${JAR} app.jar

# Удобные ENV (можно переопределить в compose)
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0" \
    SPRING_PROFILES_ACTIVE=default \
    SPRING_CONFIG_IMPORT="optional:file:/config/bot-config.yml" \
    TZ=Europe/Riga

EXPOSE 8080

# health endpoint есть благодаря actuator
HEALTHCHECK --interval=30s --timeout=3s --retries=5 CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh","-lc","java $JAVA_OPTS -jar app.jar --spring.config.import=${SPRING_CONFIG_IMPORT}"]