package ru.mirea.yoga.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Занятие в йога-студии (расписание): направление, инструктор, время, вместимость.
 */
public class YogaClass extends BaseEntity {

    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private String title;
    private String instructor;
    private ClassLevel level;
    private LocalDateTime startsAt;
    private int durationMinutes;
    private int capacity;
    private BigDecimal price;

    public YogaClass(String title, String instructor, ClassLevel level,
                     LocalDateTime startsAt, int durationMinutes, int capacity, BigDecimal price) {
        this.title = title;
        this.instructor = instructor;
        this.level = level;
        this.startsAt = startsAt;
        this.durationMinutes = durationMinutes;
        this.capacity = capacity;
        this.price = price;
    }

    public YogaClass(Integer id, String title, String instructor, ClassLevel level,
                     LocalDateTime startsAt, int durationMinutes, int capacity, BigDecimal price) {
        super(id);
        this.title = title;
        this.instructor = instructor;
        this.level = level;
        this.startsAt = startsAt;
        this.durationMinutes = durationMinutes;
        this.capacity = capacity;
        this.price = price;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getInstructor() {
        return instructor;
    }

    public void setInstructor(String instructor) {
        this.instructor = instructor;
    }

    public ClassLevel getLevel() {
        return level;
    }

    public void setLevel(ClassLevel level) {
        this.level = level;
    }

    public LocalDateTime getStartsAt() {
        return startsAt;
    }

    public void setStartsAt(LocalDateTime startsAt) {
        this.startsAt = startsAt;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public LocalDateTime getEndsAt() {
        return startsAt == null ? null : startsAt.plusMinutes(durationMinutes);
    }

    /** Занятие уже началось — записаться на него нельзя. */
    public boolean isStarted() {
        return startsAt != null && startsAt.isBefore(LocalDateTime.now());
    }

    @Override
    public String getDisplayName() {
        return title + " / " + instructor + " / " + (startsAt == null ? "-" : startsAt.format(DATE_TIME));
    }

    @Override
    public String toTableRow() {
        return String.format("%-4s | %-22s | %-18s | %-12s | %-16s | %3d мин | %2d мест | %8.2f",
                getId(),
                title,
                instructor,
                level == null ? "-" : level.name(),
                startsAt == null ? "-" : startsAt.format(DATE_TIME),
                durationMinutes,
                capacity,
                price == null ? BigDecimal.ZERO : price);
    }

    @Override
    public String toString() {
        return "Занятие #" + getId() + ": " + getDisplayName();
    }
}
