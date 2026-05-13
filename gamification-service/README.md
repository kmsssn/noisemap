# Gamification Service

Геймификация — очки, ачивки, лидерборд. Мотивирует пользователей делать больше записей. Хранит в PostgreSQL (через Flyway миграции), кэширует ачивки в Caffeine.

## API

### GET /api/v1/gamification/me

Прогресс текущего пользователя. Требует `X-User-Id`.

Ответ (200):
```json
{
  "userId": "99c03677-9069-41fe-9b25-2a75de3d3dca",
  "totalPoints": 245,
  "level": 3,
  "totalRecordings": 23,
  "achievements": [
    {
      "code": "FIRST_RECORDING",
      "title": "Первый шаг",
      "description": "Сделать первую запись",
      "icon": "🎤",
      "points": 10,
      "unlockedAt": "2026-04-08T17:38:05Z"
    }
  ],
  "nextAchievements": [
    {
      "code": "TEN_RECORDINGS",
      "title": "Активист",
      "description": "Сделать 10 записей",
      "icon": "🏆",
      "points": 50,
      "progress": 23,
      "target": 10
    }
  ]
}
```

### GET /api/v1/gamification/leaderboard

Лидерборд. **Публичный endpoint**. Запрашивает displayName из user-service.

Параметр: `limit=20` (по умолчанию)

Ответ (200):
```json
{
  "leaderboard": [
    {
      "rank": 1,
      "userId": "...",
      "displayName": "Амина",
      "totalPoints": 850,
      "totalRecordings": 75,
      "level": 7
    }
  ]
}
```

### GET /api/v1/gamification/achievements

Каталог всех доступных ачивок.

## Ачивки

Список ачивок хранится в `achievements_catalog` (Flyway миграция `V1__achievements.sql`):

| Код | Название | Условие | Очки |
|-----|----------|---------|------|
| FIRST_RECORDING | Первый шаг | 1 запись | 10 |
| FIVE_RECORDINGS | Энтузиаст | 5 записей | 25 |
| TEN_RECORDINGS | Активист | 10 записей | 50 |
| FIFTY_RECORDINGS | Профи | 50 записей | 250 |
| HUNDRED_RECORDINGS | Эксперт | 100 записей | 500 |
| MORNING_BIRD | Жаворонок | Запись до 8:00 | 15 |
| NIGHT_OWL | Сова | Запись после 22:00 | 15 |
| WEEKEND_WARRIOR | Воин выходных | Запись в выходной | 10 |

## Уровни

Уровень = `floor(totalPoints / 100) + 1`. Без ограничений.

## Как работает

Слушает 2 очереди:

**`gamification.recording.queue`** ← `recording.created`
1. Инкрементирует `totalRecordings` пользователя
2. Проверяет ачивки за количество записей (FIRST_RECORDING, FIVE_RECORDINGS, ...)
3. Проверяет ачивки за время суток (MORNING_BIRD, NIGHT_OWL)
4. Если ачивка разблокирована — публикует `achievement.unlocked`

**`gamification.achievement.queue`** ← `classification.completed`
- (placeholder) — можно добавить ачивки за классификацию (например `TRAFFIC_HUNTER` — 10 traffic записей)

## Кэш

Каталог ачивок кэшируется в Caffeine на 1 час (`@Cacheable`). Лидерборд НЕ кэшируется (запрашивается напрямую, всегда актуальный).

## Модель данных

PostgreSQL таблицы:

**`user_progress`:**
| Поле | Тип | Описание |
|------|-----|----------|
| user_id | UUID | PK |
| total_points | INT | |
| total_recordings | INT | |
| level | INT | вычисляется |
| created_at, updated_at | TIMESTAMP | |

**`achievements_catalog`:**
| Поле | Тип |
|------|-----|
| code | VARCHAR (PK) |
| title | VARCHAR |
| description | VARCHAR |
| icon | VARCHAR (emoji) |
| points | INT |
| condition_type | VARCHAR (RECORDINGS_COUNT, TIME_OF_DAY, ...) |
| condition_value | VARCHAR (JSON) |

**`user_achievements`:**
| Поле | Тип |
|------|-----|
| user_id | UUID |
| achievement_code | VARCHAR |
| unlocked_at | TIMESTAMP |

## Локальный запуск

```bash
mvn spring-boot:run -pl gamification-service -Dspring-boot.run.profiles=local
open http://localhost:8085/swagger-ui.html
```
