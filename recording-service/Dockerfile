#FROM maven:3.9-eclipse-temurin-17 AS build
#WORKDIR /build
#COPY pom.xml .
#COPY common-lib/pom.xml common-lib/
#COPY recording-service/pom.xml recording-service/
#RUN mvn -pl recording-service -am dependency:go-offline -B -q
#COPY common-lib/src common-lib/src
#COPY recording-service/src recording-service/src
#RUN mvn -pl recording-service -am clean package -DskipTests -B -q
#
#FROM eclipse-temurin:17-jre-alpine
#WORKDIR /app
#COPY --from=build /build/recording-service/target/*.jar app.jar
#ENTRYPOINT ["java", "-jar", "app.jar"]