# API Gateway

Единая точка входа для клиентов (мобильное приложение, веб).
Маршрутизирует HTTP-запросы к микросервисам, валидирует JWT, агрегирует Swagger UI.

## Назначение

- **Маршрутизация:** один URL для frontend (`https://noisemap.duckdns.org`), сервисы скрыты за gateway
- **JWT валидация:** проверка токена в `Authorization: Bearer ...` и добавление `X-User-Id`/`X-User-Role` в заголовки для downstream сервисов
- **Rate limiting:** через Redis
- **Агрегация Swagger:** документация всех сервисов в одном UI

## Маршруты

| Path Prefix | Сервис | Описание |
|-------------|--------|---------|
| `/api/v1/auth/**` | user-service | Регистрация, логин, reset password |
| `/api/v1/users/**` | user-service | Профили, FCM токены |
| `/api/v1/devices/**` | user-service | Справочник калибровок устройств |
| `/api/v1/recordings/**` | recording-service | Загрузка аудио |
| `/api/v1/stats/**` | statistics-service | Статистика |
| `/api/v1/gamification/**` | gamification-service | Очки, ачивки, лидерборд |
| `/api/v1/notifications/**` | notification-service | Уведомления |
| `/api/v1/moderation/**` | moderation-service | Модерация |
| `/api/v1/comments/**` | comment-service | Комментарии |

## Swagger UI

Агрегированный UI: `http://localhost:8080/swagger-ui.html`

Документация каждого сервиса проксируется через `/services/{name}/v3/api-docs`:
- `/services/user/v3/api-docs`
- `/services/recording/v3/api-docs`
- `/services/statistics/v3/api-docs`
- `/services/gamification/v3/api-docs`
- `/services/notification/v3/api-docs`
- `/services/moderation/v3/api-docs`
- `/services/comment/v3/api-docs`

## Авторизация

Public endpoints (без JWT):
- `/api/v1/auth/register`, `/api/v1/auth/login`, `/api/v1/auth/forgot-password`, `/api/v1/auth/reset-password`
- `/api/v1/stats/city` (общая статистика)
- `/actuator/health`
- Swagger UI

Все остальные endpoints требуют `Authorization: Bearer <JWT>` и пробрасывают:
- `X-User-Id` — UUID пользователя из claim `sub`
- `X-User-Role` — роль из claim `role` (USER, MODERATOR, ADMIN)
- `X-Display-Name` — имя пользователя

Глобальный фильтр на все запросы:
1. Берёт `Authorization: Bearer <token>` из заголовка
2. Парсит JWT, извлекает userId и role
3. Прокидывает `X-User-Id` и `X-User-Role` в заголовках к сервису
4. Если токен невалидный → 401

## Локальный запуск

```bash
mvn spring-boot:run -pl api-gateway -Dspring-boot.run.profiles=local
open http://localhost:8080/swagger-ui.html
```
