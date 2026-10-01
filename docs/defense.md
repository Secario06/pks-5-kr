# Ответы на вопросы к защите КР 1

Вариант: **йога-студия — запись на занятие**.
Основная сущность — `Enrollment` (запись клиента на занятие).

---

## 1. Назначение основных классов

| Слой | Класс | Назначение |
|---|---|---|
| Точка входа | `Main` | запускает приложение, ловит фатальные ошибки |
| UI | `ConsoleApp` | собирает объекты всех слоёв, главное меню, главный цикл |
| UI | `*Menu` | диалог с пользователем по разделам, SQL и проверок не содержат |
| UI | `ConsoleInput` / `ConsolePrinter` | безопасный ввод и форматированный вывод |
| Service | `ClientService`, `YogaClassService`, `EnrollmentService` | бизнес-правила и валидация |
| Service | `StatisticsService` | агрегация показателей (Stream API) |
| Repository | `ClientRepository`, `YogaClassRepository`, `EnrollmentRepository` | JDBC-операции с таблицами |
| Repository | `DatabaseInspectRepository` | вывод содержимого таблиц БД |
| Model | `Client`, `YogaClass`, `Enrollment` | предметная модель |
| Model | `EnrollmentStatus`, `ClassLevel` | перечисления |
| Exception | `AppException` + 4 наследника | собственные исключения |
| Export | `DataExporter`, `ExcelExporter`, `CsvExporter` | выгрузка данных |

## 2. Инкапсуляция

Все поля моделей объявлены `private`, доступ — через геттеры и сеттеры
(`Client.java`, `YogaClass.java`, `Enrollment.java`). Идентификатор вынесен
в базовый класс `BaseEntity` и недоступен наследникам напрямую.

Пример «умного» сеттера — `Enrollment.setYogaClass()`: он одновременно
синхронизирует поле `classId`, поэтому объект не может оказаться в противоречивом состоянии.

Правила предметной области спрятаны внутри `EnrollmentStatus`: карта разрешённых
переходов `TRANSITIONS` приватна, наружу отдаются только методы `canChangeTo()`,
`isFinal()`, `occupiesSlot()`.

## 3. Назначение интерфейсов

* `Identifiable` — контракт «сущность с первичным ключом»; позволяет описать
  обобщённый репозиторий `CrudRepository<T extends Identifiable>`.
* `CrudRepository<T>` — единый контракт слоя доступа к данным. Все три репозитория
  реализуют одни и те же операции, что упрощает замену реализации (в КР 3 — на Spring Data).
* `DataExporter` — контракт экспорта. `ExcelExporter` и `CsvExporter` подставляются
  в `ExportMenu` как `List<DataExporter>`, меню не знает о конкретных классах.

## 4. Применение enum

* `EnrollmentStatus` — 5 статусов записи, у каждого русское название (`title`).
  Внутри enum реализована **машина состояний**: `TRANSITIONS`, `canChangeTo()`,
  `isFinal()`, `occupiesSlot()`.
* `ClassLevel` — уровень сложности занятия (3 значения).
* Метод `parse()` в обоих enum безопасно разбирает строку из БД или ввода
  и бросает понятное исключение при недопустимом значении.
* В БД те же значения защищены ограничением `CHECK`.

## 5. Полиморфизм

1. **Переопределение методов.** `BaseEntity.toTableRow()` абстрактный; каждая сущность
   выводит себя по-своему. `ConsolePrinter.printTable(..., List<? extends BaseEntity>)`
   печатает любой список сущностей, не зная их типа.
2. **Через интерфейс.** `ExportMenu` работает со списком `List<DataExporter>` —
   вызывается `exporter.export(...)`, а какая реализация отработает, определяется в runtime.
3. **Обобщённый репозиторий.** `CrudRepository<T>` реализован тремя классами с разным SQL.
4. **Иерархия исключений.** Один `catch (AppException e)` перехватывает
   `ValidationException`, `BusinessRuleException`, `EntityNotFoundException`, `DataAccessException`.

## 6. Коллекции и Stream API

* `List<Client>`, `List<YogaClass>`, `List<Enrollment>` — результаты выборок.
* `EnumSet` / `Map<EnrollmentStatus, Set<EnrollmentStatus>>` — таблица переходов статусов.
* `EnumMap` — счётчики по статусам и уровням.
* `LinkedHashMap` — топ-3 направления с сохранением порядка сортировки.
* Stream API — сортировки (`sorted(Comparator...)`), фильтры (`filter`),
  агрегации (`Collectors.groupingBy`, `counting`, `reduce`).

Пример (`EnrollmentService.activeUpcoming()`):

```java
return enrollmentRepository.findAll().stream()
        .filter(enrollment -> enrollment.getStatus().occupiesSlot())
        .filter(enrollment -> !enrollment.getYogaClass().isStarted())
        .sorted(Comparator.comparing(Enrollment::getClassStartsAt))
        .toList();
```

## 7. Обработка исключений

```
AppException (RuntimeException)
├── ValidationException      — неверный формат/пустое поле
├── BusinessRuleException    — нарушение правила предметной области
├── EntityNotFoundException  — нет записи с указанным ID
└── DataAccessException      — ошибки JDBC (обёртка над SQLException)
```

* `SQLException` — проверяемое исключение; репозиторий ловит его и оборачивает
  в `DataAccessException`, чтобы верхние слои не зависели от JDBC.
* Ошибки ввода (буквы вместо числа, неверная дата) обрабатываются в `ConsoleInput`:
  пользователю выводится сообщение и повторяется запрос.
* Прикладные исключения перехватываются в меню и в `ConsoleApp.run()` —
  программа продолжает работу.

## 8. Как работает JDBC

1. `Class.forName("org.postgresql.Driver")` — загрузка драйвера (`DatabaseManager`).
2. `DriverManager.getConnection(url, user, password)` — установка соединения.
3. `connection.prepareStatement(sql)` — подготовка параметризованного запроса.
4. `statement.setString(1, value)` / `setInt` / `setTimestamp` — подстановка параметров.
5. `executeQuery()` → `ResultSet` для SELECT, `executeUpdate()` → число строк для INSERT/UPDATE/DELETE.
6. `Statement.RETURN_GENERATED_KEYS` + `getGeneratedKeys()` — получение сгенерированного ID.
7. `try-with-resources` — `Connection`, `PreparedStatement` и `ResultSet` закрываются автоматически.

```java
try (Connection connection = DatabaseManager.getConnection();
     PreparedStatement statement = connection.prepareStatement(SELECT_BY_ID)) {
    statement.setInt(1, id);
    try (ResultSet resultSet = statement.executeQuery()) {
        return resultSet.next() ? Optional.of(mapRow(resultSet)) : Optional.empty();
    }
} catch (SQLException e) {
    throw new DataAccessException("не удалось найти клиента по ID: " + e.getMessage(), e);
}
```

## 9. Statement и PreparedStatement

| | `Statement` | `PreparedStatement` |
|---|---|---|
| SQL | собирается конкатенацией строк | параметризован, `?` вместо значений |
| Компиляция | каждый раз заново | компилируется один раз, кэшируется |
| SQL-инъекции | уязвим | защищён — параметр всегда трактуется как значение |
| Типы данных | всё вручную в строку | `setInt`, `setDate`, `setBigDecimal`, `setNull` |

Опасно:

```java
"SELECT * FROM clients WHERE email = '" + email + "'"   // email = "' OR '1'='1"
```

Правильно (используется во всём проекте):

```java
PreparedStatement statement = connection.prepareStatement(
        "SELECT * FROM clients WHERE email = ?");
statement.setString(1, email);
```

Единственное место, где имя таблицы подставляется в строку —
`DatabaseInspectRepository.dump()`, так как имя таблицы нельзя передать параметром.
Там применён «белый список» разрешённых таблиц.

## 10. Связи между таблицами

```
clients (1) ────< enrollments >──── (1) yoga_classes
```

* `enrollments.client_id → clients.id` — один клиент имеет много записей;
* `enrollments.class_id → yoga_classes.id` — одно занятие содержит много записей;
* `enrollments` — связующая таблица «многие-ко-многим» с собственными атрибутами
  (статус, дата создания, комментарий);
* `UNIQUE (client_id, class_id)` запрещает повторную запись;
* `ON DELETE CASCADE` удаляет записи вместе с клиентом или занятием.

## 11. Основные бизнес-правила

Полный список из 14 правил — в `README.md`, раздел 9. Ключевые:

1. Нельзя записать несуществующего клиента или на несуществующее занятие.
2. Нельзя записаться на уже начавшееся занятие.
3. Клиент не может быть записан на одно занятие дважды.
4. Нельзя превысить вместимость зала (считаются статусы CREATED, CONFIRMED, ATTENDED).
5. Запрещены недопустимые переходы статуса (например, `CANCELLED → ATTENDED`).
6. Отметить посещение можно только после начала занятия.
7. Нельзя удалить клиента с активными записями.

## 12. Создание, изменение и удаление записей

* **Создание** — меню «Записи → Записать клиента на занятие»:
  `EnrollmentMenu.create()` → `EnrollmentService.create()` (пять проверок) →
  `EnrollmentRepository.save()` (INSERT + `getGeneratedKeys`).
* **Изменение** — перенос на другое занятие и комментарий (`update`),
  отдельно — смена статуса (`changeStatus`) с проверкой машины состояний.
* **Удаление** — физическое удаление строки (`delete`) с подтверждением;
  штатный сценарий отмены — перевод в статус `CANCELLED`.

## 13. Поиск, фильтрация и сортировка

* **Поиск** (4 способа) — SQL с `LIKE` и `LOWER()`, параметр передаётся через `?`.
* **Фильтрация** (6 фильтров) — SQL `WHERE` по статусу, уровню, клиенту, диапазону дат.
* **Сортировка** (9 способов) — на стороне Java через `Comparator` и Stream API,
  что демонстрирует работу с коллекциями (в БД часть сортировок задана в `ORDER BY`).

## 14. Экспорт данных

`ExcelExporter` (Apache POI, `XSSFWorkbook`) формирует `.xlsx` с четырьмя листами:
Клиенты, Расписание, Записи на занятия, Статистика. Заголовки выделены стилем,
первая строка закреплена, ширина колонок подобрана автоматически.
`CsvExporter` создаёт три `.csv`-файла (разделитель `;`, UTF-8 с BOM для корректного
открытия в Excel). Оба класса реализуют интерфейс `DataExporter`.
