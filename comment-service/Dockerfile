FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
COPY common-lib/pom.xml common-lib/
COPY comment-service/pom.xml comment-service/
RUN mvn -pl comment-service -am dependency:go-offline -B -q
COPY common-lib/src common-lib/src
COPY comment-service/src comment-service/src
RUN mvn -pl comment-service -am clean package -DskipTests -B -q

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /build/comment-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]