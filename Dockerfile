FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
COPY src src
RUN mvn -B verify
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN mkdir /app/data && chown -R 10001:10001 /app
COPY --from=build /build/target/ledgerflow-0.1.0.jar app.jar
USER 10001:10001
ENV SERVER_ADDRESS=0.0.0.0
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
