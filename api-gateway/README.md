# API Gateway

Единая точка входа для клиентов (мобильное приложение, веб). Проверяет JWT токен и маршрутизирует запросы к нужному сервису. Построен на Spring Cloud Gateway (реактивный).

## Маршрутизация

| Путь | Сервис |
|------|--------|
| /api/v1/auth/** | user-service:8081 |
| /api/v1/users/** | user-service:8081 |
| /api/v1/recordings/** | recording-service:8082 |
| /api/v1/map/** | mapping-service:8083 |
| /api/v1/stats/** | statistics-service:8084 |
| /api/v1/gamification/** | gamification-service:8085 |
| /api/v1/notifications/** | notification-service:8086 |
| /api/v1/moderation/** | moderation-service:8087 |

## JWT фильтр

Глобальный фильтр на все запросы:
1. Берёт `Authorization: Bearer <token>` из заголовка
2. Парсит JWT, извлекает userId и role
3. Прокидывает `X-User-Id` и `X-User-Role` в заголовках к сервису
4. Если токен невалидный → 401

Публичные пути (без токена): `/api/v1/auth/**`, `/api/v1/map/tiles`, `/api/v1/stats/city`, `/swagger-ui/**`, `/actuator/**`.

## Swagger агрегация

На http://localhost:8080/swagger-ui.html доступен выпадающий список со Swagger всех сервисов.
