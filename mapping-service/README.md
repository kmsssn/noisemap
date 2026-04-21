# Mapping Service

Строит тепловую карту шумового загрязнения. Агрегирует данные из классифицированных записей по ячейкам сетки. Использует MongoDB для хранения и Redis для кэширования.

## API

### GET /api/v1/map/tiles

Получить тайлы тепловой карты в bounding box. Публичный endpoint.

Параметры:
- `minLat`, `minLng`, `maxLat`, `maxLng` — границы области
- `noiseClass` (опционально) — фильтр по типу шума: traffic, construction, voices, siren, music, nature, industrial
- `timeFilter` (опционально) — фильтр по времени: morning, afternoon, evening, night

Пример: `/api/v1/map/tiles?minLat=43.20&minLng=76.85&maxLat=43.30&maxLng=76.99`

Ответ (200):
```json
{
  "tiles": [
    {
      "tileKey": "76945_43238",
      "latitude": 43.238,
      "longitude": 76.945,
      "avgNoiseLevelDba": 68.3,
      "measurementCount": 15,
      "dominantNoiseClass": "traffic",
      "noiseClassDistribution": {
        "traffic": 10,
        "voices": 3,
        "construction": 2
      }
    }
  ],
  "overallAvgDba": 68.3,
  "totalMeasurements": 15,
  "boundingBox": {
    "minLat": 43.20,
    "minLng": 76.85,
    "maxLat": 43.30,
    "maxLng": 76.99
  }
}
```

### GET /api/v1/map/nearby

Тайлы рядом с точкой.

Параметры: `lat`, `lng`, `radius` (в метрах, по умолчанию 500)

Ответ: массив тайлов.

## Как работает

Карта разбита на ячейки размером ~111 метров (0.001 градуса). При каждом `classification.completed` сервис:
1. Вычисляет ключ ячейки по координатам
2. Находит или создаёт тайл
3. Пересчитывает среднее инкрементально (не нужно пересчитывать все измерения)
4. Обновляет распределение классов шума
5. Инвалидирует кэш в Redis

Кэш тайлов живёт 5 минут.

## Модель данных

Коллекция `noise_tiles` в MongoDB:

| Поле | Тип | Описание |
|------|-----|----------|
| tileKey | String | "x_y" координаты ячейки |
| center | double[2] | [lng, lat], геоиндекс |
| avgNoiseLevelDba | Double | среднее Leq |
| minNoiseLevelDba | Double | |
| maxNoiseLevelDba | Double | |
| measurementCount | Integer | |
| noiseClassDistribution | Map | {"traffic": 10, "voices": 3} |
| dominantNoiseClass | String | |
| lastUpdated | Instant | |
