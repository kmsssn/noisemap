#FROM maven:3.9-eclipse-temurin-17 AS build
#WORKDIR /build
#COPY pom.xml .
#COPY common-lib/pom.xml common-lib/
#COPY gamification-service/pom.xml gamification-service/
#RUN mvn -pl gamification-service -am dependency:go-offline -B -q
#COPY common-lib/src common-lib/src
#COPY gamification-service/src gamification-service/src
#RUN mvn -pl gamification-service -am clean package -DskipTests -B -q
#
#FROM eclipse-temurin:17-jre-alpine
#WORKDIR /app
#COPY --from=build /build/gamification-service/target/*.jar app.jar
#ENTRYPOINT ["java", "-jar", "app.jar"]