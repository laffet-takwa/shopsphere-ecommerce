FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
ARG SERVICE
COPY backend/${SERVICE}/pom.xml pom.xml
RUN mvn -q -f pom.xml dependency:go-offline
COPY backend/${SERVICE}/src src
COPY infrastructure/docker/logback-spring.xml src/main/resources/logback-spring.xml
RUN mvn -q -f pom.xml package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
ARG SERVICE
COPY --from=build /workspace/target/${SERVICE}-1.0.0.jar app.jar
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]