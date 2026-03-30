FROM eclipse-temurin:21-jdk-alpine
COPY auth-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]