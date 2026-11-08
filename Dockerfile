# syntax=docker/dockerfile:1
# Runtime image: build the fat JAR on the host first (Maven deps are cached there),
# then package only the JAR. Multi-stage Maven-inside-Docker is too slow/unreliable
# on constrained networks to Maven Central.
#
#   mvn -DskipTests -Dmaven.test.skip=true package
#   docker compose build app

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S taino && adduser -S taino -G taino \
    && chown -R taino:taino /app

COPY --chown=taino:taino target/taino-backend.jar /app/app.jar

USER taino
EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
