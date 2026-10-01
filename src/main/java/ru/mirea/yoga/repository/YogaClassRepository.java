package ru.mirea.yoga.repository;

import ru.mirea.yoga.exception.DataAccessException;
import ru.mirea.yoga.model.ClassLevel;
import ru.mirea.yoga.model.YogaClass;
import ru.mirea.yoga.util.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Репозиторий занятий (расписание студии). Весь SQL таблицы yoga_classes собран здесь.
 */
public class YogaClassRepository implements CrudRepository<YogaClass> {

    private static final String COLUMNS =
            "id, title, instructor, level, starts_at, duration_minutes, capacity, price";

    private static final String INSERT =
            "INSERT INTO yoga_classes (title, instructor, level, starts_at, duration_minutes, capacity, price) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?)";

    // Отдельного поля "дата создания" у занятия нет, но id (SERIAL) растёт строго
    // по порядку добавления записи в базу — поэтому сортировка по id равносильна
    // сортировке по дате создания.
    private static final String SELECT_ALL =
            "SELECT " + COLUMNS + " FROM yoga_classes ORDER BY id";

    private static final String SELECT_BY_ID =
            "SELECT " + COLUMNS + " FROM yoga_classes WHERE id = ?";

    private static final String SEARCH =
            "SELECT " + COLUMNS + " FROM yoga_classes "
                    + "WHERE LOWER(title) LIKE LOWER(?) OR LOWER(instructor) LIKE LOWER(?) "
                    + "ORDER BY starts_at";

    private static final String FIND_BY_LEVEL =
            "SELECT " + COLUMNS + " FROM yoga_classes WHERE level = ? ORDER BY starts_at";

    private static final String FIND_BY_PERIOD =
            "SELECT " + COLUMNS + " FROM yoga_classes WHERE starts_at BETWEEN ? AND ? ORDER BY starts_at";

    private static final String UPDATE =
            "UPDATE yoga_classes SET title = ?, instructor = ?, level = ?, starts_at = ?, "
                    + "duration_minutes = ?, capacity = ?, price = ? WHERE id = ?";

    private static final String DELETE = "DELETE FROM yoga_classes WHERE id = ?";

    private static final String COUNT = "SELECT COUNT(*) FROM yoga_classes";

    private static final String EXISTS_INSTRUCTOR_SLOT =
            "SELECT COUNT(*) FROM yoga_classes WHERE LOWER(instructor) = LOWER(?) "
                    + "AND starts_at = ? AND (? = 0 OR id <> ?)";

    @Override
    public int save(YogaClass yogaClass) {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {

            fill(statement, yogaClass);
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt("id");
                    yogaClass.setId(id);
                    return id;
                }
            }
            throw new DataAccessException("не удалось получить идентификатор нового занятия");
        } catch (SQLException e) {
            throw new DataAccessException("не удалось сохранить занятие: " + e.getMessage(), e);
        }
    }

    @Override
    public List<YogaClass> findAll() {
        return query(SELECT_ALL);
    }

    @Override
    public Optional<YogaClass> findById(int id) {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_ID)) {

            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapRow(resultSet)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("не удалось найти занятие по ID: " + e.getMessage(), e);
        }
    }

    /** Поиск занятия по названию направления или имени инструктора. */
    public List<YogaClass> search(String query) {
        String pattern = "%" + query.trim() + "%";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(SEARCH)) {

            statement.setString(1, pattern);
            statement.setString(2, pattern);
            return read(statement);
        } catch (SQLException e) {
            throw new DataAccessException("ошибка поиска занятий: " + e.getMessage(), e);
        }
    }

    /** Фильтр занятий по уровню сложности. */
    public List<YogaClass> findByLevel(ClassLevel level) {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_LEVEL)) {

            statement.setString(1, level.name());
            return read(statement);
        } catch (SQLException e) {
            throw new DataAccessException("ошибка фильтрации занятий по уровню: " + e.getMessage(), e);
        }
    }

    /** Фильтр занятий по диапазону дат проведения. */
    public List<YogaClass> findByPeriod(LocalDateTime from, LocalDateTime to) {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_PERIOD)) {

            statement.setTimestamp(1, Timestamp.valueOf(from));
            statement.setTimestamp(2, Timestamp.valueOf(to));
            return read(statement);
        } catch (SQLException e) {
            throw new DataAccessException("ошибка фильтрации занятий по периоду: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(YogaClass yogaClass) {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE)) {

            fill(statement, yogaClass);
            statement.setInt(8, yogaClass.getId());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("не удалось обновить занятие: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteById(int id) {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE)) {

            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("не удалось удалить занятие: " + e.getMessage(), e);
        }
    }

    @Override
    public long count() {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(COUNT);
             ResultSet resultSet = statement.executeQuery()) {

            return resultSet.next() ? resultSet.getLong(1) : 0L;
        } catch (SQLException e) {
            throw new DataAccessException("не удалось посчитать занятия: " + e.getMessage(), e);
        }
    }

    /** Занят ли инструктор в это время другим занятием. */
    public boolean instructorBusy(String instructor, LocalDateTime startsAt, int excludeId) {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(EXISTS_INSTRUCTOR_SLOT)) {

            statement.setString(1, instructor);
            statement.setTimestamp(2, Timestamp.valueOf(startsAt));
            statement.setInt(3, excludeId);
            statement.setInt(4, excludeId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getLong(1) > 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("ошибка проверки занятости инструктора: " + e.getMessage(), e);
        }
    }

    @Override
    public String entityName() {
        return "Занятие";
    }

    private List<YogaClass> query(String sql) {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            return read(statement);
        } catch (SQLException e) {
            throw new DataAccessException("не удалось получить список занятий: " + e.getMessage(), e);
        }
    }

    private List<YogaClass> read(PreparedStatement statement) throws SQLException {
        List<YogaClass> classes = new ArrayList<>();
        try (ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                classes.add(mapRow(resultSet));
            }
        }
        return classes;
    }

    private void fill(PreparedStatement statement, YogaClass yogaClass) throws SQLException {
        statement.setString(1, yogaClass.getTitle());
        statement.setString(2, yogaClass.getInstructor());
        statement.setString(3, yogaClass.getLevel().name());
        statement.setTimestamp(4, Timestamp.valueOf(yogaClass.getStartsAt()));
        statement.setInt(5, yogaClass.getDurationMinutes());
        statement.setInt(6, yogaClass.getCapacity());
        statement.setBigDecimal(7, yogaClass.getPrice());
    }

    static YogaClass mapRow(ResultSet resultSet) throws SQLException {
        Timestamp startsAt = resultSet.getTimestamp("starts_at");
        return new YogaClass(
                resultSet.getInt("id"),
                resultSet.getString("title"),
                resultSet.getString("instructor"),
                ClassLevel.parse(resultSet.getString("level")),
                startsAt == null ? null : startsAt.toLocalDateTime(),
                resultSet.getInt("duration_minutes"),
                resultSet.getInt("capacity"),
                resultSet.getBigDecimal("price"));
    }
}
