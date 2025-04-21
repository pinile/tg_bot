# Stage 1: Build stage
FROM maven:3.8.4-openjdk-17-slim AS build

WORKDIR /app

# Копируем все файлы проекта в контейнер
COPY . .

# Собираем проект с fat JAR (включает все зависимости)
RUN mvn clean compile assembly:single -DskipTests

# Stage 2: Runtime stage
FROM openjdk:17-jdk-slim

WORKDIR /app

# Копируем скомпилированный fat JAR из build stage
COPY --from=build /app/target/CodeCompostInspectorBot-1.0-SNAPSHOT-jar-with-dependencies.jar /app/CodeCompostInspectorBot.jar

# Команда для запуска приложения
CMD ["java", "-Dcom.sun.management.jmxremote", "-Dcom.sun.management.jmxremote.port=12345", "-Dcom.sun.management.jmxremote.rmi.port=12345", "-Dcom.sun.management.jmxremote.ssl=false", "-Dcom.sun.management.jmxremote.authenticate=false", "-Djava.rmi.server.hostname=localhost", "-jar", "CodeCompostInspectorBot.jar"]