# User Service

Регистрация, авторизация, управление профилями, справочник калибровок устройств. Хранит данные в PostgreSQL, выдаёт JWT токены, отправляет email через Mailjet SMTP.

## API

### Аутентификация

#### POST /api/v1/auth/register

Запрос:
```json
{
  "email": "user@example.com",
  "password": "MyPass123!",
  "displayName": "Амина",
  "language": "ru"
}
```

Пароль: минимум 8 символов, 1 заглавная буква, 1 цифра, 1 спецсимвол.

Ответ (201):
```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
  "expiresIn": 3600
}
```

#### POST /api/v1/auth/login

Запрос:
```json
{
  "email": "user@example.com",
  "password": "MyPass123!"
}
```

Email нормализуется (lowercase + trim) перед поиском. Ответ (200): аналогичен register.

#### POST /api/v1/auth/forgot-password

Запрос: `{ "email": "user@example.com" }`

Отправляет письмо со ссылкой на сброс пароля через Mailjet SMTP. Всегда возвращает одинаковый ответ (защита от email enumeration):
```json
{ "message": "If the email exists, a password reset link has been sent" }
```

Ссылка в письме: `https://noisemap.duckdns.org/reset-password?token=eyJ...`

#### POST /api/v1/auth/reset-password

Запрос:
```json
{
  "resetToken": "eyJ...",
  "newPassword": "NewPass123!"
}
```

Новый пароль не должен совпадать со старым.

#### POST /api/v1/auth/change-password

Смена пароля для залогиненного пользователя. Требует `X-User-Id`.

Запрос:
```json
{
  "currentPassword": "OldPass123!",
  "newPassword": "NewPass456!"
}
```

#### POST /api/v1/auth/change-email

Смена email. Требует `X-User-Id` и подтверждения текущим паролем. Возвращает новые JWT токены.

Запрос:
```json
{
  "newEmail": "newemail@example.com",
  "currentPassword": "MyPass123!"
}
```

---

### Профиль пользователя

#### GET /api/v1/users/me

Требует `X-User-Id`.

Ответ (200):
```json
{
  "id": "99c03677-9069-41fe-9b25-2a75de3d3dca",
  "email": "user@example.com",
  "displayName": "Амина",
  "role": "USER",
  "language": "ru",
  "createdAt": "2026-04-08T17:37:39Z"
}
```

#### PUT /api/v1/users/me

Запрос: `{ "displayName": "Новое имя", "language": "kz" }`

#### GET /api/v1/users/{userId}/public

Публичные данные пользователя (только id и displayName). Используется gamification-service для лидерборда.

---

### Справочник калибровок устройств

#### GET /api/v1/devices/calibration?model={model}

Internal endpoint для recording-service. Возвращает калибровочный offset для модели устройства. Если устройство неизвестно — автоматически добавляется в справочник с offset=0.0, verified=false.

Ответ (200):
```json
{
  "model": "iPhone 13 Pro",
  "calibrationOffsetDb": -2.5,
  "verified": true
}
```

#### GET /api/v1/devices?page=0&size=20&manufacturer=Apple

Список устройств с пагинацией. Опциональный фильтр по производителю.

#### GET /api/v1/devices/{id}

Детали устройства.

#### POST /api/v1/devices

Добавить устройство в справочник (для администраторов).

Запрос:
```json
{
  "manufacturer": "Apple",
  "model": "iPhone 16 Pro",
  "calibrationOffsetDb": -1.0,
  "source": "research",
  "verified": true
}
```

#### PUT /api/v1/devices/{id}

Обновить калибровку устройства (для администраторов).

#### DELETE /api/v1/devices/{id}

Удалить устройство из справочника.

---

## Модель данных

### Таблица `users`

| Поле | Тип | Описание |
|------|-----|----------|
| id | UUID | PK, auto-generated |
| email | VARCHAR(255) | уникальный, lowercase |
| password_hash | VARCHAR | BCrypt |
| display_name | VARCHAR(50) | только безопасные символы |
| role | ENUM | USER, MODERATOR, ADMIN |
| language | VARCHAR | kz, ru, en |
| active | BOOLEAN | флаг деактивации |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |

### Таблица `device_calibrations`

| Поле | Тип | Описание |
|------|-----|----------|
| id | UUID | PK |
| manufacturer | VARCHAR(50) | Apple, Samsung, ... |
| model | VARCHAR(100) | уникальный |
| calibration_offset_db | DOUBLE | поправка микрофона в дБ |
| source | VARCHAR(20) | research / manufacturer / auto |
| verified | BOOLEAN | подтверждено администратором |
| sample_count | INTEGER | количество записей с этого устройства |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |

---

## Конфигурация (application.yml)

```yaml
spring:
  mail:
    host: ${MAIL_HOST:smtp.gmail.com}
    port: ${MAIL_PORT:587}
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}
app:
  mail:
    from: ${MAIL_FROM}
    from-name: ${MAIL_FROM_NAME:NoiseMap}
  frontend:
    base-url: ${FRONTEND_BASE_URL:https://noisemap.duckdns.org}
jwt:
  secret: ${JWT_SECRET}
  access-expiration-ms: 3600000   
  refresh-expiration-ms: 604800000 
```

---

## Безопасность

- **Email enumeration protection** — forgot-password всегда возвращает одинаковый ответ
- **Email нормализация** — trim + lowercase при register/login
- **Строгая валидация пароля** — 8+ символов, заглавная, цифра, спецсимвол
- **displayName** — только безопасные символы (запрещены `<`, `>`, `"`, `&`)
- **Токен сброса пароля** — только в email, не в API response
- **JPA Auditing** — @EnableJpaAuditing на главном классе для auto-timestamps

---

## Локальный запуск

```bash
mvn spring-boot:run -pl user-service -Dspring-boot.run.profiles=local

open http://localhost:8081/swagger-ui.html
```