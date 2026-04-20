FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY heatmap-api/pom.xml heatmap-api/
COPY data-generator/pom.xml data-generator/

COPY config config/

RUN mvn dependency:go-offline -B -pl data-generator -am

COPY data-generator/src data-generator/src
RUN mvn package -pl data-generator -am -DskipTests -B

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /app/data-generator/target/*.jar app.jar

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]