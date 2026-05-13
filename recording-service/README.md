# Recording Service

Принимает аудиозаписи, сохраняет файл и метаданные, подтягивает калибровку устройства, публикует событие в RabbitMQ. Содержит **ML Bridge** — компонент, который вызывает внешний ML-сервис одногруппника для классификации. Хранит данные в MongoDB.

## API

### POST /api/v1/recordings

Загрузить аудиозапись. Формат: `multipart/form-data`.
Возвращает 202 Accepted — классификация выполняется асинхронно через RabbitMQ + ML Bridge.

| Параметр | Тип | Обязательный | Описание |
|----------|-----|--------------|----------|
| audio | file | ✅ | Аудиофайл wav/mp3/m4a, до 10 МБ |
| latitude | double | ✅ | Широта (-90..90) |
| longitude | double | ✅ | Долгота (-180..180) |
| deviceModel | string | ❌ | Модель устройства, например "iPhone 13 Pro" |
| recordedAt | string | ❌ | ISO-8601 UTC |

**Пример:**
```bash
curl -X POST "http://localhost:8082/api/v1/recordings" \
  -H "X-User-Id: 99c03677-9069-41fe-9b25-2a75de3d3dca" \
  -F "audio=@sound.wav;type=audio/wav" \
  -F "latitude=43.238" \
  -F "longitude=76.945" \
  -F "deviceModel=Samsung Galaxy S24"
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

### GET /api/v1/recordings/{id}

Одна запись по ID.

### GET /api/v1/recordings/my/count

Количество записей пользователя.

---

## Калибровка устройства

При загрузке каждой записи recording-service синхронно запрашивает `calibrationOffset` из user-service через WebClient:

```
POST /api/v1/recordings {deviceModel: "Samsung Galaxy S24"}
        ↓
recording-service → GET user-service:8081/api/v1/devices/calibration?model=Samsung Galaxy S24
        ↓
Если устройство в справочнике → offset = -1.5 dB
Если нет → user-service создаёт запись с offset = 0.0 (auto-cataloguing)
        ↓
Recording сохраняется с calibrationOffset
RabbitMQ событие включает calibrationOffset
```

Fail-safe: если user-service недоступен — offset=0.0.

---

## ML Bridge — интеграция с внешним ML-сервисом

Recording-service содержит компонент `MlBridgeListener` — соединение нашей event-driven архитектуры с HTTP-based ML-сервисом одногруппника.

### Архитектура (Anti-Corruption Layer pattern)

```
1. POST /api/v1/recordings (audio + metadata)
                ↓
2. recording-service сохраняет файл + публикует recording.created в RabbitMQ
                ↓
3. ml.classification.queue ← слушает MlBridgeListener
                ↓
4. MlBridgeListener вызывает HTTP API одногруппника:
     POST http://ml-service:8000/predict
     Body: multipart, file=@audio.wav
                ↓
5. Получает ответ:
     {"label": "transport", "confidence": 0.977, "noise_level_dba": 72.3, ...}
                ↓
6. Публикует classification.completed → statistics, gamification, notification
```

### Классы шума

Используются классы ML-сервиса напрямую (pass-through, без маппинга):

- `transport` — машины, мотоциклы, общественный транспорт
- `human` — голоса, разговоры, крики
- `alert` — сирены, тревоги, гудки
- `building_noise` — стройка, ремонт, инструменты
- `animals` — собаки, кошки, птицы
- `others` — всё остальное

Если ML вернёт неизвестный класс — будет залогирован warning, но событие всё равно опубликовано (downstream сервисы обработают как строку).

### dBA измерение

ML-сервис должен возвращать `noise_level_dba` в ответе (RMS-based). Если поле отсутствует — placeholder `55.0` (см. `app.ml.default-dba-placeholder`).

Итоговый dBA = `noise_level_dba` + `calibrationOffset` (поправка микрофона устройства).

### Конфигурация

```yaml
services:
  ml-service:
    url: ${ML_SERVICE_URL:http://ml-service:8000}
    timeout-seconds: 30
app:
  ml:
    default-dba-placeholder: 55.0
```

Переменная окружения для production:
```env
ML_SERVICE_URL=http://host.docker.internal:8000
# Или, если ML на отдельном домене:
# ML_SERVICE_URL=https://ml-service.duckdns.org
```

### Fail-safe

Если ML-сервис недоступен или вернул ошибку:
- `MlBridgeListener` логирует ошибку
- Сообщение остаётся в RabbitMQ
- Запись остаётся в статусе `PENDING`
- Frontend увидит запись, но без классификации

---

## Статусы записи

| Статус | Описание |
|--------|----------|
| PENDING | Загружена, ждёт ML классификации |
| CLASSIFIED | ML обработал, есть результат dBA + noiseClass |
| FLAGGED | Помечена модерацией |
| REJECTED | Отклонена модератором |

---

## События RabbitMQ

### Публикует: `recording.created`

В три очереди: `ml.classification.queue`, `moderation.check.queue`, `gamification.recording.queue`.

```json
{
  "recordingId": "69dfe928e2c8de4e34b2b6bb",
  "userId": "99c03677-9069-41fe-9b25-2a75de3d3dca",
  "audioFileUrl": "/data/audio/.../file.wav",
  "latitude": 43.238,
  "longitude": 76.945,
  "deviceModel": "Samsung Galaxy S24",
  "calibrationOffset": -1.5,
  "recordedAt": "2026-05-09T10:00:00Z",
  "publishedAt": "2026-05-09T10:00:01Z"
}
```

### Слушает: `ml.classification.queue` через `MlBridgeListener`

Вызывает HTTP API ML-сервиса, публикует `classification.completed`.

### Публикует: `classification.completed`

В три очереди: `statistics.update.queue`, `gamification.achievement.queue`, `notification.noise.alert.queue`.

```json
{
  "recordingId": "...",
  "userId": "...",
  "latitude": 43.238,
  "longitude": 76.945,
  "noiseLevelDba": 70.8,
  "noiseClass": "transport",
  "confidenceScore": 0.977,
  "recordedAt": "...",
  "classifiedAt": "..."
}
```

---

## Хранилище файлов

Аудиофайлы в `/data/audio/{userId}/{recordingId}.{ext}`. В production — Docker volume `audio_data`.

---

## Локальный запуск

```bash
ML_SERVICE_URL=http://localhost:8000 \
  mvn spring-boot:run -pl recording-service -Dspring-boot.run.profiles=local

open http://localhost:8082/swagger-ui.html
```

Требуется MongoDB и RabbitMQ.