# Notification Service

In-app уведомления + real-time push через **WebSocket** (STOMP протокол). Ачивки, алерты о высоком шуме, результаты модерации. Хранит в MongoDB.

## API

### REST endpoints

#### GET /api/v1/notifications

Мои уведомления с пагинацией. Требует `X-User-Id`. Отсортированы по дате (новые первыми).

Ответ (200):
```json
{
  "content": [
    {
      "id": "662f1a2b3c4d5e6f7a8b9c0d",
      "type": "ACHIEVEMENT_UNLOCKED",
      "title": "Новая ачивка: Первый шаг",
      "message": "Вы получили +10 очков!",
      "read": false,
      "createdAt": "2026-04-08T17:38:05Z"
    }
  ],
  "totalElements": 2,
  "totalPages": 1
}
```

#### GET /api/v1/notifications/unread-count

Количество непрочитанных (для бейджа в UI).

Ответ (200): `{ "count": 3 }`

#### PUT /api/v1/notifications/{id}/read

Отметить одно уведомление прочитанным.

#### PUT /api/v1/notifications/read-all

Отметить все прочитанными. Bulk MongoDB update — один запрос.

---

### WebSocket endpoint

#### `/ws` (или `/ws/websocket` через SockJS)

Real-time доставка уведомлений в открытое мобильное приложение через STOMP протокол поверх WebSocket.

**Подключение:**

URL:
- Локально: `ws://localhost:8086/ws` или `ws://localhost:8080/ws` (через gateway)
- Production: `wss://noisemap.duckdns.org/ws`

Headers при CONNECT:
```
Authorization: Bearer eyJ...
```

JWT обязательно — без него сервер отклоняет подключение.

**Подписка:**

```
SUBSCRIBE
destination:/user/queue/notifications
```

Spring автоматически маршрутизирует сообщения этого destination'а только тому юзеру, чей JWT использован при подключении. Никто другой не увидит твои уведомления.

**Формат входящего сообщения:**

```json
{
  "id": "662f1a2b3c4d5e6f7a8b9c0d",
  "type": "ACHIEVEMENT_UNLOCKED",
  "title": "Новая ачивка: Профи",
  "message": "Вы получили +250 очков!",
  "read": false,
  "createdAt": "2026-04-08T17:38:05Z"
}
```

Тот же формат что в REST endpoint `GET /api/v1/notifications`.

### Гарантии доставки

- **In-app (MongoDB):** все уведомления сохраняются всегда → юзер увидит при следующем запросе REST endpoint
- **WebSocket:** доставка только если юзер сейчас online (приложение открыто, подключение активно)
- **Reconnect:** при восстановлении соединения клиент должен делать GET `/api/v1/notifications` чтобы догнать пропущенное

## Типы уведомлений

| Тип | Кому | Когда |
|-----|------|-------|
| ACHIEVEMENT_UNLOCKED | Пользователю | Получил новую ачивку |
| NOISE_ALERT | Пользователю | Зафиксирован шум > 85 дБА |
| RECORDING_FLAGGED | Пользователю | Его запись отправлена на модерацию |
| MODERATION_ALERT | Модераторам | Новая запись в очереди модерации |

## События RabbitMQ

| Очередь | Событие | Действие |
|---------|---------|---------|
| `notification.achievement.queue` | `AchievementUnlockedEvent` | Уведомление пользователю об ачивке |
| `notification.noise.alert.queue` | `ClassificationCompletedEvent` (dBA > 85) | Алерт о высоком шуме |
| `notification.moderator.queue` | `RecordingFlaggedEvent` | Уведомление юзеру + всем модераторам |

## Настройка модераторов

```env
MODERATOR_IDS=uuid-модератора-1,uuid-модератора-2
```

Если пустой — модераторы не уведомляются (только сам пользователь).

## Roadmap

- **FCM (Firebase Cloud Messaging)** — для доставки уведомлений когда приложение **закрыто**. Не реализовано в MVP (требует Apple Developer Program $99/год для iOS push). Архитектура готова к интеграции.
- **Push для веб-клиентов** — через Web Push API + Service Worker.

## Тестирование WebSocket

### Через wscat (CLI)

```bash
npm install -g wscat
wscat -c ws://localhost:8086/ws -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

После подключения отправить STOMP CONNECT:
```
CONNECT
accept-version:1.2
host:localhost
Authorization:Bearer YOUR_JWT_TOKEN

^@
```

Затем подписаться:
```
SUBSCRIBE
id:sub-0
destination:/user/queue/notifications

^@
```

### Через JS в браузере (если нужен)

Используй библиотеку [SockJS](https://github.com/sockjs/sockjs-client) + [STOMP.js](https://github.com/stomp-js/stompjs).

## Локальный запуск

```bash
mvn spring-boot:run -pl notification-service -Dspring-boot.run.profiles=local
open http://localhost:8086/swagger-ui.html
```

WebSocket endpoint доступен на `ws://localhost:8086/ws`.
