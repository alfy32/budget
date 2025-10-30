# 1. Start with a lean, secure base image with a JVM (Java Virtual Machine)
FROM eclipse-temurin:21-jre-jammy

# 2. Define the application artifact name
# This should match the name of the JAR file in your build/libs directory
ARG JAR_FILE=build/libs/*.jar

# 3. Copy the built JAR into the container as 'app.jar'
COPY ${JAR_FILE} app.jar

# 4. Define the command to run the application when the container starts
ENTRYPOINT ["java","-jar","/app.jar"]