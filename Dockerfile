# Build
FROM gradle:jdk17 AS build
WORKDIR /app
COPY . .

# Run .jar
RUN gradle build --no-daemon
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app
COPY --from=build /app/build/libs/*.jar /app/gateway.jar
EXPOSE 8083
CMD ["java", "-jar", "/app/gateway.jar"]