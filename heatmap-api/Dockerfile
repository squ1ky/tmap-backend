FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY config config/

COPY heatmap-api/pom.xml heatmap-api/
COPY data-generator/pom.xml data-generator/

RUN mvn dependency:go-offline -B -pl heatmap-api -am

COPY heatmap-api/src heatmap-api/src
COPY data-generator/src data-generator/src

RUN mvn package -pl heatmap-api -am -DskipTests -B

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /app/heatmap-api/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
