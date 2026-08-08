# Stage 1: Build the Spring Boot application
FROM eclipse-temurin:17-jdk-alpine AS build

WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

RUN chmod +x mvnw
RUN ./mvnw -q -DskipTests dependency:go-offline

COPY src/ src/

RUN ./mvnw -q -DskipTests clean package


# Stage 2: Run the compiled application
FROM eclipse-temurin:17-jre-alpine

# Install wget for Docker's health check,
# then create the restricted application user.
RUN apk add --no-cache wget \
    && addgroup -S smartserve \
    && adduser -S smartserve -G smartserve

WORKDIR /app

COPY --from=build \
    --chown=smartserve:smartserve \
    /workspace/target/SmartServe-0.0.1-SNAPSHOT.jar \
    app.jar

USER smartserve

EXPOSE 8080

HEALTHCHECK \
    --interval=30s \
    --timeout=3s \
    --start-period=30s \
    --retries=3 \
    CMD wget -q --spider \
        http://localhost:8080/actuator/health/readiness \
        || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
