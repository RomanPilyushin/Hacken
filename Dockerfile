# Use Maven for building and packaging the app
FROM maven:3.8.6-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline  # Download dependencies to speed up builds
COPY src ./src
RUN mvn clean install -DskipTests  # Skip tests for faster build

# Build final image
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /build/target/Hacken-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
