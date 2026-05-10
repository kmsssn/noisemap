#FROM maven:3.9-eclipse-temurin-17 AS build
#WORKDIR /build
#COPY pom.xml .
#COPY common-lib/pom.xml common-lib/
#COPY user-service/pom.xml user-service/
#RUN mvn -pl user-service -am dependency:go-offline -B -q
#COPY common-lib/src common-lib/src
#COPY user-service/src user-service/src
#RUN mvn -pl user-service -am clean package -DskipTests -B -q
#
#FROM eclipse-temurin:17-jre-alpine
#WORKDIR /app
#COPY --from=build /build/user-service/target/*.jar app.jar
#ENTRYPOINT ["java", "-jar", "app.jar"]