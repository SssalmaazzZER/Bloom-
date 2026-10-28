# --- build ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

# --- run ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S bloome && adduser -S bloome -G bloome
COPY --from=build /app/target/bloome-backend-*.jar app.jar
USER bloome
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --retries=10 CMD wget -qO- http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
