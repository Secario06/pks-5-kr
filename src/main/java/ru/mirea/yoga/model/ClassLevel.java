package ru.mirea.yoga.model;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Уровень сложности занятия в йога-студии.
 */
public enum ClassLevel {

    BEGINNER("Начинающий"),
    INTERMEDIATE("Средний"),
    ADVANCED("Продвинутый");

    private final String title;

    ClassLevel(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    /**
     * Безопасный разбор значения из строки (ввод пользователя, значение из БД).
     *
     * @throws IllegalArgumentException если значение недопустимо
     */
    public static ClassLevel parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Уровень занятия не указан. Допустимо: " + allowed());
        }
        String normalized = raw.trim().toUpperCase();
        return Arrays.stream(values())
                .filter(level -> level.name().equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Недопустимый уровень занятия: '" + raw + "'. Допустимо: " + allowed()));
    }

    public static String allowed() {
        return Arrays.stream(values())
                .map(level -> level.name() + " (" + level.title + ")")
                .collect(Collectors.joining(", "));
    }

    @Override
    public String toString() {
        return name() + " — " + title;
    }
}
