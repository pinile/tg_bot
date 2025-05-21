FROM maven:3.8.4-openjdk-17-slim AS build

WORKDIR /app

COPY . .

# fat JAR
RUN mvn clean package -DskipTests

FROM openjdk:17-jdk-slim

WORKDIR /app

# fat JAR из build stage
COPY --from=build /app/target/CodeCompostInspectorBot-1.0-SNAPSHOT.jar /app/CodeCompostInspectorBot.jar
COPY .env .env

# запуск приложения
CMD ["java", "-jar", "CodeCompostInspectorBot.jar"]