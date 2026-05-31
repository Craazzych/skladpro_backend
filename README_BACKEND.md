# SkladPRO backend

## Локальный PostgreSQL

В проекте используется локальный PostgreSQL-кластер в папке `.postgres-data`.

Запуск БД:

```bash
/Library/PostgreSQL/17/bin/pg_ctl -D .postgres-data -l .postgres-data/logfile -o '-p 5433' start
```

Остановка БД:

```bash
/Library/PostgreSQL/17/bin/pg_ctl -D .postgres-data stop
```

Backend ожидает базу `skladpro` на `localhost:5433`.

## Запуск backend

```bash
JWT_SECRET="replace-with-a-long-random-secret" ./gradlew run
```

Проверка:

```bash
curl http://127.0.0.1:8080/health
curl http://127.0.0.1:8080/api/items
curl http://127.0.0.1:8080/api/employees
```

## Учетные записи сотрудников

При первом запуске создаются тестовые учетные записи:

- администратор: логин `admin`, пароль `admin123`;
- неактивированный сотрудник: логин `m.orlova`, временный пароль `TMP-654321`.

Основные маршруты:

```text
GET    /api/employees
POST   /api/employees
DELETE /api/employees/{id}
POST   /api/auth/activate
POST   /api/auth/login
```

После создания сотрудника сервер возвращает временный пароль. После успешной
активации временный пароль удаляется, а новый пароль хранится в виде
PBKDF2-хеша.

После входа `POST /api/auth/login` возвращает JWT-токен. Защищенные маршруты
ожидают заголовок:

```text
Authorization: Bearer <token>
```

Работник может просматривать запасы и сообщать о списании. Создание,
редактирование и удаление товаров, управление поставками и сотрудниками
доступны только администратору.

Переменная окружения `JWT_SECRET` обязательна для запуска backend. Для
развертывания используйте длинное случайное значение и не добавляйте его в Git.
