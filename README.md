# КР 1. Информационная система «Йога-студия: запись на занятие»

Консольное Java-приложение для учёта клиентов, расписания занятий и записей на них.

**Архитектура:** `Console UI → Service → Repository (JDBC) → PostgreSQL`

| Требование КР | Где реализовано |
|---|---|
| Основная сущность | `Enrollment` — запись клиента на занятие |
| Вторая сущность (участник) | `Client` — клиент студии |
| Третья таблица | `YogaClass` — занятие в расписании |
| enum | `EnrollmentStatus`, `ClassLevel` |
| Интерфейсы | `Identifiable`, `CrudRepository<T>`, `DataExporter` |
| Собственные исключения | `AppException` и 4 наследника |
| Экспорт | Excel (.xlsx, Apache POI) и CSV |

---

## Способ A. Запуск через Docker (рекомендуется)

Не нужно ставить JDK, Maven или PostgreSQL на компьютер — всё собирается и запускается
в контейнерах. Нужен только [Docker Desktop](https://www.docker.com/products/docker-desktop/)
(включает `docker compose`).

### A.1. Поднять базу данных

```bash
docker compose up -d db
```

Postgres поднимается в контейнере, а `database/schema.sql` монтируется в
`/docker-entrypoint-initdb.d/` и выполняется автоматически при первом старте —
создаются три таблицы и тестовые данные (6 клиентов, 10 занятий, 18 записей).
Дождаться готовности БД (обычно 5–10 секунд) можно командой:

```bash
docker compose ps
# db должен быть в статусе "healthy"
```

### A.2. Собрать и запустить приложение

Консольное приложение интерактивное, поэтому его запускают через `run`
(а не через `up`, где стандартный ввод не привязан к конкретному сервису):

```bash
docker compose run --rm app
```

Первый запуск соберёт образ (Maven-сборка внутри контейнера, JDK 21 не нужен на хосте) —
это займёт около минуты. Приложение подключается к БД по внутреннему имени `db:5432` —
никаких дополнительных настроек не требуется.

Каталог `export/` на хосте примонтирован в контейнер, поэтому файлы Excel/CSV,
выгруженные через пункт меню «Экспорт данных», сразу появляются в `export/` рядом с проектом.

### A.3. Остановить

```bash
docker compose down          # остановить контейнеры, данные в БД сохранятся
docker compose down -v       # то же самое, но удалить и данные БД (следующий запуск — с нуля)
```

### A.4. Если на хосте уже был запущен свой PostgreSQL на порту 5432

Он будет конфликтовать с портом БД в docker-compose. Отключите локальный сервис:

```bash
brew services stop postgresql@16    # macOS + Homebrew
```

---

## Способ B. Локальный запуск (JDK + Maven + PostgreSQL)

### B.1. Требования к окружению

* JDK 21 и выше
* Apache Maven 3.8 и выше
* PostgreSQL 14 и выше

Проверка:

```bash
java -version && mvn -version && psql --version
```

### B.2. Создание базы данных

Если сервер PostgreSQL не запущен (macOS + Homebrew):

```bash
brew services start postgresql@16
```

Затем:

```bash
createdb -U postgres yoga_studio
psql -U postgres -d yoga_studio -f database/schema.sql
```

Скрипт `database/schema.sql` создаёт три таблицы со всеми ограничениями и загружает
тестовые данные: 6 клиентов, 10 занятий, 18 записей во всех пяти статусах.

Если пользователь/пароль отличаются от `postgres/postgres`, отредактируйте
`src/main/resources/application.properties` либо задайте переменные окружения:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/yoga_studio
export DB_USER=postgres
export DB_PASSWORD=postgres
```

### B.3. Сборка

```bash
mvn clean package
```

В каталоге `target/` появится исполняемый jar со всеми зависимостями — `yoga-studio.jar`.

### B.4. Запуск

```bash
java -jar target/yoga-studio.jar
```

Альтернативно, без сборки jar:

```bash
mvn exec:java
```

---

## 5. Главное меню

```
==============================================================================
  ИНФОРМАЦИОННАЯ СИСТЕМА «ЙОГА-СТУДИЯ»
==============================================================================
1. Клиенты
2. Расписание занятий
3. Записи на занятия
4. Поиск
5. Фильтрация
6. Статистика
7. Экспорт данных
8. Вывести таблицы базы данных
0. Выход
```

После каждой операции программа возвращается в соответствующее меню.

## 6. Экспорт

Пункт «Экспорт данных» → формат (Excel или CSV) → каталог (по умолчанию `export/`).

Excel-файл содержит четыре листа: **Клиенты**, **Расписание**, **Записи на занятия**, **Статистика**.

---

## 7. Структура проекта

```
Dockerfile                          — сборка jar + образ для запуска (см. «Способ A»)
docker-compose.yml                  — сервисы app + db (PostgreSQL)
database/schema.sql                 — SQL-скрипт (используется и локально, и в Docker)

src/main/java/ru/mirea/yoga/
├── Main.java                     — точка входа
├── model/                        — предметная модель
│   ├── Identifiable.java         — интерфейс сущности с ID
│   ├── BaseEntity.java           — абстрактный базовый класс
│   ├── Client.java               — клиент студии
│   ├── YogaClass.java            — занятие
│   ├── Enrollment.java           — запись на занятие (основная сущность)
│   ├── EnrollmentStatus.java     — enum статусов + машина состояний
│   └── ClassLevel.java           — enum уровней сложности
├── repository/                   — слой доступа к данным (JDBC)
│   ├── CrudRepository.java       — общий интерфейс репозитория
│   ├── ClientRepository.java
│   ├── YogaClassRepository.java
│   ├── EnrollmentRepository.java
│   └── DatabaseInspectRepository.java — вывод таблиц БД
├── service/                      — бизнес-логика и проверки
│   ├── ClientService.java
│   ├── YogaClassService.java
│   ├── EnrollmentService.java
│   └── StatisticsService.java
├── exception/                    — собственные исключения
│   ├── AppException.java
│   ├── ValidationException.java
│   ├── BusinessRuleException.java
│   ├── EntityNotFoundException.java
│   └── DataAccessException.java
├── export/                       — экспорт данных
│   ├── DataExporter.java         — интерфейс
│   ├── ExcelExporter.java        — .xlsx через Apache POI
│   └── CsvExporter.java          — .csv
└── ui/                           — консольный интерфейс
    ├── ConsoleApp.java           — сборка приложения и главное меню
    ├── ConsoleInput.java         — безопасный ввод
    ├── ConsolePrinter.java       — форматированный вывод
    ├── ClientMenu.java
    ├── ClassMenu.java
    ├── EnrollmentMenu.java
    ├── SearchMenu.java
    ├── FilterMenu.java
    ├── StatisticsMenu.java
    ├── ExportMenu.java
    └── DatabaseMenu.java
```

## 8. Возможности системы

**CRUD для основной сущности (`Enrollment`)**: создание, вывод всех, получение по ID,
изменение, удаление, поиск, фильтрация, сортировка.

**Поиск (4 способа):** клиент по ФИО/телефону/e-mail; занятие по направлению/инструктору;
записи по клиенту; записи по занятию или инструктору.

**Фильтрация (6 фильтров):** записи по статусу, по уровню занятия, по клиенту, за период;
занятия по уровню, за период.

**Сортировка (9 способов):** клиенты — по ФИО, возрасту, дате регистрации; занятия — по дате,
цене, уровню; записи — по дате занятия, ФИО клиента, статусу.

**Статистика (11 показателей):** всего клиентов / занятий / записей, активные записи,
средняя заполняемость, доля отмен, выручка, самый активный клиент, разбивка по статусам,
разбивка занятий по уровням, топ-3 направления.

## 9. Бизнес-правила

| № | Правило | Класс |
|---|---------|-------|
| 1 | Нельзя записать несуществующего клиента | `EnrollmentService.create` |
| 2 | Нельзя записать на несуществующее занятие | `EnrollmentService.create` |
| 3 | Нельзя записаться на уже начавшееся занятие | `EnrollmentService.create` |
| 4 | Клиент не может записаться на одно занятие дважды | `EnrollmentService.create` |
| 5 | Нельзя превысить вместимость зала | `EnrollmentService.create` |
| 6 | Запись в финальном статусе изменять нельзя | `EnrollmentService.update` |
| 7 | Запрещены недопустимые переходы между статусами | `EnrollmentService.changeStatus` |
| 8 | Отметить посещение можно только после начала занятия | `EnrollmentService.changeStatus` |
| 9 | Телефон и e-mail клиента уникальны | `ClientService.create/update` |
| 10 | Возраст клиента — от 14 до 100 лет, ФИО и телефон обязательны и проверяются по формату | `ClientService.validate` |
| 11 | Нельзя удалить клиента с активными записями | `ClientService.delete` |
| 12 | Один инструктор не ведёт два занятия одновременно | `YogaClassService.create/update` |
| 13 | Вместимость нельзя опустить ниже числа занятых мест | `YogaClassService.update` |
| 14 | Нельзя удалить занятие, на которое есть записи | `YogaClassService.delete` |

Разрешённые переходы статусов (`EnrollmentStatus`):

```
CREATED   → CONFIRMED, CANCELLED
CONFIRMED → ATTENDED, NO_SHOW, CANCELLED
ATTENDED  → (финальный)
CANCELLED → (финальный)
NO_SHOW   → (финальный)
```

## 10. Обработка ошибок

| Ситуация | Поведение |
|---|---|
| Текст вместо числа | `Ошибка: ID должен быть целым числом (получено: 'abc').` — повторный запрос |
| Нет записи с таким ID | `EntityNotFoundException` → `[ОШИБКА] Клиент с ID = 999 не найден(а).` |
| Нарушение бизнес-правила | `BusinessRuleException` → сообщение с причиной |
| Некорректный формат данных | `ValidationException` |
| Нет подключения к БД / ошибка SQL | `DataAccessException` (оборачивает `SQLException`) |

Программа не завершается аварийно: все прикладные исключения перехватываются
в меню и в главном цикле `ConsoleApp.run()`.

## 11. Документация

* `database/schema.sql` — SQL-скрипт создания БД и тестовых данных
* `docs/er-diagram.md` — ER-диаграмма (Mermaid) и описание связей
* `docs/er-diagram.svg` — ER-диаграмма в виде изображения
* `docs/defense.md` — ответы на вопросы к защите
* `Dockerfile`, `docker-compose.yml` — контейнеризация (см. «Способ A» выше)
# PKS_SEM5_KR_1
