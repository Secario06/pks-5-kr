package ru.mirea.yoga.repository;

import ru.mirea.yoga.exception.DataAccessException;
import ru.mirea.yoga.model.ClassLevel;
import ru.mirea.yoga.model.Client;
import ru.mirea.yoga.model.Enrollment;
import ru.mirea.yoga.model.EnrollmentStatus;
import ru.mirea.yoga.model.YogaClass;
import ru.mirea.yoga.util.DatabaseManager;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Репозиторий записей на занятия — основной сущности системы.
 * Все выборки идут через JOIN с clients и yoga_classes, поэтому объект Enrollment
 * сразу содержит связанные объекты Client и YogaClass.
 */
public class EnrollmentRepository implements CrudRepository<Enrollment> {

    private static final String BASE_SELECT = """
            SELECT e.id            AS e_id,
                   e.client_id     AS e_client_id,
                   e.class_id      AS e_class_id,
                   e.status        AS e_status,
                   e.created_at    AS e_created_at,
                   e.comment       AS e_comment,
                   c.full_name     AS c_full_name,
                   c.phone         AS c_phone,
                   c.email         AS c_email,
                   c.birth_date    AS c_birth_date,
                   c.registered_at AS c_registered_at,
                   k.title            AS k_title,
                   k.instructor       AS k_instructor,
                   k.level            AS k_level,
                   k.starts_at        AS k_starts_at,
                   k.duration_minutes AS k_duration,
                   k.capacity         AS k_capacity,
                   k.price            AS k_price
            FROM enrollments e
                     JOIN clients c ON c.id = e.client_id
                     JOIN yoga_classes k ON k.id = e.class_id
            """;

    private static final String INSERT =
            "INSERT INTO enrollments (client_id, class_id, status, comment) VALUES (?, ?, ?, ?)";

    private static final String SELECT_ALL = BASE_SELECT + " ORDER BY e.id";

    private static final String SELECT_BY_ID = BASE_SELECT + " WHERE e.id = ?";

    private static final String SEARCH_BY_CLIENT = BASE_SELECT
            + " WHERE LOWER(c.full_name) LIKE LOWER(?) OR c.phone LIKE ? ORDER BY k.starts_at";

    private static final String SEARCH_BY_CLASS = BASE_SELECT
            + " WHERE LOWER(k.title) LIKE LOWER(?) OR LOWER(k.instructor) LIKE LOWER(?) ORDER BY k.starts_at";

    private static final String FIND_BY_STATUS = BASE_SELECT + " WHERE e.status = ? ORDER BY k.starts_at";

    private static final String FIND_BY_LEVEL = BASE_SELECT + " WHERE k.level = ? ORDER BY k.starts_at";

    private static final String FIND_BY_CLIENT = BASE_SELECT + " WHERE e.client_id = ? ORDER BY k.starts_at";

    private static final String FIND_BY_PERIOD = BASE_SELECT
            + " WHERE k.starts_at BETWEEN ? AND ? ORDER BY k.starts_at";

    private static final String UPDATE =
            "UPDATE enrollments SET client_id = ?, class_id = ?, status = ?, comment = ? WHERE id = ?";

    private static final String DELETE = "DELETE FROM enrollments WHERE id = ?";

    private static final String COUNT = "SELECT COUNT(*) FROM enrollments";

    private static final String COUNT_OCCUPIED =
            "SELECT COUNT(*) FROM enrollments WHERE class_id = ? AND status IN ('CREATED', 'CONFIRMED', 'ATTENDED')";

    private static final String COUNT_ACTIVE_BY_CLIENT =
            "SELECT COUNT(*) FROM enrollments WHERE client_id = ? AND status IN ('CREATED', 'CONFIRMED')";

    private static final String EXISTS_DUPLICATE =
            "SELECT COUNT(*) FROM enrollments WHERE client_id = ? AND class_id = ? AND (? = 0 OR id <> ?)";

    private static final String COUNT_BY_STATUS =
            "SELECT status, COUNT(*) AS total FROM enrollments GROUP BY status";

    @Override
    public int save(Enrollment enrollment) {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {

            statement.setInt(1, enrollment.getClientId());
            statement.setInt(2, enrollment.getClassId());
            statement.setString(3, enrollment.getStatus().name());
            statement.setString(4, enrollment.getComment());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt("id");
                    enrollment.setId(id);
                    return id;
                }
            }
            throw new DataAccessException("не удалось получить идентификатор новой записи");
        } catch (SQLException e) {
            throw new DataAccessException("не удалось сохранить запись на занятие: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Enrollment> findAll() {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_ALL)) {
            return read(statement);
        } catch (SQLException e) {
            throw new DataAccessException("не удалось получить список записей: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Enrollment> findById(int id) {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_ID)) {

            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapRow(resultSet)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("не удалось найти запись по ID: " + e.getMessage(), e);
        }
    }

    /** Поиск записей по ФИО или телефону клиента. */
    public List<Enrollment> searchByClient(String query) {
        return searchWithTwoPatterns(SEARCH_BY_CLIENT, query, "поиск записей по клиенту");
    }

    /** Поиск записей по названию занятия или имени инструктора. */
    public List<Enrollment> searchByClass(String query) {
        return searchWithTwoPatterns(SEARCH_BY_CLASS, query, "поиск записей по занятию");
    }

    /** Фильтр записей по статусу. */
    public List<Enrollment> findByStatus(EnrollmentStatus status) {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_STATUS)) {

            statement.setString(1, status.name());
            return read(statement);
        } catch (SQLException e) {
            throw new DataAccessException("ошибка фильтрации записей по статусу: " + e.getMessage(), e);
        }
    }

    /** Фильтр записей по уровню сложности занятия. */
    public List<Enrollment> findByLevel(ClassLevel level) {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_LEVEL)) {

            statement.setString(1, level.name());
            return read(statement);
        } catch (SQLException e) {
            throw new DataAccessException("ошибка фильтрации записей по уровню: " + e.getMessage(), e);
        }
    }

    /** Фильтр записей по клиенту. */
    public List<Enrollment> findByClientId(int clientId) {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_CLIENT)) {

            statement.setInt(1, clientId);
            return read(statement);
        } catch (SQLException e) {
            throw new DataAccessException("ошибка фильтрации записей по клиенту: " + e.getMessage(), e);
        }
    }

    /** Фильтр записей по диапазону дат проведения занятия. */
    public List<Enrollment> findByPeriod(LocalDateTime from, LocalDateTime to) {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_PERIOD)) {

            statement.setTimestamp(1, Timestamp.valueOf(from));
            statement.setTimestamp(2, Timestamp.valueOf(to));
            return read(statement);
        } catch (SQLException e) {
            throw new DataAccessException("ошибка фильтрации записей по периоду: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(Enrollment enrollment) {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE)) {

            statement.setInt(1, enrollment.getClientId());
            statement.setInt(2, enrollment.getClassId());
            statement.setString(3, enrollment.getStatus().name());
            statement.setString(4, enrollment.getComment());
            statement.setInt(5, enrollment.getId());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("не удалось обновить запись: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteById(int id) {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE)) {

            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("не удалось удалить запись: " + e.getMessage(), e);
        }
    }

    @Override
    public long count() {
        return scalar(COUNT, null, "не удалось посчитать записи");
    }

    /** Сколько мест занято на занятии (учитываются только неотменённые записи). */
    public long countOccupiedSlots(int classId) {
        return scalar(COUNT_OCCUPIED, classId, "не удалось посчитать занятые места");
    }

    /** Количество активных записей клиента (используется при удалении клиента). */
    public long countActiveByClientId(int clientId) {
        return scalar(COUNT_ACTIVE_BY_CLIENT, clientId, "не удалось посчитать активные записи клиента");
    }

    /** Есть ли уже запись этого клиента на это занятие. */
    public boolean existsForClientAndClass(int clientId, int classId, int excludeId) {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(EXISTS_DUPLICATE)) {

            statement.setInt(1, clientId);
            statement.setInt(2, classId);
            statement.setInt(3, excludeId);
            statement.setInt(4, excludeId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getLong(1) > 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("ошибка проверки повторной записи: " + e.getMessage(), e);
        }
    }

    /** Количество записей в разрезе статусов (агрегация на стороне БД). */
    public Map<EnrollmentStatus, Long> countByStatus() {
        Map<EnrollmentStatus, Long> result = new EnumMap<>(EnrollmentStatus.class);
        for (EnrollmentStatus status : EnrollmentStatus.values()) {
            result.put(status, 0L);
        }
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(COUNT_BY_STATUS);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                result.put(EnrollmentStatus.parse(resultSet.getString("status")), resultSet.getLong("total"));
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException("не удалось сгруппировать записи по статусам: " + e.getMessage(), e);
        }
    }

    @Override
    public String entityName() {
        return "Запись на занятие";
    }

    private List<Enrollment> searchWithTwoPatterns(String sql, String query, String action) {
        String pattern = "%" + query.trim() + "%";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, pattern);
            statement.setString(2, pattern);
            return read(statement);
        } catch (SQLException e) {
            throw new DataAccessException(action + " завершился ошибкой: " + e.getMessage(), e);
        }
    }

    private long scalar(String sql, Integer parameter, String errorMessage) {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            if (parameter != null) {
                statement.setInt(1, parameter);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong(1) : 0L;
            }
        } catch (SQLException e) {
            throw new DataAccessException(errorMessage + ": " + e.getMessage(), e);
        }
    }

    private List<Enrollment> read(PreparedStatement statement) throws SQLException {
        List<Enrollment> enrollments = new ArrayList<>();
        try (ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                enrollments.add(mapRow(resultSet));
            }
        }
        return enrollments;
    }

    /** Сборка объекта Enrollment вместе со связанными Client и YogaClass. */
    private Enrollment mapRow(ResultSet resultSet) throws SQLException {
        Timestamp createdAt = resultSet.getTimestamp("e_created_at");
        Enrollment enrollment = new Enrollment(
                resultSet.getInt("e_id"),
                resultSet.getInt("e_client_id"),
                resultSet.getInt("e_class_id"),
                EnrollmentStatus.parse(resultSet.getString("e_status")),
                createdAt == null ? null : createdAt.toLocalDateTime(),
                resultSet.getString("e_comment"));

        Date birthDate = resultSet.getDate("c_birth_date");
        Timestamp registeredAt = resultSet.getTimestamp("c_registered_at");
        enrollment.setClient(new Client(
                resultSet.getInt("e_client_id"),
                resultSet.getString("c_full_name"),
                resultSet.getString("c_phone"),
                resultSet.getString("c_email"),
                birthDate == null ? null : birthDate.toLocalDate(),
                registeredAt == null ? null : registeredAt.toLocalDateTime()));

        Timestamp startsAt = resultSet.getTimestamp("k_starts_at");
        enrollment.setYogaClass(new YogaClass(
                resultSet.getInt("e_class_id"),
                resultSet.getString("k_title"),
                resultSet.getString("k_instructor"),
                ClassLevel.parse(resultSet.getString("k_level")),
                startsAt == null ? null : startsAt.toLocalDateTime(),
                resultSet.getInt("k_duration"),
                resultSet.getInt("k_capacity"),
                resultSet.getBigDecimal("k_price")));

        return enrollment;
    }
}
