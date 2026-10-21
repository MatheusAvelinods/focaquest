# Build a partir da raiz do repositório: docker build -f apps/api/Dockerfile .
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src/apps/api
COPY apps/api/.mvn .mvn
COPY apps/api/mvnw apps/api/pom.xml ./
RUN ./mvnw -q -B dependency:go-offline
COPY apps/api/src src
RUN ./mvnw -q -B -DskipTests package

FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 focaquest
WORKDIR /app
COPY --from=build /src/apps/api/target/focaquest-api-*.jar app.jar
COPY content /app/content
ENV FOCAQUEST_CONTENT_PATH=/app/content/tjpb-2026
USER focaquest
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
