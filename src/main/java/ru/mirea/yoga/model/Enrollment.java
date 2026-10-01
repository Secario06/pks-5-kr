package ru.mirea.yoga.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Запись клиента на занятие — основная сущность варианта «Йога-студия».
 * Связывает {@link Client} и {@link YogaClass}, хранит статус жизненного цикла.
 */
public class Enrollment extends BaseEntity {

    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private Integer clientId;
    private Integer classId;
    private EnrollmentStatus status;
    private LocalDateTime createdAt;
    private String comment;

    /** Связанные объекты подгружаются репозиторием через JOIN (могут быть null). */
    private Client client;
    private YogaClass yogaClass;

    public Enrollment(Integer clientId, Integer classId, EnrollmentStatus status, String comment) {
        this.clientId = clientId;
        this.classId = classId;
        this.status = status;
        this.comment = comment;
    }

    public Enrollment(Integer id, Integer clientId, Integer classId, EnrollmentStatus status,
                      LocalDateTime createdAt, String comment) {
        super(id);
        this.clientId = clientId;
        this.classId = classId;
        this.status = status;
        this.createdAt = createdAt;
        this.comment = comment;
    }

    public Integer getClientId() {
        return clientId;
    }

    public void setClientId(Integer clientId) {
        this.clientId = clientId;
    }

    public Integer getClassId() {
        return classId;
    }

    public void setClassId(Integer classId) {
        this.classId = classId;
    }

    public EnrollmentStatus getStatus() {
        return status;
    }

    public void setStatus(EnrollmentStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
        if (client != null) {
            this.clientId = client.getId();
        }
    }

    public YogaClass getYogaClass() {
        return yogaClass;
    }

    public void setYogaClass(YogaClass yogaClass) {
        this.yogaClass = yogaClass;
        if (yogaClass != null) {
            this.classId = yogaClass.getId();
        }
    }

    public String getClientName() {
        return client == null ? "клиент #" + clientId : client.getFullName();
    }

    public String getClassTitle() {
        return yogaClass == null ? "занятие #" + classId : yogaClass.getTitle();
    }

    public LocalDateTime getClassStartsAt() {
        return yogaClass == null ? null : yogaClass.getStartsAt();
    }

    @Override
    public String getDisplayName() {
        return getClientName() + " → " + getClassTitle();
    }

    @Override
    public String toTableRow() {
        return String.format("%-4s | %-26s | %-22s | %-16s | %-12s | %-16s | %s",
                getId(),
                getClientName(),
                getClassTitle(),
                getClassStartsAt() == null ? "-" : getClassStartsAt().format(DATE_TIME),
                status == null ? "-" : status.name(),
                createdAt == null ? "-" : createdAt.format(DATE_TIME),
                comment == null || comment.isBlank() ? "-" : comment);
    }

    @Override
    public String toString() {
        return "Запись #" + getId() + ": " + getDisplayName() + " [" + status + "]";
    }
}
