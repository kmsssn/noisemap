#FROM maven:3.9-eclipse-temurin-17 AS build
#WORKDIR /build
#
## Копируем все pom.xml для multi-module structure
#COPY pom.xml .
#COPY common-lib/pom.xml common-lib/
#COPY api-gateway/pom.xml api-gateway/
#COPY user-service/pom.xml user-service/
#COPY recording-service/pom.xml recording-service/
#COPY statistics-service/pom.xml statistics-service/
#COPY gamification-service/pom.xml gamification-service/
#COPY notification-service/pom.xml notification-service/
#COPY moderation-service/pom.xml moderation-service/
#COPY comment-service/pom.xml comment-service/
#
#RUN mvn -pl api-gateway -am dependency:go-offline -B -q
#
#COPY common-lib/src common-lib/src
#COPY api-gateway/src api-gateway/src
#
#RUN mvn -pl api-gateway -am clean package -DskipTests -B -q
#
#FROM eclipse-temurin:17-jre-alpine
#WORKDIR /app
#COPY --from=build /build/api-gateway/target/*.jar app.jar
#ENTRYPOINT ["java", "-jar", "app.jar"]