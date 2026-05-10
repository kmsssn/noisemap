# Запуск проекта

## Требования

- Docker Desktop
- Java 17+
- Maven 3.8+

## Запуск

```bash
# поднять базы
docker compose up -d postgres mongodb redis rabbitmq

# собрать
mvn clean package -DskipTests

# запустить всё
docker compose up -d
```

## Проверка

```bash
docker compose ps
```

## Swagger UI

- User Service: http://localhost:8081/swagger-ui.html
- Recording Service: http://localhost:8082/swagger-ui.html
- Mapping Service: http://localhost:8083/swagger-ui.html
- Statistics Service: http://localhost:8084/swagger-ui.html
- Gamification Service: http://localhost:8085/swagger-ui.html
- Notification Service: http://localhost:8086/swagger-ui.html
- Moderation Service: http://localhost:8087/swagger-ui.html
- Comment Service: http://localhost:8088/swagger-ui.html
- RabbitMQ: http://localhost:15672 (guest/guest)

## Остановка

```bash
docker compose down
```

## Пересборка

```bash
mvn clean package -DskipTests
docker compose build --no-cache
docker compose up -d
```

## Если не хватает RAM

```bash
docker compose up -d postgres mongodb redis rabbitmq user-service recording-service gamification-service
```
