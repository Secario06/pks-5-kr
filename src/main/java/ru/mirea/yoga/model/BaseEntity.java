package ru.mirea.yoga.model;

import java.util.Objects;

/**
 * Базовый класс всех сущностей предметной области.
 * Инкапсулирует первичный ключ и общее поведение (сравнение, хеш-код).
 */
public abstract class BaseEntity implements Identifiable {

    private Integer id;

    protected BaseEntity() {
    }

    protected BaseEntity(Integer id) {
        this.id = id;
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    /** Строка для табличного вывода в консоли. Переопределяется наследниками. */
    public abstract String toTableRow();

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BaseEntity other = (BaseEntity) o;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass(), id);
    }
}
