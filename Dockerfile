ARG SERVICE_NAME

FROM maven:3.9-eclipse-temurin-17 AS build
ARG SERVICE_NAME
WORKDIR /build

COPY pom.xml .
COPY common-lib common-lib
COPY ${SERVICE_NAME} ${SERVICE_NAME}

COPY api-gateway/pom.xml api-gateway/pom.xml
COPY user-service/pom.xml user-service/pom.xml
COPY recording-service/pom.xml recording-service/pom.xml
COPY statistics-service/pom.xml statistics-service/pom.xml
COPY gamification-service/pom.xml gamification-service/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml
COPY moderation-service/pom.xml moderation-service/pom.xml
COPY comment-service/pom.xml comment-service/pom.xml

RUN mvn -pl ${SERVICE_NAME} -am clean package -DskipTests -B -q

FROM eclipse-temurin:17-jre-alpine
ARG SERVICE_NAME
WORKDIR /app

COPY --from=build /build/${SERVICE_NAME}/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]