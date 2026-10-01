package ru.mirea.yoga.ui;

import ru.mirea.yoga.model.Enrollment;
import ru.mirea.yoga.model.EnrollmentStatus;
import ru.mirea.yoga.model.YogaClass;
import ru.mirea.yoga.service.EnrollmentService;
import ru.mirea.yoga.service.YogaClassService;

import java.util.List;

/**
 * Раздел меню «Записи на занятия» — работа с основной сущностью системы.
 */
public class EnrollmentMenu {

    static final String HEADER = String.format("%-4s | %-26s | %-22s | %-16s | %-12s | %-16s | %s",
            "ID", "Клиент", "Занятие", "Дата занятия", "Статус", "Создана", "Комментарий");

    private final EnrollmentService enrollmentService;
    private final YogaClassService classService;
    private final ConsoleInput input;

    public EnrollmentMenu(EnrollmentService enrollmentService,
                          YogaClassService classService,
                          ConsoleInput input) {
        this.enrollmentService = enrollmentService;
        this.classService = classService;
        this.input = input;
    }

    public void show() {
        while (true) {
            ConsolePrinter.title("Записи на занятия");
            System.out.println("1. Все записи");
            System.out.println("2. Записать клиента на занятие");
            System.out.println("3. Найти запись по ID");
            System.out.println("4. Изменить запись (перенос / комментарий)");
            System.out.println("5. Изменить статус записи");
            System.out.println("6. Отменить запись");
            System.out.println("7. Удалить запись");
            System.out.println("8. Активные записи на предстоящие занятия");
            System.out.println("9. Сортировка записей");
            System.out.println("0. Назад в главное меню");

            int choice = input.readMenuChoice(0, 9);
            try {
                switch (choice) {
                    case 1 -> printAll();
                    case 2 -> create();
                    case 3 -> findById();
                    case 4 -> update();
                    case 5 -> changeStatus();
                    case 6 -> cancel();
                    case 7 -> delete();
                    case 8 -> ConsolePrinter.printTable("Активные записи", HEADER,
                            enrollmentService.activeUpcoming());
                    case 9 -> sort();
                    case 0 -> {
                        return;
                    }
                    default -> ConsolePrinter.error("Неизвестный пункт меню.");
                }
            } catch (ru.mirea.yoga.exception.AppException e) {
                ConsolePrinter.error(e.getMessage());
            }
            input.pause();
        }
    }

    private void printAll() {
        ConsolePrinter.printTable("Записи на занятия", HEADER, enrollmentService.findAll());
    }

    private void create() {
        ConsolePrinter.section("Новая запись на занятие");

        List<YogaClass> upcoming = classService.upcoming();
        if (upcoming.isEmpty()) {
            ConsolePrinter.error("В расписании нет предстоящих занятий — записывать не на что.");
            return;
        }
        ConsolePrinter.printTable("Доступные занятия", ClassMenu.HEADER, upcoming);

        Integer clientId = input.readIdOrCancel("ID клиента");
        if (clientId == null) {
            return;
        }
        Integer classId = input.readIdOrCancel("ID занятия");
        if (classId == null) {
            return;
        }
        String comment = input.readUntilValid(
                () -> input.readLine("Комментарий (можно пропустить): "),
                EnrollmentService::assertValidComment);

        Enrollment enrollment = enrollmentService.create(clientId, classId, comment);
        ConsolePrinter.success("Запись создана: " + enrollment);
    }

    private void findById() {
        Integer id = input.readIdOrCancel("Введите ID записи");
        if (id == null) {
            return;
        }
        Enrollment enrollment = enrollmentService.getById(id);
        ConsolePrinter.printTable("Запись #" + id, HEADER, List.of(enrollment));
        ConsolePrinter.info("Возможные переходы статуса: "
                + (enrollment.getStatus().isFinal() ? "нет (финальный статус)"
                : enrollment.getStatus().allowedTransitions()));
    }

    private void update() {
        printAll();
        Integer id = input.readIdOrCancel("ID изменяемой записи");
        if (id == null) {
            return;
        }
        Enrollment enrollment = enrollmentService.getById(id);
        ConsolePrinter.info("Текущее занятие: " + enrollment.getClassTitle()
                + " (ID " + enrollment.getClassId() + ")");

        int classId = input.readIntOrKeep("ID нового занятия", enrollment.getClassId());
        String comment = input.readUntilValid(
                () -> input.readLineOrKeep("Комментарий", enrollment.getComment()),
                EnrollmentService::assertValidComment);

        Enrollment updated = enrollmentService.update(id, classId, comment);
        ConsolePrinter.success("Запись обновлена: " + updated);
    }

    private void changeStatus() {
        printAll();
        Integer id = input.readIdOrCancel("ID записи");
        if (id == null) {
            return;
        }
        Enrollment enrollment = enrollmentService.getById(id);
        ConsolePrinter.info("Текущий статус: " + enrollment.getStatus());

        if (enrollment.getStatus().isFinal()) {
            ConsolePrinter.error("Статус " + enrollment.getStatus().getTitle() + " является финальным.");
            return;
        }

        EnrollmentStatus newStatus = input.readEnum("Новый статус:", EnrollmentStatus.values());
        Enrollment updated = enrollmentService.changeStatus(id, newStatus);
        ConsolePrinter.success("Статус изменён: " + updated);
    }

    private void cancel() {
        printAll();
        Integer id = input.readIdOrCancel("ID отменяемой записи");
        if (id == null) {
            return;
        }
        if (!input.confirm("Отменить запись #" + id + "?")) {
            ConsolePrinter.info("Действие отменено.");
            return;
        }
        Enrollment updated = enrollmentService.changeStatus(id, EnrollmentStatus.CANCELLED);
        ConsolePrinter.success("Запись отменена: " + updated);
    }

    private void delete() {
        printAll();
        Integer id = input.readIdOrCancel("ID удаляемой записи");
        if (id == null) {
            return;
        }
        if (!input.confirm("Удалить запись #" + id + " из базы данных?")) {
            ConsolePrinter.info("Удаление отменено.");
            return;
        }
        enrollmentService.delete(id);
        ConsolePrinter.success("Запись удалена.");
    }

    private void sort() {
        ConsolePrinter.section("Сортировка записей");
        System.out.println("1. По дате занятия");
        System.out.println("2. По ФИО клиента");
        System.out.println("3. По статусу");
        System.out.println("0. Назад");

        int choice = input.readMenuChoice(0, 3);
        List<Enrollment> enrollments = enrollmentService.findAll();
        switch (choice) {
            case 1 -> ConsolePrinter.printTable("Записи по дате занятия", HEADER,
                    enrollmentService.sortedByClassDate(enrollments));
            case 2 -> ConsolePrinter.printTable("Записи по клиенту", HEADER,
                    enrollmentService.sortedByClientName(enrollments));
            case 3 -> ConsolePrinter.printTable("Записи по статусу", HEADER,
                    enrollmentService.sortedByStatus(enrollments));
            default -> {
            }
        }
    }
}
