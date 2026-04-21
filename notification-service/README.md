# Notification Service

Уведомления пользователям — ачивки, алерты о высоком шуме, результаты модерации. Хранит в MongoDB.

## API

### GET /api/v1/notifications

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
    },
    {
      "id": "662f1a2b3c4d5e6f7a8b9c0e",
      "type": "NOISE_ALERT",
      "title": "Высокий уровень шума!",
      "message": "Зафиксировано 87.3 дБА. Тип шума: construction. Рекомендуется защита слуха.",
      "read": true,
      "createdAt": "2026-04-08T15:20:00Z"
    }
  ],
  "totalElements": 2,
  "totalPages": 1
}
```

### GET /api/v1/notifications/unread-count

Количество непрочитанных (для бейджа в UI).

Ответ (200):
```json
{
  "count": 3
}
```

### PUT /api/v1/notifications/{id}/read

Отметить одно уведомление прочитанным.

### PUT /api/v1/notifications/read-all

Отметить все прочитанными.

## Типы уведомлений

| Тип | Когда создаётся |
|-----|----------------|
| ACHIEVEMENT_UNLOCKED | Пользователь получил ачивку |
| NOISE_ALERT | Зафиксирован шум > 85 дБА |
| RECORDING_FLAGGED | Запись отправлена на модерацию |
| MODERATION_ALERT | Для модераторов |
| WEEKLY_DIGEST | Еженедельный отчёт |

## События RabbitMQ

Слушает три очереди:
- `notification.achievement.queue` — ачивка разблокирована
- `notification.noise.alert.queue` — classification.completed с дБА > 85
- `notification.moderator.queue` — запись помечена модерацией
