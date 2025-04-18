# Stage 1: Build stage
FROM maven:3.8.4-openjdk-17-slim AS build

WORKDIR /app

# Копируем все файлы проекта в контейнер
COPY . .

# Собираем проект (если требуется, можно пропустить тесты)
RUN mvn clean install -DskipTests

# Stage 2: Runtime stage
FROM openjdk:17-jdk-slim

WORKDIR /app

# Копируем скомпилированный .jar файл из build stage
COPY --from=build /app/target/CodeCompostInspectorBot-1.0-SNAPSHOT.jar /app/CodeCompostInspectorBot.jar

# Команда для запуска вашего приложения
CMD ["java", "-jar", "CodeCompostInspectorBot.jar"]
