FROM maven:3.8.4-openjdk-17-slim AS build

WORKDIR /app

COPY . .

# fat JAR (включает все зависимости)
RUN mvn clean compile assembly:single -DskipTests

# Stage 2: Runtime stage
FROM openjdk:17-jdk-slim

WORKDIR /app

# fat JAR из build stage
COPY --from=build /app/target/CodeCompostInspectorBot-1.0-SNAPSHOT-jar-with-dependencies.jar /app/CodeCompostInspectorBot.jar

# запуск приложения
CMD ["java", "-XX:-UseContainerSupport", "-Dcom.sun.management.jmxremote", "-Dcom.sun.management.jmxremote.port=12345", "-Dcom.sun.management.jmxremote.rmi.port=12345", "-Dcom.sun.management.jmxremote.ssl=false", "-Dcom.sun.management.jmxremote.authenticate=false", "-Djava.rmi.server.hostname=localhost", "-jar", "CodeCompostInspectorBot.jar"]