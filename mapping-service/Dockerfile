FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
COPY common-lib/pom.xml common-lib/
COPY mapping-service/pom.xml mapping-service/
RUN mvn -pl mapping-service -am dependency:go-offline -B -q
COPY common-lib/src common-lib/src
COPY mapping-service/src mapping-service/src
RUN mvn -pl mapping-service -am clean package -DskipTests -B -q

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /build/mapping-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]