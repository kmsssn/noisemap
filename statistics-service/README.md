# Statistics Service

Аналитика шумового загрязнения — общая статистика по городу и персональная для каждого пользователя. Хранит данные в MongoDB, кэширует в Redis.

## API

### GET /api/v1/stats/city

Общая статистика по городу. Публичный endpoint.

Ответ (200):
```json
{
  "avgNoiseLevelDba": 65.4,
  "maxNoiseLevelDba": 92.1,
  "minNoiseLevelDba": 28.3,
  "totalMeasurements": 1250,
  "totalContributors": 47,
  "measurementsByNoiseClass": {
    "traffic": 580,
    "construction": 210,
    "voices": 190,
    "music": 120,
    "nature": 85,
    "siren": 40,
    "industrial": 25
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
    "traffic": 12,
    "voices": 6,
    "construction": 5
  },
  "personalHourlyAverages": null,
  "recommendation": "Повышенный уровень шума. Длительное воздействие может влиять на здоровье."
}
```

Рекомендации на основе норм ВОЗ:
- < 55 дБА — в пределах нормы
- 55-70 дБА — умеренный уровень, рекомендуются перерывы
- 70-85 дБА — повышенный, может влиять на здоровье
- > 85 дБА — опасный, нужна защита слуха

## Модель данных

Коллекция `noise_measurements` в MongoDB:

| Поле | Тип | Описание |
|------|-----|----------|
| recordingId | String | |
| userId | UUID | |
| latitude, longitude | Double | |
| noiseLevelDba | Double | |
| noiseClass | String | |
| confidenceScore | Double | |
| recordedAt | Instant | |
| hourOfDay | Integer | 0-23, для агрегации |
| dayOfWeek | Integer | 1-7 |
| month | Integer | 1-12 |

Кэш городской статистики в Redis — TTL 10 минут.