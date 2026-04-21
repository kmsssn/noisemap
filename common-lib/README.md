# Common Lib

Общая библиотека — не сервис, не запускается отдельно. Подключается ко всем сервисам как Maven-зависимость.

## Что внутри

### События RabbitMQ

Классы-события которые сервисы отправляют друг другу через RabbitMQ:

- `RecordingCreatedEvent` — новая запись загружена (recordingId, userId, audioFileUrl, координаты, устройство)
- `ClassificationCompletedEvent` — ML обработал запись (recordingId, userId, noiseLevelDba, noiseClass, confidenceScore)
- `AchievementUnlockedEvent` — пользователь получил ачивку (userId, achievementCode, pointsAwarded)
- `RecordingFlaggedEvent` — запись помечена модерацией (recordingId, userId, reason)

### RabbitConstants

Все имена exchange, routing keys и очередей:

- Exchange: `noisemap.events` (topic)
- Routing keys: `recording.created`, `classification.completed`, `achievement.unlocked`, `recording.flagged`
- Очереди: `ml.classification.queue`, `moderation.check.queue`, `gamification.recording.queue`, `mapping.update.queue`, `statistics.update.queue`, `gamification.achievement.queue`, `notification.achievement.queue`, `notification.moderator.queue`, `notification.noise.alert.queue`

### Общие DTO

- `ApiErrorResponse` — единый формат ошибок
- `PageResponse` — обёртка пагинации

### GlobalExceptionHandler

Перехватывает исключения и возвращает ApiErrorResponse. Работает автоматически во всех сервисах.
