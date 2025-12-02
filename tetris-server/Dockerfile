# Use a base image with Java 17 and Maven
FROM eclipse-temurin:21

# Set the working directory
WORKDIR /app

# Copy the JAR file from the build directory
# Assuming the JAR is built by Gradle and located in tetris-server/build/libs/
# The exact JAR name might vary, so using a wildcard for now.
COPY tetris-server/build/libs/*.jar tetris-server.jar

# Expose the port the Spring Boot application runs on
EXPOSE 8090

# Command to run the application
ENTRYPOINT ["java", "-jar", "tetris-server.jar"]
