# Recording Service

Принимает аудиозаписи от пользователей, сохраняет файл и метаданные, отправляет событие в RabbitMQ для дальнейшей обработки. Хранит данные в MongoDB.

## API

### POST /api/v1/recordings

Загрузить аудиозапись. Формат: multipart/form-data. Возвращает 202 Accepted — классификация выполняется асинхронно.

Параметры:
- `audio` — аудиофайл (до 10 МБ)
- `metadata` — JSON с координатами

Метаданные:
```json
{
  "latitude": 43.238,
  "longitude": 76.945,
  "deviceModel": "Samsung Galaxy S24",
  "recordedAt": "2026-04-08T12:00:00Z"
}
```

Ответ (202):
```json
{
  "id": "69dfe928e2c8de4e34b2b6bb",
  "latitude": 43.238,
  "longitude": 76.945,
  "status": "PENDING",
  "noiseLevelDba": null,
  "noiseClass": null,
  "confidenceScore": null,
  "recordedAt": "2026-04-08T12:00:00Z",
  "createdAt": "2026-04-08T12:00:01Z"
}
```

Пример curl:
```bash
curl -X POST http://localhost:8082/api/v1/recordings \
  -H "X-User-Id: 99c03677-9069-41fe-9b25-2a75de3d3dca" \
  -F "audio=@sound.wav;type=audio/wav" \
  -F 'metadata={"latitude":43.238,"longitude":76.945,"deviceModel":"Samsung Galaxy S24"};type=application/json'
```

### GET /api/v1/recordings/my

Мои записи с пагинацией. Требует `X-User-Id`.

Ответ (200):
```json
{
  "content": [
    {
      "id": "69dfe928e2c8de4e34b2b6bb",
      "latitude": 43.238,
      "longitude": 76.945,
      "status": "CLASSIFIED",
      "noiseLevelDba": 72.5,
      "noiseClass": "traffic",
      "confidenceScore": 0.89,
      "recordedAt": "2026-04-08T12:00:00Z",
      "createdAt": "2026-04-08T12:00:01Z"
    }
  ],
  "totalElements": 1,
  "totalPages": 1
}
```

### GET /api/v1/recordings/{id}

Одна запись по ID.

### GET /api/v1/recordings/my/count

Количество записей пользователя. Ответ: `5`

## Статусы записи

- **PENDING** — загружена, ждёт ML классификации
- **CLASSIFIED** — ML обработал, есть результат
- **FLAGGED** — помечена модерацией как подозрительная
- **REJECTED** — отклонена модератором

## События RabbitMQ

При загрузке записи публикуется `recording.created`:
```json
{
  "recordingId": "69dfe928e2c8de4e34b2b6bb",
  "userId": "99c03677-9069-41fe-9b25-2a75de3d3dca",
  "audioFileUrl": "/data/audio/99c03677.../file.wav",
  "latitude": 43.238,
  "longitude": 76.945,
  "deviceModel": "Samsung Galaxy S24",
  "calibrationOffset": -2.5,
  "recordedAt": "2026-04-08T12:00:00Z",
  "publishedAt": "2026-04-08T12:00:01Z"
}
```

Уходит в очереди: `ml.classification.queue`, `moderation.check.queue`, `gamification.recording.queue`.

## Модель данных

Коллекция `recordings` в MongoDB:

| Поле | Тип | Описание |
|------|-----|----------|
| _id | ObjectId | автоматический |
| userId | UUID | |
| audioFileUrl | String | путь к файлу |
| location | double[2] | [lng, lat], геоиндекс 2dsphere |
| deviceModel | String | |
| calibrationOffset | Double | |
| status | String | PENDING, CLASSIFIED, FLAGGED, REJECTED |
| noiseLevelDba | Double | заполняется после ML |
| noiseClass | String | заполняется после ML |
| confidenceScore | Double | заполняется после ML |
| recordedAt | Instant | |
| createdAt | Instant | |
