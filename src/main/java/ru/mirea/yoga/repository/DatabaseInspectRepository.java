package ru.mirea.yoga.repository;

import ru.mirea.yoga.exception.DataAccessException;
import ru.mirea.yoga.util.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Служебный репозиторий для пункта меню «Вывести таблицы базы данных».
 * Читает список таблиц из information_schema и выгружает содержимое любой таблицы
 * в универсальном виде через ResultSetMetaData.
 */
public class DatabaseInspectRepository {

    private static final String TABLES =
            "SELECT table_name FROM information_schema.tables "
                    + "WHERE table_schema = 'public' AND table_type = 'BASE TABLE' ORDER BY table_name";

    /** Разрешённые для просмотра таблицы. Защита от подстановки произвольного имени в SQL. */
    private static final List<String> ALLOWED = List.of("clients", "yoga_classes", "enrollments");

    public List<String> listTables() {
        List<String> tables = new ArrayList<>();
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(TABLES);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                tables.add(resultSet.getString("table_name"));
            }
            return tables;
        } catch (SQLException e) {
            throw new DataAccessException("не удалось получить список таблиц: " + e.getMessage(), e);
        }
    }

    /**
     * Содержимое таблицы: заголовки колонок и строки значений.
     * Имя таблицы нельзя передать через PreparedStatement, поэтому оно проверяется
     * по «белому списку» — так исключается SQL-инъекция.
     */
    public TableDump dump(String tableName) {
        String normalized = tableName == null ? "" : tableName.trim().toLowerCase();
        if (!ALLOWED.contains(normalized)) {
            throw new DataAccessException("таблица '" + tableName + "' недоступна для просмотра. "
                    + "Доступны: " + String.join(", ", ALLOWED));
        }

        String sql = "SELECT * FROM " + normalized + " ORDER BY id";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            ResultSetMetaData meta = resultSet.getMetaData();
            int columnCount = meta.getColumnCount();

            List<String> headers = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                headers.add(meta.getColumnLabel(i));
            }

            List<List<String>> rows = new ArrayList<>();
            while (resultSet.next()) {
                List<String> row = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    Object value = resultSet.getObject(i);
                    row.add(value == null ? "NULL" : String.valueOf(value));
                }
                rows.add(row);
            }
            return new TableDump(normalized, headers, rows);
        } catch (SQLException e) {
            throw new DataAccessException("не удалось прочитать таблицу " + normalized + ": " + e.getMessage(), e);
        }
    }

    /** Содержимое всех таблиц системы. */
    public Map<String, TableDump> dumpAll() {
        Map<String, TableDump> result = new LinkedHashMap<>();
        for (String table : ALLOWED) {
            result.put(table, dump(table));
        }
        return result;
    }

    /** Простой контейнер результата выгрузки таблицы. */
    public record TableDump(String tableName, List<String> headers, List<List<String>> rows) {
    }
}
