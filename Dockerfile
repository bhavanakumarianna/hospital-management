# ── Build stage: compile the app with Maven ──
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q package -DskipTests

# ── Run stage: small Java runtime image ──
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# On Render, the database details arrive as separate DB_* variables;
# build the JDBC URL from them. Locally, DB_URL can be set directly.
CMD ["sh", "-c", "if [ -n \"$DB_HOST\" ]; then export DB_URL=\"jdbc:postgresql://$DB_HOST:${DB_PORT:-5432}/$DB_NAME\"; fi; exec java -XX:MaxRAMPercentage=75 -jar app.jar"]
