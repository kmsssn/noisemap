# NoiseMap

Дипломный проект — система мониторинга шумового загрязнения города Алматы.

Пользователи записывают звуки окружающей среды через мобильное приложение. Система принимает аудиозаписи, прогоняет через ML модель для классификации типа и уровня шума, и на основе этих данных строит тепловую карту города. Для мотивации пользователей реализована геймификация — очки и ачивки за вклад в мониторинг.

## Архитектура
Бэкенд состоит из 8 микросервисов на Spring Boot 3.3. Каждый сервис работает в своём Docker-контейнере со своей базой данных.

Сервисы взаимодействуют асинхронно через RabbitMQ. Когда пользователь загружает запись, recording-service публикует событие `recording.created`. Его параллельно получают gamification-service (начислить очки), moderation-service (проверить на спам) и ML-сервис (классифицировать звук). После классификации событие `classification.completed` обновляет карту, статистику и проверяет ачивки.

Синхронные REST-вызовы используются для ответов клиенту — профиль, карта, статистика, лидерборд.

### Внешние сервисы

- **ML Classification Service** — https://github.com/nodirbekUmarov04/thesis-classification-microservice
  Принимает WAV файлы, возвращает класс шума и dBA.
  Классы: `transport, human, alert, building_noise, animals, others`.
  Интегрируется через `MlBridgeListener` в recording-service (HTTP вызов).

- **Map Tiles Service** — отдельный сервис Миши, frontend обращается напрямую к нему.
  В нашем gateway маршрут не объявлен.

## Сервисы

| # | Сервис | Порт | Хранилище | Назначение |
|---|--------|------|-----------|-----------|
| 1 | api-gateway | 8080 | Redis | Единый вход, JWT валидация, маршрутизация, WebSocket upgrade |
| 2 | user-service | 8081 | PostgreSQL | Регистрация, JWT, профили, справочник калибровок устройств, email reset |
| 3 | recording-service | 8082 | MongoDB | Приём аудио, метаданные, ML bridge, публикация в RabbitMQ |
| 4 | statistics-service | 8084 | MongoDB + Redis | Городская и персональная статистика |
| 5 | gamification-service | 8085 | PostgreSQL + Caffeine | Очки, ачивки, лидерборд |
| 6 | notification-service | 8086 | MongoDB | In-app уведомления + **WebSocket real-time push** |
| 7 | moderation-service | 8087 | MongoDB | Контроль качества данных |
| 8 | comment-service | 8088 | MongoDB | Комментарии к точкам на карте |

## Push-уведомления

**WebSocket (STOMP)** — real-time доставка в открытое приложение.
- Endpoint: `wss://noisemap.duckdns.org/ws`
- Авторизация: JWT в заголовке `Authorization: Bearer ...` при CONNECT
- Подписка: `/user/queue/notifications`

## Production deployment

- **Сервер:** Hetzner CPX42, Ubuntu 24.04
- **Домены:** `noisemap.duckdns.org`, `grafana-noisemap.duckdns.org`
- **CI/CD:** GitHub Actions on push to main → `git pull` + `docker compose up`
- **TLS:** Let's Encrypt автоматически

## События RabbitMQ

Topic exchange `noisemap.events`:

| Routing Key | Producer | Consumers |
|------------|----------|-----------|
| `recording.created` | recording-service | ml-bridge (внутри recording-service), moderation, gamification |
| `classification.completed` | recording-service (через ML bridge) | statistics, gamification, notification |
| `achievement.unlocked` | gamification-service | notification |
| `recording.flagged` | moderation-service | notification |

## Классы шума

Используются классы от ML-сервиса без преобразований:
- `transport` — машины, мотоциклы, общественный транспорт
- `human` — голоса, разговоры
- `alert` — сирены, тревоги
- `building_noise` — стройка, ремонт
- `animals` — собаки, кошки, птицы
- `others` — всё остальное

## Роли

- **GUEST** — просмотр карты и общей статистики
- **USER** — запись звуков, профиль, геймификация (при регистрации)
- **MODERATOR** — доступ к очереди модерации
- **ADMIN** — полный доступ


## Стек

- Java 17, Spring Boot 3.3, Maven multi-module
- Spring Cloud Gateway, Spring Security (JWT), Spring Data JPA, Spring Data MongoDB, Spring AMQP, **Spring WebSocket + STOMP**
- PostgreSQL 16, MongoDB 7, Redis 7, RabbitMQ 3
- SpringDoc OpenAPI 2.6.0 для Swagger UI
- Mailjet SMTP для email
- Docker, Docker Compose

## Документация сервисов

Swagger UI: `https://noisemap.duckdns.org/swagger-ui.html` (агрегированный)

