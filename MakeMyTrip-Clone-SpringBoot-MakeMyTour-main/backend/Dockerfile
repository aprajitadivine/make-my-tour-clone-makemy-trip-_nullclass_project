# ── Stage 1: Build ──────────────────────────────────────────────────────────
# Uses the official Maven image with Eclipse Temurin JDK 17 to compile and
# package the Spring Boot fat-JAR.
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy the POM first so Maven dependency downloads are cached in a layer.
COPY pom.xml ./
RUN mvn -B dependency:go-offline

# Copy the source tree and produce the fat-JAR (skipping tests, which run in CI).
COPY src ./src
RUN mvn -B package -DskipTests

# ── Stage 2: Run ─────────────────────────────────────────────────────────────
# Uses a minimal JRE image for the final runtime layer.
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy only the compiled fat-JAR from the build stage.
COPY --from=build /app/target/makemytour-backend-*.jar app.jar

# PORT is injected by Render (and most PaaS platforms).
# Spring Boot reads it via server.port=${PORT:8080} in application.properties.
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
