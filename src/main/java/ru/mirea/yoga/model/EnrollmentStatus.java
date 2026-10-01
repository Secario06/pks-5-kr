package ru.mirea.yoga.model;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Статус записи клиента на занятие — основное перечисление предметной модели.
 * Дополнительно инкапсулирует машину состояний: какие переходы между статусами разрешены.
 */
public enum EnrollmentStatus {

    CREATED("Создана"),
    CONFIRMED("Подтверждена"),
    ATTENDED("Посещено"),
    CANCELLED("Отменена"),
    NO_SHOW("Неявка");

    /** Разрешённые переходы. Пустое множество означает терминальный статус. */
    private static final Map<EnrollmentStatus, Set<EnrollmentStatus>> TRANSITIONS = Map.of(
            CREATED, EnumSet.of(CONFIRMED, CANCELLED),
            CONFIRMED, EnumSet.of(ATTENDED, NO_SHOW, CANCELLED),
            ATTENDED, EnumSet.noneOf(EnrollmentStatus.class),
            CANCELLED, EnumSet.noneOf(EnrollmentStatus.class),
            NO_SHOW, EnumSet.noneOf(EnrollmentStatus.class));

    private final String title;

    EnrollmentStatus(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    /** Запись занимает место на занятии (учитывается при проверке вместимости). */
    public boolean occupiesSlot() {
        return this == CREATED || this == CONFIRMED || this == ATTENDED;
    }

    /** Статус, из которого переходы невозможны. */
    public boolean isFinal() {
        return TRANSITIONS.get(this).isEmpty();
    }

    public boolean canChangeTo(EnrollmentStatus target) {
        return target != null && TRANSITIONS.get(this).contains(target);
    }

    public Set<EnrollmentStatus> allowedTransitions() {
        return TRANSITIONS.get(this);
    }

    public static EnrollmentStatus parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Статус записи не указан. Допустимо: " + allowed());
        }
        String normalized = raw.trim().toUpperCase();
        return Arrays.stream(values())
                .filter(status -> status.name().equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Недопустимый статус записи: '" + raw + "'. Допустимо: " + allowed()));
    }

    public static String allowed() {
        return Arrays.stream(values())
                .map(status -> status.name() + " (" + status.title + ")")
                .collect(Collectors.joining(", "));
    }

    @Override
    public String toString() {
        return name() + " — " + title;
    }
}
