# ER-диаграмма базы данных `yoga_studio`

```mermaid
erDiagram
    CLIENTS ||--o{ ENROLLMENTS : "оформляет"
    YOGA_CLASSES ||--o{ ENROLLMENTS : "содержит"

    CLIENTS {
        SERIAL       id PK
        VARCHAR(100) full_name  "NOT NULL"
        VARCHAR(20)  phone      "NOT NULL, UNIQUE, формат +7XXXXXXXXXX"
        VARCHAR(100) email      "UNIQUE"
        DATE         birth_date "NOT NULL, < CURRENT_DATE"
        TIMESTAMP    registered_at "NOT NULL, DEFAULT now()"
    }

    YOGA_CLASSES {
        SERIAL         id PK
        VARCHAR(100)   title      "NOT NULL"
        VARCHAR(100)   instructor "NOT NULL"
        VARCHAR(20)    level      "NOT NULL, CHECK: BEGINNER|INTERMEDIATE|ADVANCED"
        TIMESTAMP      starts_at  "NOT NULL, UNIQUE(instructor, starts_at)"
        INTEGER        duration_minutes "NOT NULL, CHECK 30..180"
        INTEGER        capacity   "NOT NULL, CHECK 1..40"
        NUMERIC(10,2)  price      "NOT NULL, CHECK >= 0"
    }

    ENROLLMENTS {
        SERIAL       id PK
        INTEGER      client_id FK "NOT NULL → clients.id, ON DELETE CASCADE"
        INTEGER      class_id  FK "NOT NULL → yoga_classes.id, ON DELETE CASCADE"
        VARCHAR(20)  status    "NOT NULL, CHECK: CREATED|CONFIRMED|ATTENDED|CANCELLED|NO_SHOW"
        TIMESTAMP    created_at "NOT NULL, DEFAULT now()"
        VARCHAR(255) comment
    }
```

## Связи

| Связь | Тип | Внешний ключ |
|---|---|---|
| Клиент — Записи | один-ко-многим | `enrollments.client_id → clients.id` |
| Занятие — Записи | один-ко-многим | `enrollments.class_id → yoga_classes.id` |

Таблица `enrollments` реализует связь «многие-ко-многим» между клиентами и занятиями
и одновременно является основной сущностью системы: она хранит собственные атрибуты
(статус, дату создания, комментарий).

## Ограничения целостности

* **PRIMARY KEY** — `clients.id`, `yoga_classes.id`, `enrollments.id` (SERIAL).
* **FOREIGN KEY** — `enrollments.client_id`, `enrollments.class_id` с `ON DELETE CASCADE`.
* **NOT NULL** — все обязательные поля предметной области.
* **UNIQUE** — `clients.phone`, `clients.email`, `(yoga_classes.instructor, starts_at)`,
  `(enrollments.client_id, enrollments.class_id)`.
* **CHECK** — допустимые значения `level` и `status`, диапазоны длительности,
  вместимости и цены, формат телефона, дата рождения в прошлом.
* **INDEX** — `enrollments.status`, `enrollments.client_id`, `yoga_classes.starts_at`.

Ограничения БД дублируют бизнес-правила уровня Java: даже при прямой правке данных
через `psql` некорректная запись в базу не попадёт.
