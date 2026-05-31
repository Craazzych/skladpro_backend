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
./gradlew run
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
