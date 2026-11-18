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
# Serial GC + capped heap for small/shared VPS; override JAVA_OPTS if needed.
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=55.0 -XX:+UseSerialGC -XX:MaxMetaspaceSize=128m -Xss256k -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
