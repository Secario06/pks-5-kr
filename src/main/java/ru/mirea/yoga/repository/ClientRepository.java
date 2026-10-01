package ru.mirea.yoga.repository;

import ru.mirea.yoga.exception.DataAccessException;
import ru.mirea.yoga.model.Client;
import ru.mirea.yoga.util.DatabaseManager;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Репозиторий клиентов студии. Единственное место, где находится SQL для таблицы clients.
 * Используются только параметризованные запросы (PreparedStatement) и try-with-resources.
 */
public class ClientRepository implements CrudRepository<Client> {

    private static final String INSERT =
            "INSERT INTO clients (full_name, phone, email, birth_date) VALUES (?, ?, ?, ?)";

    private static final String SELECT_ALL =
            "SELECT id, full_name, phone, email, birth_date, registered_at FROM clients ORDER BY id";

    private static final String SELECT_BY_ID =
            "SELECT id, full_name, phone, email, birth_date, registered_at FROM clients WHERE id = ?";

    private static final String SEARCH =
            "SELECT id, full_name, phone, email, birth_date, registered_at FROM clients "
                    + "WHERE LOWER(full_name) LIKE LOWER(?) OR phone LIKE ? OR LOWER(COALESCE(email, '')) LIKE LOWER(?) "
                    + "ORDER BY full_name";

    private static final String UPDATE =
            "UPDATE clients SET full_name = ?, phone = ?, email = ?, birth_date = ? WHERE id = ?";

    private static final String DELETE = "DELETE FROM clients WHERE id = ?";

    private static final String COUNT = "SELECT COUNT(*) FROM clients";

    private static final String EXISTS_PHONE =
            "SELECT COUNT(*) FROM clients WHERE phone = ? AND (? = 0 OR id <> ?)";

    private static final String EXISTS_EMAIL =
            "SELECT COUNT(*) FROM clients WHERE LOWER(email) = LOWER(?) AND (? = 0 OR id <> ?)";

    @Override
    public int save(Client client) {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, client.getFullName());
            statement.setString(2, client.getPhone());
            statement.setString(3, client.getEmail());
            statement.setDate(4, client.getBirthDate() == null ? null : Date.valueOf(client.getBirthDate()));
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt("id");
                    client.setId(id);
                    return id;
                }
            }
            throw new DataAccessException("не удалось получить идентификатор нового клиента");
        } catch (SQLException e) {
            throw new DataAccessException("не удалось сохранить клиента: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Client> findAll() {
        List<Client> clients = new ArrayList<>();
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_ALL);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                clients.add(mapRow(resultSet));
            }
            return clients;
        } catch (SQLException e) {
            throw new DataAccessException("не удалось получить список клиентов: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Client> findById(int id) {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_ID)) {

            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapRow(resultSet)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("не удалось найти клиента по ID: " + e.getMessage(), e);
        }
    }

    /** Поиск клиента по ФИО, телефону или e-mail (подстрока, без учёта регистра). */
    public List<Client> search(String query) {
        String pattern = "%" + query.trim() + "%";
        List<Client> clients = new ArrayList<>();
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(SEARCH)) {

            statement.setString(1, pattern);
            statement.setString(2, pattern);
            statement.setString(3, pattern);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    clients.add(mapRow(resultSet));
                }
            }
            return clients;
        } catch (SQLException e) {
            throw new DataAccessException("ошибка поиска клиентов: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(Client client) {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE)) {

            statement.setString(1, client.getFullName());
            statement.setString(2, client.getPhone());
            statement.setString(3, client.getEmail());
            statement.setDate(4, client.getBirthDate() == null ? null : Date.valueOf(client.getBirthDate()));
            statement.setInt(5, client.getId());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("не удалось обновить клиента: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteById(int id) {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE)) {

            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("не удалось удалить клиента: " + e.getMessage(), e);
        }
    }

    @Override
    public long count() {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(COUNT);
             ResultSet resultSet = statement.executeQuery()) {

            return resultSet.next() ? resultSet.getLong(1) : 0L;
        } catch (SQLException e) {
            throw new DataAccessException("не удалось посчитать клиентов: " + e.getMessage(), e);
        }
    }

    /** Проверка уникальности телефона (excludeId = 0 при создании нового клиента). */
    public boolean existsByPhone(String phone, int excludeId) {
        return existsBy(EXISTS_PHONE, phone, excludeId);
    }

    /** Проверка уникальности e-mail (excludeId = 0 при создании нового клиента). */
    public boolean existsByEmail(String email, int excludeId) {
        return email != null && !email.isBlank() && existsBy(EXISTS_EMAIL, email, excludeId);
    }

    private boolean existsBy(String sql, String value, int excludeId) {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, value);
            statement.setInt(2, excludeId);
            statement.setInt(3, excludeId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getLong(1) > 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("ошибка проверки уникальности: " + e.getMessage(), e);
        }
    }

    @Override
    public String entityName() {
        return "Клиент";
    }

    /** Преобразование строки ResultSet в объект предметной модели. */
    static Client mapRow(ResultSet resultSet) throws SQLException {
        Date birthDate = resultSet.getDate("birth_date");
        Timestamp registeredAt = resultSet.getTimestamp("registered_at");
        return new Client(
                resultSet.getInt("id"),
                resultSet.getString("full_name"),
                resultSet.getString("phone"),
                resultSet.getString("email"),
                birthDate == null ? null : birthDate.toLocalDate(),
                registeredAt == null ? null : registeredAt.toLocalDateTime());
    }
}
