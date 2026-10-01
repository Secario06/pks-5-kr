package ru.mirea.yoga.ui;

import ru.mirea.yoga.model.BaseEntity;
import ru.mirea.yoga.repository.DatabaseInspectRepository.TableDump;

import java.util.List;

/**
 * Форматированный вывод в консоль.
 * Метод {@link #printTable(String, String, List)} принимает любые наследники
 * {@link BaseEntity} и вызывает их реализацию toTableRow() — это полиморфизм в действии.
 */
public final class ConsolePrinter {

    private static final String LINE = "=".repeat(110);
    private static final String THIN = "-".repeat(110);

    private ConsolePrinter() {
    }

    public static void title(String text) {
        System.out.println();
        System.out.println(LINE);
        System.out.println("  " + text.toUpperCase());
        System.out.println(LINE);
    }

    public static void section(String text) {
        System.out.println();
        System.out.println(text);
        System.out.println(THIN);
    }

    public static void success(String text) {
        System.out.println("[OK] " + text);
    }

    public static void error(String text) {
        System.out.println("[ОШИБКА] " + text);
    }

    public static void info(String text) {
        System.out.println(text);
    }

    /** Универсальный вывод списка сущностей в виде таблицы. */
    public static void printTable(String caption, String header, List<? extends BaseEntity> entities) {
        section(caption + " (найдено: " + entities.size() + ")");
        if (entities.isEmpty()) {
            System.out.println("Записей не найдено.");
            return;
        }
        System.out.println(header);
        System.out.println(THIN);
        for (BaseEntity entity : entities) {
            System.out.println(entity.toTableRow());
        }
    }

    /** Вывод содержимого таблицы БД (пункт меню «Вывести таблицы базы данных»). */
    public static void printDump(TableDump dump) {
        section("Таблица " + dump.tableName() + " (строк: " + dump.rows().size() + ")");
        System.out.println(String.join(" | ", dump.headers()));
        System.out.println(THIN);
        for (List<String> row : dump.rows()) {
            System.out.println(String.join(" | ", row));
        }
    }
}
