# Moderation Service

Контроль качества данных. Автоматически проверяет записи на аномалии, ведёт очередь для ручной модерации. Хранит в MongoDB.

## API

Все endpoints требуют заголовок `X-User-Role: MODERATOR` или `X-User-Role: ADMIN`.

### GET /api/v1/moderation/queue

Очередь записей на проверку с пагинацией.

Ответ (200):
```json
{
  "content": [
    {
      "id": "662f1a2b3c4d5e6f7a8b9c0d",
      "recordingId": "69dfe928e2c8de4e34b2b6bb",
      "userId": "99c03677-9069-41fe-9b25-2a75de3d3dca",
      "reason": "out_of_bounds",
      "details": "Coordinates [55.7510, 37.6170] outside Almaty area",
      "status": "PENDING",
      "flaggedAt": "2026-04-08T18:00:00Z"
    }
  ],
  "totalElements": 1,
  "totalPages": 1
}
```

### PUT /api/v1/moderation/queue/{id}/review

Принять решение по записи.

Запрос:
```json
{
  "decision": "reject",
  "comment": "Координаты не соответствуют Алматы"
}
```

Допустимые значения decision: `approve`, `reject`.

### GET /api/v1/moderation/stats

Статистика очереди.

Ответ (200):
```json
{
  "pending": 5,
  "approvedToday": null,
  "rejectedToday": null
}
```

## Автоматические проверки

При каждом `recording.created` проверяется:

1. **Координаты** — широта должна быть 40-44, долгота 76-78 (Алматы). Иначе → `out_of_bounds`
2. **Спам-паттерн** — если у пользователя больше 10 флагов → `spam_pattern`

При срабатывании создаётся запись в очереди модерации и публикуется событие `recording.flagged` → notification-service уведомляет модераторов.

## Статусы

- **PENDING** — ожидает проверки модератором
- **APPROVED** — одобрена
- **REJECTED** — отклонена

## Модель данных

Коллекция `moderation_queue` в MongoDB:

| Поле | Тип | Описание |
|------|-----|----------|
| recordingId | String | |
| userId | UUID | |
| reason | String | out_of_bounds, spam_pattern |
| details | String | подробности |
| status | String | PENDING, APPROVED, REJECTED |
| reviewedBy | UUID | модератор |
| reviewComment | String | |
| flaggedAt | Instant | |
| reviewedAt | Instant | |
