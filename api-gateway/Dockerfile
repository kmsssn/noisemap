FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
COPY common-lib/pom.xml common-lib/
COPY api-gateway/pom.xml api-gateway/
RUN mvn -pl api-gateway -am dependency:go-offline -B -q
COPY common-lib/src common-lib/src
COPY api-gateway/src api-gateway/src
RUN mvn -pl api-gateway -am clean package -DskipTests -B -q

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /build/api-gateway/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]