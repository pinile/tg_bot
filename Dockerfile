FROM maven:3.8.4-openjdk-17-slim AS build

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

FROM openjdk:17-jdk-slim

WORKDIR /app

COPY --from=build /app/target/CodeCompostInspectorBot-1.0-SNAPSHOT.jar /app/CodeCompostInspectorBot.jar

CMD ["java", "-jar", "CodeCompostInspectorBot.jar"]