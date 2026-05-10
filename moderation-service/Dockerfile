#FROM maven:3.9-eclipse-temurin-17 AS build
#WORKDIR /build
#COPY pom.xml .
#COPY common-lib/pom.xml common-lib/
#COPY moderation-service/pom.xml moderation-service/
#RUN mvn -pl moderation-service -am dependency:go-offline -B -q
#COPY common-lib/src common-lib/src
#COPY moderation-service/src moderation-service/src
#RUN mvn -pl moderation-service -am clean package -DskipTests -B -q
#
#FROM eclipse-temurin:17-jre-alpine
#WORKDIR /app
#COPY --from=build /build/moderation-service/target/*.jar app.jar
#ENTRYPOINT ["java", "-jar", "app.jar"]