#FROM maven:3.9-eclipse-temurin-17 AS build
#WORKDIR /build
#COPY pom.xml .
#COPY common-lib/pom.xml common-lib/
#COPY notification-service/pom.xml notification-service/
#RUN mvn -pl notification-service -am dependency:go-offline -B -q
#COPY common-lib/src common-lib/src
#COPY notification-service/src notification-service/src
#RUN mvn -pl notification-service -am clean package -DskipTests -B -q
#
#FROM eclipse-temurin:17-jre-alpine
#WORKDIR /app
#COPY --from=build /build/notification-service/target/*.jar app.jar
#ENTRYPOINT ["java", "-jar", "app.jar"]