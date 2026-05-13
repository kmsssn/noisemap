# Comment Service

Комментарии пользователей к точкам на карте. Позволяет делиться наблюдениями без обязательной записи аудио. Хранит в MongoDB.

## API

### POST /api/v1/comments

Создать комментарий. Требует `X-User-Id` и `X-Display-Name`.

Запрос:
```json
{
  "latitude": 43.238,
  "longitude": 76.945,
  "text": "Тихий парк, прекрасное место для прогулок",
  "noiseClass": "animals",
  "noiseLevelDba": 45.0
}
```

`noiseClass` и `noiseLevelDba` опциональны. Текст до 500 символов.

Классы шума (как в ML-сервисе):
`transport, human, alert, building_noise, animals, others`

Ответ (201):
```json
{
  "id": "662f1a2b3c4d5e6f7a8b9c0d",
  "userId": "...",
  "displayName": "Амина",
  "latitude": 43.238,
  "longitude": 76.945,
  "text": "Тихий парк, прекрасное место для прогулок",
  "noiseClass": "animals",
  "noiseLevelDba": 45.0,
  "createdAt": "2026-04-08T17:38:05Z"
}
```

### GET /api/v1/comments

Комментарии в заданном bounding box. **Публичный endpoint**.

Параметры: `minLat`, `minLng`, `maxLat`, `maxLng`, `limit` (по умолчанию 100)

### GET /api/v1/comments/nearby

Комментарии в радиусе от точки. **Публичный endpoint**.

Параметры: `lat`, `lng`, `radius` (метры, по умолчанию 500)

### GET /api/v1/comments/my

Мои комментарии. Требует `X-User-Id`.

### DELETE /api/v1/comments/{id}

Удалить свой комментарий (soft delete). Только владелец или ADMIN.

## Модель данных

Коллекция `comments` в MongoDB:

| Поле | Тип |
|------|-----|
| id | String |
| userId | UUID, индекс |
| displayName | String — snapshot имени |
| location | double[2] — [lng, lat], geo-индекс 2dsphere |
| text | String до 500 символов |
| noiseClass | String — опциональный тег |
| noiseLevelDba | Double — опционально |
| deleted | Boolean — soft delete |
| createdAt | Instant |

## Локальный запуск

```bash
mvn spring-boot:run -pl comment-service -Dspring-boot.run.profiles=local
open http://localhost:8088/swagger-ui.html
```
