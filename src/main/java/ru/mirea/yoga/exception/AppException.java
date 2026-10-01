package ru.mirea.yoga.exception;

/**
 * Базовое собственное исключение приложения.
 * Все прикладные ошибки наследуются от него, что позволяет консольному слою
 * перехватывать их одним catch-блоком и показывать пользователю понятное сообщение.
 */
public class AppException extends RuntimeException {

    public AppException(String message) {
        super(message);
    }

    public AppException(String message, Throwable cause) {
        super(message, cause);
    }
}
