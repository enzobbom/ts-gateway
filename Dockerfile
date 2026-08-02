### Build stage

# creates a build image stage with Gradle and JDK
FROM gradle:jdk17 AS build

# sets the working directory inside the image stage
WORKDIR /app

# copies build files and Gradle wrapper to the image stage
COPY build.gradle settings.gradle gradlew ./
COPY gradle gradle

# downloads dependencies and caches them in a Docker image layer
RUN ./gradlew dependencies --no-daemon || true

# copies source code to the image stage
COPY src src

# runs a temporary build container, builds the executable .jar, and saves the generated files as a new image layer
RUN ./gradlew bootJar --no-daemon

### Runtime stage

# creates a smaller runtime image stage with only the JRE
FROM eclipse-temurin:17-jre-jammy

# sets the working directory inside the runtime image
WORKDIR /app

# copies the built .jar file from the build stage into the runtime image
COPY --from=build /app/build/libs/*.jar ts-gateway.jar

# documents the port the application container listens on
EXPOSE 8080

# defines the command executed when the application container starts
ENTRYPOINT ["java", "-jar", "/app/ts-gateway.jar"]
