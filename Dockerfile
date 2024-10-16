# Use an official OpenJDK 17 image from the Docker Hub as the base image
FROM openjdk:17-jdk-slim

# Set the working directory inside the container
WORKDIR /app

# Argument for the JAR file location, change 'hacken-0.0.1-SNAPSHOT.jar' to the actual name of your JAR
ARG JAR_FILE=target/Hacken-0.0.1-SNAPSHOT.jar

# Copy the application's JAR file into the container
COPY ${JAR_FILE} app.jar

# Expose port 8080 to the outside world
EXPOSE 8080

# Command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
