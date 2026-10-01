-- =====================================================================
--  КР 1. Информационная система «Йога-студия: запись на занятие»
--  СУБД: PostgreSQL
--
--  Создание базы (выполняется отдельно, вне этого скрипта):
--      CREATE DATABASE yoga_studio;
--  Затем:  psql -U postgres -d yoga_studio -f database/schema.sql
-- =====================================================================

DROP TABLE IF EXISTS enrollments;
DROP TABLE IF EXISTS yoga_classes;
DROP TABLE IF EXISTS clients;

-- ---------------------------------------------------------------------
-- Клиенты студии (участник предметной области)
-- ---------------------------------------------------------------------
CREATE TABLE clients
(
    id            SERIAL PRIMARY KEY,
    full_name     VARCHAR(100) NOT NULL,
    phone         VARCHAR(20)  NOT NULL UNIQUE,
    email         VARCHAR(100) UNIQUE,
    birth_date    DATE         NOT NULL,
    registered_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_clients_full_name CHECK (LENGTH(TRIM(full_name)) >= 3),
    CONSTRAINT chk_clients_phone     CHECK (phone ~ '^\+7[0-9]{10}$'),
    CONSTRAINT chk_clients_birthdate CHECK (birth_date < CURRENT_DATE)
);

COMMENT ON TABLE clients IS 'Клиенты йога-студии';

-- ---------------------------------------------------------------------
-- Занятия (расписание студии)
-- ---------------------------------------------------------------------
CREATE TABLE yoga_classes
(
    id               SERIAL PRIMARY KEY,
    title            VARCHAR(100)   NOT NULL,
    instructor       VARCHAR(100)   NOT NULL,
    level            VARCHAR(20)    NOT NULL,
    starts_at        TIMESTAMP      NOT NULL,
    duration_minutes INTEGER        NOT NULL,
    capacity         INTEGER        NOT NULL,
    price            NUMERIC(10, 2) NOT NULL,

    CONSTRAINT chk_classes_level    CHECK (level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED')),
    CONSTRAINT chk_classes_duration CHECK (duration_minutes BETWEEN 30 AND 180),
    CONSTRAINT chk_classes_capacity CHECK (capacity BETWEEN 1 AND 40),
    CONSTRAINT chk_classes_price    CHECK (price >= 0),
    -- один инструктор не может вести два занятия в одно и то же время
    CONSTRAINT uq_classes_slot      UNIQUE (instructor, starts_at)
);

COMMENT ON TABLE yoga_classes IS 'Расписание занятий студии';

-- ---------------------------------------------------------------------
-- Записи на занятия (основная сущность варианта)
--   enrollments.client_id -> clients.id
--   enrollments.class_id  -> yoga_classes.id
-- ---------------------------------------------------------------------
CREATE TABLE enrollments
(
    id         SERIAL PRIMARY KEY,
    client_id  INTEGER     NOT NULL,
    class_id   INTEGER     NOT NULL,
    status     VARCHAR(20) NOT NULL DEFAULT 'CREATED',
    created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    comment    VARCHAR(255),

    CONSTRAINT fk_enrollments_client FOREIGN KEY (client_id) REFERENCES clients (id) ON DELETE CASCADE,
    CONSTRAINT fk_enrollments_class  FOREIGN KEY (class_id) REFERENCES yoga_classes (id) ON DELETE CASCADE,
    CONSTRAINT chk_enrollments_status CHECK (status IN ('CREATED', 'CONFIRMED', 'ATTENDED', 'CANCELLED', 'NO_SHOW')),
    -- клиент не может быть записан на одно занятие дважды
    CONSTRAINT uq_enrollments_client_class UNIQUE (client_id, class_id)
);

COMMENT ON TABLE enrollments IS 'Записи клиентов на занятия';

CREATE INDEX idx_enrollments_status ON enrollments (status);
CREATE INDEX idx_enrollments_client ON enrollments (client_id);
CREATE INDEX idx_classes_starts_at  ON yoga_classes (starts_at);

-- =====================================================================
--  Начальные тестовые данные
--  Даты заданы относительно текущей — часть занятий уже прошла,
--  часть предстоит, что позволяет проверить все статусы и фильтры.
-- =====================================================================

INSERT INTO clients (full_name, phone, email, birth_date)
VALUES ('Анна Соколова', '+79101112233', 'sokolova@mail.ru', DATE '1995-03-14'),
       ('Дмитрий Орлов', '+79102223344', 'orlov.d@gmail.com', DATE '1988-11-02'),
       ('Мария Кузнецова', '+79103334455', 'kuznetsova@yandex.ru', DATE '2000-07-23'),
       ('Игорь Лебедев', '+79104445566', 'lebedev@mail.ru', DATE '1979-01-30'),
       ('Ольга Морозова', '+79105556677', 'morozova@inbox.ru', DATE '1992-05-18'),
       ('Павел Ветров', '+79106667788', NULL, DATE '2004-09-09');

INSERT INTO yoga_classes (title, instructor, level, starts_at, duration_minutes, capacity, price)
VALUES ('Хатха-йога', 'Елена Смирнова', 'BEGINNER',
        (CURRENT_DATE - 14) + TIME '18:30', 90, 12, 1200.00),
       ('Виньяса-флоу', 'Артём Волков', 'INTERMEDIATE',
        (CURRENT_DATE - 10) + TIME '19:00', 75, 10, 1500.00),
       ('Йога-нидра', 'Елена Смирнова', 'BEGINNER',
        (CURRENT_DATE - 5) + TIME '20:00', 60, 15, 1000.00),
       ('Аштанга-йога', 'Артём Волков', 'ADVANCED',
        (CURRENT_DATE - 2) + TIME '08:00', 120, 8, 2000.00),
       ('Хатха-йога', 'Елена Смирнова', 'BEGINNER',
        (CURRENT_DATE + 1) + TIME '18:30', 90, 12, 1200.00),
       ('Йога для спины', 'Наталья Зайцева', 'BEGINNER',
        (CURRENT_DATE + 2) + TIME '10:00', 60, 14, 1100.00),
       ('Виньяса-флоу', 'Артём Волков', 'INTERMEDIATE',
        (CURRENT_DATE + 3) + TIME '19:00', 75, 10, 1500.00),
       ('Аштанга-йога', 'Артём Волков', 'ADVANCED',
        (CURRENT_DATE + 5) + TIME '08:00', 120, 8, 2000.00),
       ('Кундалини-йога', 'Наталья Зайцева', 'INTERMEDIATE',
        (CURRENT_DATE + 7) + TIME '19:30', 90, 10, 1700.00),
       ('Йога-нидра', 'Елена Смирнова', 'BEGINNER',
        (CURRENT_DATE + 10) + TIME '20:00', 60, 15, 1000.00);

INSERT INTO enrollments (client_id, class_id, status, comment)
VALUES
    -- прошедшие занятия: посещения, неявки и отмена
    (1, 1, 'ATTENDED', 'Первое пробное занятие'),
    (2, 1, 'ATTENDED', NULL),
    (3, 1, 'NO_SHOW', 'Не пришла, не предупредила'),
    (2, 2, 'ATTENDED', NULL),
    (4, 2, 'CANCELLED', 'Отменил за сутки'),
    (1, 3, 'ATTENDED', 'Вечерняя практика'),
    (5, 3, 'ATTENDED', NULL),
    (4, 4, 'ATTENDED', 'Постоянный клиент'),
    (2, 4, 'NO_SHOW', NULL),

    -- предстоящие занятия: созданные и подтверждённые записи
    (1, 5, 'CONFIRMED', 'Оплачено абонементом'),
    (3, 5, 'CREATED', NULL),
    (6, 5, 'CREATED', 'Новый клиент, нужен коврик'),
    (5, 6, 'CONFIRMED', 'Проблемы с осанкой'),
    (2, 7, 'CONFIRMED', NULL),
    (4, 8, 'CREATED', 'Просил место у окна'),
    (1, 9, 'CREATED', NULL),
    (5, 9, 'CANCELLED', 'Уезжает в командировку'),
    (6, 10, 'CREATED', NULL);

-- Проверка загруженных данных
SELECT 'clients' AS table_name, COUNT(*) AS rows FROM clients
UNION ALL
SELECT 'yoga_classes', COUNT(*) FROM yoga_classes
UNION ALL
SELECT 'enrollments', COUNT(*) FROM enrollments;
