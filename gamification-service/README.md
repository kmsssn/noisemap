# Gamification Service

Система мотивации пользователей — очки за записи, ачивки, уровни, лидерборд. Хранит данные в PostgreSQL.

## API

### GET /api/v1/gamification/me

Профиль геймификации. Требует `X-User-Id`.

Ответ (200):
```json
{
  "userId": "99c03677-9069-41fe-9b25-2a75de3d3dca",
  "totalPoints": 185,
  "totalRecordings": 8,
  "level": 2,
  "currentStreak": 3,
  "achievements": [
    {
      "code": "first_recording",
      "title": "Первый шаг",
      "description": "Сделайте первую запись",
      "pointsAwarded": 10,
      "unlockedAt": "2026-04-08T17:38:00Z"
    },
    {
      "code": "night_owl",
      "title": "Ночной дозор",
      "description": "Сделайте запись между 23:00 и 5:00",
      "pointsAwarded": 75,
      "unlockedAt": "2026-04-09T01:15:00Z"
    }
  ]
}
```

### GET /api/v1/gamification/leaderboard

Лидерборд. Параметр `limit` (по умолчанию 20).

Ответ (200):
```json
[
  {
    "rank": 1,
    "userId": "99c03677-9069-41fe-9b25-2a75de3d3dca",
    "totalPoints": 2350,
    "totalRecordings": 156,
    "level": 24
  },
  {
    "rank": 2,
    "userId": "a1b2c3d4-...",
    "totalPoints": 1820,
    "totalRecordings": 98,
    "level": 19
  }
]
```

## Начисление очков

- Каждая запись: +10 очков
- Бонусы за ачивки (от 10 до 2000 очков)
- Уровень = totalPoints / 100 + 1

## Ачивки

| Код | Название | Условие | Очки |
|-----|----------|---------|------|
| first_recording | Первый шаг | 1 запись | 10 |
| recordings_10 | Активист | 10 записей | 50 |
| recordings_50 | Исследователь | 50 записей | 200 |
| recordings_100 | Эксперт | 100 записей | 500 |
| recordings_500 | Легенда | 500 записей | 2000 |
| quiet_finder | Тишина и покой | запись < 40 дБА | 100 |
| loud_discoverer | Горячая точка | запись > 85 дБА | 50 |
| night_owl | Ночной дозор | запись 23:00-5:00 | 75 |
| early_bird | Ранняя пташка | запись 5:00-7:00 | 75 |
| streak_7 | Неделя подряд | 7 дней стрик | 150 |
| streak_30 | Месяц подряд | 30 дней стрик | 500 |

## Стрик

Считается по дням. Если пользователь записал звук сегодня и вчера — стрик продолжается. Пропустил день — сбрасывается на 1. Часовой пояс: Asia/Almaty.

## События RabbitMQ

Слушает:
- `gamification.recording.queue` (recording.created) — начислить очки, проверить ачивки по количеству и времени
- `gamification.achievement.queue` (classification.completed) — проверить ачивки по дБА

Публикует:
- `achievement.unlocked` → notification-service

## Модель данных

Таблица `user_scores` в PostgreSQL:

| Поле | Тип | Описание |
|------|-----|----------|
| user_id | UUID | PK |
| total_points | INTEGER | |
| total_recordings | INTEGER | |
| level | INTEGER | |
| current_streak | INTEGER | |
| last_recording_date | TIMESTAMP | |

Таблица `user_achievements`:

| Поле | Тип | Описание |
|------|-----|----------|
| id | UUID | PK |
| user_id | UUID | |
| achievement_code | VARCHAR | уникально вместе с user_id |
| achievement_title | VARCHAR | |
| description | VARCHAR | |
| points_awarded | INTEGER | |
| unlocked_at | TIMESTAMP | |
