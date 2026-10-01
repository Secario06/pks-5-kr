package ru.mirea.yoga.exception;

/**
 * Ошибка работы с базой данных: нет подключения, некорректный SQL-запрос,
 * нарушение ограничений целостности. Оборачивает проверяемое java.sql.SQLException.
 */
public class DataAccessException extends AppException {

    public DataAccessException(String message, Throwable cause) {
        super("Ошибка базы данных: " + message, cause);
    }

    public DataAccessException(String message) {
        super("Ошибка базы данных: " + message);
    }
}
