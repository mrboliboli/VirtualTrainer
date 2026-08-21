FROM maven:3.9.9-eclipse-temurin-21-alpine AS construction
WORKDIR /source
COPY pom.xml ./
RUN mvn -B -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -B package -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S pace && adduser -S -G pace pace
WORKDIR /application
COPY --from=construction /source/target/pace-backend-*.jar application.jar
USER pace
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/application/application.jar"]
