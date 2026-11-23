# syntax=docker/dockerfile:1.4
# ==============================================================================
# SupplyGraph Multi-Stage Production Dockerfile
# Base: Eclipse Temurin 21 (Java 21 LTS)
# Hardened: Non-root user, minimal JRE runtime, and container healthchecks
# ==============================================================================

# Stage 1: Build & Package
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /build

# Copy Maven Wrapper and POM descriptors for caching
COPY pom.xml .
COPY .mvn/ .mvn/
COPY mvnw .
COPY supplygraph-core-domain/pom.xml supplygraph-core-domain/
COPY supplygraph-industry-api/pom.xml supplygraph-industry-api/
COPY supplygraph-concurguard/pom.xml supplygraph-concurguard/
COPY supplygraph-simulation-engine/pom.xml supplygraph-simulation-engine/
COPY supplygraph-core-application/pom.xml supplygraph-core-application/
COPY supplygraph-industry-ev/pom.xml supplygraph-industry-ev/
COPY supplygraph-industry-pharma/pom.xml supplygraph-industry-pharma/
COPY supplygraph-industry-semiconductor/pom.xml supplygraph-industry-semiconductor/
COPY supplygraph-industry-automotive/pom.xml supplygraph-industry-automotive/
COPY supplygraph-adapter-persistence/pom.xml supplygraph-adapter-persistence/
COPY supplygraph-adapter-ai/pom.xml supplygraph-adapter-ai/
COPY supplygraph-adapter-integration/pom.xml supplygraph-adapter-integration/
COPY supplygraph-web/pom.xml supplygraph-web/

RUN chmod +x ./mvnw

# Copy full source tree and compile packaging
COPY . .
RUN ./mvnw clean package -DskipTests -Pproduction

# Stage 2: Hardened Runtime Container
FROM eclipse-temurin:21-jre-jammy AS runtime

# Install curl for container health checking
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Run as dedicated unprivileged system user
RUN groupadd -r supplygraph && useradd -r -g supplygraph -m -d /app supplygraph
WORKDIR /app

# Copy executable jar from builder stage
COPY --from=builder --chown=supplygraph:supplygraph /build/supplygraph-web/target/supplygraph-web-*.jar /app/supplygraph.jar

USER supplygraph:supplygraph

ENV SERVER_PORT=8080 \
    JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/supplygraph.jar"]

