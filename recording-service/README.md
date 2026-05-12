# Recording Service

Принимает аудиозаписи, сохраняет файл и метаданные, подтягивает калибровку устройства, публикует событие в RabbitMQ. Хранит данные в MongoDB.

## API

### POST /api/v1/recordings

Загрузить аудиозапись. Формат: `multipart/form-data` с плоскими параметрами (не JSON metadata).
Возвращает 202 Accepted — классификация выполняется асинхронно через RabbitMQ.

| Параметр | Тип | Обязательный | Описание |
|----------|-----|--------------|----------|
| audio | file | ДА           | Аудиофайл mp3/wav/m4a, до 10 МБ |
| latitude | double | ДА           | Широта (-90..90), например 43.238 |
| longitude | double | ДА           | Долгота (-180..180), например 76.945 |
| deviceModel | string | НЕТ          | Модель устройства, например "iPhone 13 Pro" |
| recordedAt | string | НЕТ          | ISO-8601 UTC, например 2026-05-09T10:00:00Z |

**Пример curl:**
```bash
curl -X POST "http://localhost:8082/api/v1/recordings" \
  -H "X-User-Id: 99c03677-9069-41fe-9b25-2a75de3d3dca" \
  -F "audio=@sound.wav;type=audio/wav" \
  -F "latitude=43.238" \
  -F "longitude=76.945" \
  -F "deviceModel=Samsung Galaxy S24" \
  -F "recordedAt=2026-05-09T10:00:00Z"
```

**Через API Gateway (production):**
```bash
curl -X POST "https://noisemap.duckdns.org/api/v1/recordings" \
  -H "Authorization: Bearer eyJ..." \
  -H "X-User-Id: 99c03677-9069-41fe-9b25-2a75de3d3dca" \
  -F "audio=@sound.wav;type=audio/wav" \
  -F "latitude=43.238" \
  -F "longitude=76.945" \
  -F "deviceModel=iPhone 13 Pro"
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
  "recordedAt": "2026-05-09T10:00:00Z",
  "createdAt": "2026-05-09T10:00:01Z"
}
```

### GET /api/v1/recordings/my

Мои записи с пагинацией. Требует `X-User-Id`.

Параметры: `page=0`, `size=20`, `sort=createdAt,desc`

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
      "recordedAt": "2026-05-09T10:00:00Z",
      "createdAt": "2026-05-09T10:00:01Z"
    }
  ],
  "totalElements": 5,
  "totalPages": 1
}
```

### GET /api/v1/recordings/{id}

Одна запись по ID.

### GET /api/v1/recordings/my/count

Количество записей пользователя. Ответ: `5`

---

## Калибровка устройства

При загрузке каждой записи recording-service **автоматически** запрашивает calibrationOffset из user-service:

```
POST /api/v1/recordings {deviceModel: "Samsung Galaxy S24"}
        ↓
recording-service → GET user-service:8081/api/v1/devices/calibration?model=Samsung Galaxy S24
        ↓
Если устройство есть в справочнике → offset = -1.5 dB
Если нет → user-service создаёт запись с offset = 0.0 (auto-cataloguing)
        ↓
Recording сохраняется с calibrationOffset = -1.5
RabbitMQ событие включает calibrationOffset для ML-сервиса
```

Fail-safe: если user-service недоступен — offset=0.0, запись всё равно принимается.

---

## Статусы записи

| Статус | Описание |
|--------|----------|
| PENDING | Загружена, ждёт ML классификации |
| CLASSIFIED | ML обработал, есть результат dBA + noiseClass |
| FLAGGED | Помечена модерацией как подозрительная |
| REJECTED | Отклонена модератором |

---

## События RabbitMQ

### Публикует: `recording.created`

Уходит в три очереди: `ml.classification.queue`, `moderation.check.queue`, `gamification.recording.queue`.

```json
{
  "recordingId": "69dfe928e2c8de4e34b2b6bb",
  "userId": "99c03677-9069-41fe-9b25-2a75de3d3dca",
  "audioFileUrl": "/data/audio/99c03677.../file.wav",
  "latitude": 43.238,
  "longitude": 76.945,
  "deviceModel": "Samsung Galaxy S24",
  "calibrationOffset": -1.5,
  "recordedAt": "2026-05-09T10:00:00Z",
  "publishedAt": "2026-05-09T10:00:01Z"
}
```

### Слушает: `recording.classification.result.queue`

Получает результаты ML классификации и обновляет запись.

---

## Хранилище файлов

Аудиофайлы хранятся в `/data/audio/{userId}/{recordingId}.{ext}`.
В production — Docker volume `audio_data` смонтирован в `/data/audio`.

---

## Локальный запуск

```bash
mvn spring-boot:run -pl recording-service -Dspring-boot.run.profiles=local
open http://localhost:8082/swagger-ui.html
```