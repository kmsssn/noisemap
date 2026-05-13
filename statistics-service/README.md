# Statistics Service

Аналитика шумового загрязнения — общая статистика по городу и персональная для каждого пользователя. Слушает `classification.completed` события из RabbitMQ, агрегирует, кэширует в Redis.

## API

### GET /api/v1/stats/city

Общая статистика по городу. **Публичный endpoint**.

Ответ (200):
```json
{
  "avgNoiseLevelDba": 65.4,
  "maxNoiseLevelDba": 92.1,
  "minNoiseLevelDba": 28.3,
  "totalMeasurements": 1250,
  "totalContributors": 47,
  "measurementsByNoiseClass": {
    "transport": 580,
    "human": 210,
    "building_noise": 190,
    "alert": 40,
    "animals": 85,
    "others": 25
  },
  "hourlyAverages": [
    {"hour": 0, "avgDba": 42.1, "measurementCount": 30},
    {"hour": 8, "avgDba": 71.5, "measurementCount": 95},
    {"hour": 18, "avgDba": 74.2, "measurementCount": 110}
  ]
}
```

### GET /api/v1/stats/me

Персональная статистика. Требует `X-User-Id`.

Ответ (200):
```json
{
  "totalRecordings": 23,
  "avgExposureDba": 67.8,
  "maxExposureDba": 85.2,
  "recordingsByNoiseClass": {
    "transport": 12,
    "human": 6,
    "building_noise": 5
  },
  "recommendation": "Повышенный уровень шума. Длительное воздействие может влиять на здоровье."
}
```

Рекомендации на основе норм ВОЗ:
- < 55 дБА — в пределах нормы
- 55-70 дБА — умеренный уровень, рекомендуются перерывы
- 70-85 дБА — повышенный, может влиять на здоровье
- > 85 дБА — опасный, нужна защита слуха

## Классы шума

Используются классы от ML-сервиса напрямую (pass-through):
`transport, human, alert, building_noise, animals, others`

## Как работает

Слушает `statistics.update.queue` → `ClassificationCompletedEvent`. Каждое событие:
1. Сохраняет одно измерение в `noise_measurements`
2. Инвалидирует кэш городской статистики в Redis

Городская статистика считается on-demand при первом запросе и кэшируется на 10 минут.

## Модель данных

Коллекция `noise_measurements` в MongoDB:

| Поле | Тип | Описание |
|------|-----|----------|
| recordingId | String | связь с recording |
| userId | UUID | |
| latitude, longitude | Double | |
| noiseLevelDba | Double | с учётом калибровки |
| noiseClass | String | transport, human, ... |
| confidenceScore | Double | 0..1 |
| recordedAt | Instant | |
| hourOfDay | Integer | 0-23 |
| dayOfWeek | Integer | 1-7 |
| month | Integer | 1-12 |

Индексы: `userId`, `recordedAt`, `noiseClass`.

## Локальный запуск

```bash
mvn spring-boot:run -pl statistics-service -Dspring-boot.run.profiles=local
open http://localhost:8084/swagger-ui.html
```