package ru.mirea.yoga.exception;

/**
 * Запись с указанным идентификатором не найдена в базе данных.
 */
public class EntityNotFoundException extends AppException {

    public EntityNotFoundException(String entityName, Integer id) {
        super(entityName + " с ID = " + id + " не найден(а).");
    }

    public EntityNotFoundException(String message) {
        super(message);
    }
}
