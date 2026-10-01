package ru.mirea.yoga.exception;

/**
 * Ошибка валидации данных объекта (пустое поле, неверный формат, отрицательное число).
 */
public class ValidationException extends AppException {

    public ValidationException(String message) {
        super("Ошибка валидации: " + message);
    }
}
