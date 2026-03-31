# ==========================================
# ÉTAPE 1 : COMPILATION (Le "Builder")
# ==========================================
# On utilise directement une image Java (plus besoin de l'image Maven)
FROM eclipse-temurin:21-jdk-alpine AS builder

# On définit le dossier de travail
WORKDIR /app

# On copie le dossier caché .mvn (qui contient la config du wrapper)
COPY .mvn/ .mvn

# On copie le script Linux du wrapper et le pom.xml
COPY mvnw .
COPY pom.xml .

# TRÈS IMPORTANT : On donne les droits d'exécution au script Linux
RUN chmod +x mvnw

# On télécharge les dépendances via le wrapper
RUN ./mvnw dependency:go-offline

# On copie le reste du code source
COPY src ./src

# On compile le projet via le wrapper
RUN ./mvnw clean package -DskipTests

# ==========================================
# ÉTAPE 2 : EXÉCUTION (Le "Run")
# ==========================================
# On repart d'une image vierge pour lancer l'app
FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

# On récupère le .jar compilé
COPY --from=builder /app/target/*.jar app.jar

# On lance l'application
ENTRYPOINT ["java", "-jar", "app.jar"]