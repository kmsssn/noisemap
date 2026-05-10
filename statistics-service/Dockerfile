FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
COPY common-lib/pom.xml common-lib/
COPY statistics-service/pom.xml statistics-service/
RUN mvn -pl statistics-service -am dependency:go-offline -B -q
COPY common-lib/src common-lib/src
COPY statistics-service/src statistics-service/src
RUN mvn -pl statistics-service -am clean package -DskipTests -B -q

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /build/statistics-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]