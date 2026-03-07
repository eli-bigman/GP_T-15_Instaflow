# Stage 1: Build
FROM maven:3.9.12-eclipse-temurin-25-noble AS build
WORKDIR /app

# Cache dependencies effectively
COPY pom.xml .
RUN mvn dependency:resolve-plugins dependency:resolve -B

COPY src ./src
RUN mvn package -DskipTests -B

# Stage 2: Run
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# Create a non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy the built artifact
COPY --from=build /app/target/*.jar app.jar

# Setup directories and permissions
RUN mkdir -p /app/uploads && chown -R appuser:appgroup /app

USER appuser
EXPOSE 8080

# Added Container Support flags for Java 25
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
