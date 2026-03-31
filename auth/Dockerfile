FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY auth-service/ .
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline
RUN ./mvnw clean package -DskipTests
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]