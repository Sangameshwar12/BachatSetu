# syntax=docker/dockerfile:1
#
# Multi-stage production image for the BachatSetu backend (Spring Boot 3, Java 21).
# Stage 1 compiles and packages the application with Maven, with dependency resolution
# cached in its own layer. Stage 2 runs the packaged jar on a minimal JRE as a non-root
# user, with a container-aware heap sizing and a liveness-backed HEALTHCHECK.

FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline

COPY src src
RUN mvn -B -ntp package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S bachatsetu \
    && adduser -S -G bachatsetu bachatsetu \
    && apk add --no-cache curl

COPY --from=build --chown=bachatsetu:bachatsetu /workspace/target/bachatsetu-backend-*.jar /app/application.jar

USER bachatsetu
EXPOSE 8080

# Overridable at `docker run`/Compose time. Defaults keep the JVM heap proportional to
# whatever memory limit the orchestrator assigns the container, rather than a fixed -Xmx
# that would either waste memory or OOM-kill the process under a tighter limit.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0 -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError"

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl --fail --silent "http://localhost:${SERVER_PORT:-8080}/actuator/health/liveness" || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/application.jar"]
