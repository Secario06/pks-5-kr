package ru.mirea.yoga.ui;

import ru.mirea.yoga.model.ClassLevel;
import ru.mirea.yoga.model.EnrollmentStatus;
import ru.mirea.yoga.service.EnrollmentService;
import ru.mirea.yoga.service.YogaClassService;

import java.time.LocalDateTime;

/**
 * Раздел меню «Фильтрация». Реализовано шесть фильтров по разным критериям.
 */
public class FilterMenu {

    private final EnrollmentService enrollmentService;
    private final YogaClassService classService;
    private final ConsoleInput input;

    public FilterMenu(EnrollmentService enrollmentService, YogaClassService classService, ConsoleInput input) {
        this.enrollmentService = enrollmentService;
        this.classService = classService;
        this.input = input;
    }

    public void show() {
        while (true) {
            ConsolePrinter.title("Фильтрация");
            System.out.println("1. Записи по статусу");
            System.out.println("2. Записи по уровню занятия");
            System.out.println("3. Записи по клиенту");
            System.out.println("4. Записи за период");
            System.out.println("5. Занятия по уровню сложности");
            System.out.println("6. Занятия за период");
            System.out.println("0. Назад в главное меню");

            int choice = input.readMenuChoice(0, 6);
            if (choice == 0) {
                return;
            }
            try {
                switch (choice) {
                    case 1 -> byStatus();
                    case 2 -> byEnrollmentLevel();
                    case 3 -> byClient();
                    case 4 -> byEnrollmentPeriod();
                    case 5 -> byClassLevel();
                    case 6 -> byClassPeriod();
                    default -> ConsolePrinter.error("Неизвестный пункт меню.");
                }
            } catch (ru.mirea.yoga.exception.AppException e) {
                ConsolePrinter.error(e.getMessage());
            }
            input.pause();
        }
    }

    private void byStatus() {
        EnrollmentStatus status = input.readEnum("Статус записи:", EnrollmentStatus.values());
        ConsolePrinter.printTable("Записи со статусом " + status.getTitle(),
                EnrollmentMenu.HEADER, enrollmentService.filterByStatus(status));
    }

    private void byEnrollmentLevel() {
        ClassLevel level = input.readEnum("Уровень занятия:", ClassLevel.values());
        ConsolePrinter.printTable("Записи на занятия уровня " + level.getTitle(),
                EnrollmentMenu.HEADER, enrollmentService.filterByLevel(level));
    }

    private void byClient() {
        Integer clientId = input.readIdOrCancel("ID клиента");
        if (clientId == null) {
            return;
        }
        ConsolePrinter.printTable("Записи клиента #" + clientId,
                EnrollmentMenu.HEADER, enrollmentService.filterByClient(clientId));
    }

    private void byEnrollmentPeriod() {
        LocalDateTime from = input.readDateTime("Начало периода");
        LocalDateTime to = input.readDateTime("Конец периода");
        ConsolePrinter.printTable("Записи за период", EnrollmentMenu.HEADER,
                enrollmentService.filterByPeriod(from, to));
    }

    private void byClassLevel() {
        ClassLevel level = input.readEnum("Уровень сложности:", ClassLevel.values());
        ConsolePrinter.printTable("Занятия уровня " + level.getTitle(),
                ClassMenu.HEADER, classService.filterByLevel(level));
    }

    private void byClassPeriod() {
        LocalDateTime from = input.readDateTime("Начало периода");
        LocalDateTime to = input.readDateTime("Конец периода");
        ConsolePrinter.printTable("Занятия за период", ClassMenu.HEADER, classService.filterByPeriod(from, to));
    }
}
