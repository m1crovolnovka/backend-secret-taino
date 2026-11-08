# syntax=docker/dockerfile:1
# Multi-stage: works on a bare server (no prebuilt JAR required).
# -P-with-tests → no Testcontainers download
# BuildKit cache → fast rebuilds after the first dependency fetch

FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY docker/maven-settings.xml /root/.m2/settings.xml
COPY pom.xml .
COPY src ./src

RUN --mount=type=cache,target=/root/.m2/repository \
    mvn -B -T 1C -P-with-tests -DskipTests -Dmaven.test.skip=true package \
    && test -f /app/target/taino-backend.jar

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S taino && adduser -S taino -G taino \
    && chown -R taino:taino /app

COPY --from=build --chown=taino:taino /app/target/taino-backend.jar /app/app.jar

USER taino
EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
