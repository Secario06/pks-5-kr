package ru.mirea.yoga.repository;

import ru.mirea.yoga.model.Identifiable;

import java.util.List;
import java.util.Optional;

/**
 * Общий интерфейс репозитория (слой доступа к данным).
 * Все конкретные репозитории реализуют его — это даёт единообразие и полиморфизм:
 * с любым репозиторием можно работать через ссылку типа {@code CrudRepository<?>}.
 *
 * @param <T> тип сущности предметной модели
 */
public interface CrudRepository<T extends Identifiable> {

    /** Сохранить новую запись и вернуть сгенерированный БД идентификатор. */
    int save(T entity);

    /** Все записи таблицы. */
    List<T> findAll();

    /** Поиск записи по первичному ключу. */
    Optional<T> findById(int id);

    /** Обновить существующую запись. Возвращает true, если строка была изменена. */
    boolean update(T entity);

    /** Удалить запись по первичному ключу. Возвращает true, если строка была удалена. */
    boolean deleteById(int id);

    /** Количество записей в таблице. */
    long count();

    /** Имя сущности для сообщений об ошибках (используется полиморфно). */
    String entityName();
}
