FROM gradle:8.10.2-jdk21 AS builder
WORKDIR /app
COPY . .
RUN gradle --settings-file settings.server.gradle.kts -PenableAndroidTarget=false :server:installDist --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/server/build/install/server /app
EXPOSE 8080
CMD ["./bin/server"]
