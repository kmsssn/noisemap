# Notification Service

In-app уведомления пользователям — ачивки, алерты о высоком шуме, результаты модерации. Хранит в MongoDB. Push через FCM запланирован (roadmap).

## API

### GET /api/v1/notifications

Мои уведомления с пагинацией. Требует `X-User-Id`. Отсортированы по дате (новые первыми).

Ответ (200):
```json
{
  "content": [
    {
      "id": "662f1a2b3c4d5e6f7a8b9c0d",
      "type": "ACHIEVEMENT_UNLOCKED",
      "title": "Новая ачивка: Первый шаг",
      "message": "Вы получили +10 очков!",
      "read": false,
      "createdAt": "2026-04-08T17:38:05Z"
    },
    {
      "id": "662f1a2b3c4d5e6f7a8b9c0e",
      "type": "NOISE_ALERT",
      "title": "Высокий уровень шума!",
      "message": "Зафиксировано 87.3 дБА. Тип шума: construction. Рекомендуется защита слуха.",
      "read": true,
      "createdAt": "2026-04-08T15:20:00Z"
    }
  ],
  "totalElements": 2,
  "totalPages": 1
}
```

### GET /api/v1/notifications/unread-count

Количество непрочитанных (для бейджа в UI).

Ответ (200): `{ "count": 3 }`

### PUT /api/v1/notifications/{id}/read

Отметить одно уведомление прочитанным.

### PUT /api/v1/notifications/read-all

Отметить все уведомления прочитанными. Использует bulk MongoDB update (один запрос).

---

## Типы уведомлений

| Тип | Кому | Когда |
|-----|------|-------|
| ACHIEVEMENT_UNLOCKED | Пользователю | Получил новую ачивку |
| NOISE_ALERT | Пользователю | Зафиксирован шум > 85 дБА |
| RECORDING_FLAGGED | Пользователю | Его запись отправлена на модерацию |
| MODERATION_ALERT | Модераторам | Новая запись в очереди модерации |

---

## События RabbitMQ

Слушает три очереди:

| Очередь | Событие | Действие |
|---------|---------|---------|
| `notification.achievement.queue` | `AchievementUnlockedEvent` | Уведомление пользователю об ачивке |
| `notification.noise.alert.queue` | `ClassificationCompletedEvent` (dBA > 85) | Алерт о высоком шуме |
| `notification.moderator.queue` | `RecordingFlaggedEvent` | Уведомление юзеру + всем модераторам |

---

## Настройка модераторов

Список модераторов которые получают уведомления о флагировании через переменную окружения:

```env
MODERATOR_IDS=uuid-модератора-1,uuid-модератора-2
```

Или в `application.yml`:
```yaml
app:
  moderation:
    moderator-ids: uuid1,uuid2
```

Если пустой — модераторы не уведомляются через notification-service (только сам пользователь получит уведомление).

---

## Локальный запуск

```bash
mvn spring-boot:run -pl notification-service -Dspring-boot.run.profiles=local
open http://localhost:8086/swagger-ui.html
```