FROM gradle:9.6.0-jdk21 AS builder
WORKDIR /workspace
COPY settings.gradle build.gradle ./
COPY src ./src
RUN gradle clean build --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /workspace/build/classes/java/main ./classes
COPY --from=builder /workspace/build/dependencies ./lib
ENTRYPOINT ["java", "-cp", "classes:lib/*", "py.edu.ucom.notifications.NotificationApplication"]
