# Use Java 17 runtime image (using jammy for better platform compatibility)
FROM eclipse-temurin:17-jre-jammy

# Set working directory
WORKDIR /app

# Copy the JAR built by Maven
COPY target/*.jar app.jar

# Expose the port your Spring Boot app uses
EXPOSE 5004

# Run the JAR
ENTRYPOINT ["java", "-jar", "app.jar"]
