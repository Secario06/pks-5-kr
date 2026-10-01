package ru.mirea.yoga.util;

import ru.mirea.yoga.exception.DataAccessException;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Управление подключением к PostgreSQL через JDBC.
 * Параметры читаются из src/main/resources/application.properties и могут быть
 * переопределены переменными окружения (DB_URL, DB_USER, DB_PASSWORD).
 */
public final class DatabaseManager {

    private static final String PROPERTIES_FILE = "application.properties";

    private static String url;
    private static String user;
    private static String password;

    static {
        loadSettings();
    }

    private DatabaseManager() {
        // утилитный класс — создание объектов запрещено
    }

    private static void loadSettings() {
        Properties props = new Properties();
        try (InputStream in = DatabaseManager.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            throw new DataAccessException("не удалось прочитать " + PROPERTIES_FILE, e);
        }

        url = resolve("DB_URL", props.getProperty("db.url", "jdbc:postgresql://localhost:5432/yoga_studio"));
        user = resolve("DB_USER", props.getProperty("db.user", "postgres"));
        password = resolve("DB_PASSWORD", props.getProperty("db.password", "postgres"));

        try {
            Class.forName(props.getProperty("db.driver", "org.postgresql.Driver"));
        } catch (ClassNotFoundException e) {
            throw new DataAccessException("JDBC-драйвер PostgreSQL не найден в classpath", e);
        }
    }

    private static String resolve(String envName, String defaultValue) {
        String fromEnv = System.getenv(envName);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }
        String fromSystem = System.getProperty(envName);
        return (fromSystem != null && !fromSystem.isBlank()) ? fromSystem : defaultValue;
    }

    /**
     * Новое подключение к БД. Вызывающий код обязан использовать try-with-resources.
     *
     * @throws DataAccessException если подключение установить не удалось
     */
    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            throw new DataAccessException(
                    "не удалось подключиться к " + url + " (пользователь " + user + "). "
                            + "Проверьте, что сервер PostgreSQL запущен и база создана. "
                            + "Причина: " + e.getMessage(), e);
        }
    }

    /** Проверка доступности БД при старте приложения. */
    public static void checkConnection() {
        try (Connection connection = getConnection()) {
            if (connection == null || connection.isClosed()) {
                throw new DataAccessException("подключение не установлено");
            }
        } catch (SQLException e) {
            throw new DataAccessException("проверка подключения завершилась ошибкой", e);
        }
    }

    public static String getUrl() {
        return url;
    }

    public static String getUser() {
        return user;
    }
}
