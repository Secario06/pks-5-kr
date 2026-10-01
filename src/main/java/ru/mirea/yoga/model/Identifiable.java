package ru.mirea.yoga.model;

/**
 * Интерфейс сущности, обладающей первичным ключом.
 * Позволяет работать со всеми сущностями системы полиморфно
 * (см. {@link ru.mirea.yoga.repository.CrudRepository}).
 */
public interface Identifiable {

    Integer getId();

    void setId(Integer id);

    /** Краткое человекочитаемое имя записи (для меню и сообщений об ошибках). */
    String getDisplayName();
}
