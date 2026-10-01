package ru.mirea.yoga.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Клиент йога-студии — участник предметной области, на которого оформляются записи.
 * Все поля private (инкапсуляция), доступ — только через геттеры/сеттеры.
 */
public class Client extends BaseEntity {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private String fullName;
    private String phone;
    private String email;
    private LocalDate birthDate;
    private LocalDateTime registeredAt;

    /** Конструктор для создания новой записи (id назначает БД). */
    public Client(String fullName, String phone, String email, LocalDate birthDate) {
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.birthDate = birthDate;
    }

    /** Конструктор для восстановления объекта из ResultSet. */
    public Client(Integer id, String fullName, String phone, String email,
                  LocalDate birthDate, LocalDateTime registeredAt) {
        super(id);
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.birthDate = birthDate;
        this.registeredAt = registeredAt;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(LocalDateTime registeredAt) {
        this.registeredAt = registeredAt;
    }

    /** Возраст клиента в полных годах (вычисляемое свойство, в БД не хранится). */
    public int getAge() {
        if (birthDate == null) {
            return 0;
        }
        return java.time.Period.between(birthDate, LocalDate.now()).getYears();
    }

    @Override
    public String getDisplayName() {
        return fullName + " (" + phone + ")";
    }

    @Override
    public String toTableRow() {
        return String.format("%-4s | %-28s | %-16s | %-26s | %-10s | %3d",
                getId(),
                fullName,
                phone,
                email == null ? "-" : email,
                birthDate == null ? "-" : birthDate.format(DATE),
                getAge());
    }

    @Override
    public String toString() {
        return "Клиент #" + getId() + ": " + getDisplayName();
    }
}
