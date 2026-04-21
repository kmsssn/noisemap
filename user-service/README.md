# User Service

Регистрация, авторизация, управление профилями. Хранит данные в PostgreSQL, выдаёт JWT токены.

## API

### POST /api/v1/auth/register

Регистрация нового пользователя.

Запрос:
```json
{
  "email": "user@example.com",
  "password": "mypassword123",
  "displayName": "Амина",
  "language": "ru",
  "deviceModel": "Samsung Galaxy S24"
}
```

Ответ (201):
```json
{
  "accessToken": "eyJhbGciOiJIUzM4NCJ9...",
  "refreshToken": "eyJhbGciOiJIUzM4NCJ9...",
  "expiresIn": 3600
}
```

### POST /api/v1/auth/login

Вход в систему.

Запрос:
```json
{
  "email": "user@example.com",
  "password": "mypassword123"
}
```

Ответ (200): такой же как у register.

### GET /api/v1/users/me

Получить свой профиль. Требует заголовок `X-User-Id`.

Ответ (200):
```json
{
  "id": "99c03677-9069-41fe-9b25-2a75de3d3dca",
  "email": "user@example.com",
  "displayName": "Амина",
  "role": "USER",
  "language": "ru",
  "deviceModel": "Samsung Galaxy S24",
  "calibrationOffset": -2.5,
  "createdAt": "2026-04-08T17:37:39Z"
}
```

### PUT /api/v1/users/me

Обновить профиль.

Запрос:
```json
{
  "displayName": "Новое имя",
  "language": "kz"
}
```

### PUT /api/v1/users/me/device

Обновить данные устройства и калибровку.

Запрос:
```json
{
  "deviceModel": "iPhone 15 Pro",
  "calibrationOffset": -1.8
}
```

### GET /api/v1/users/{userId}/calibration

Внутренний endpoint — возвращает calibrationOffset для других сервисов.

Ответ (200): `-2.5`

## Модель данных

Таблица `users` в PostgreSQL:

| Поле | Тип | Описание |
|------|-----|----------|
| id | UUID | PK, генерируется автоматически |
| email | VARCHAR | уникальный |
| password_hash | VARCHAR | BCrypt |
| display_name | VARCHAR | |
| role | ENUM | GUEST, USER, MODERATOR, ADMIN |
| language | VARCHAR | kz, ru, en |
| device_model | VARCHAR | модель смартфона |
| calibration_offset | DOUBLE | поправка микрофона в дБ |
| active | BOOLEAN | флаг деактивации |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |
