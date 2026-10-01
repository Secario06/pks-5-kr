package ru.mirea.yoga.exception;

/**
 * Нарушение бизнес-правила предметной области
 * (превышена вместимость, недопустимый переход статуса, повторная запись и т. п.).
 */
public class BusinessRuleException extends AppException {

    public BusinessRuleException(String message) {
        super("Нарушение бизнес-правила: " + message);
    }
}
