package ru.mirea.yoga.ui;

import ru.mirea.yoga.model.ClassLevel;
import ru.mirea.yoga.model.YogaClass;
import ru.mirea.yoga.service.YogaClassService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Раздел меню «Расписание занятий»: CRUD, поиск, фильтрация и сортировка занятий.
 */
public class ClassMenu {

    static final String HEADER = String.format("%-4s | %-22s | %-18s | %-12s | %-16s | %7s | %6s | %8s",
            "ID", "Направление", "Инструктор", "Уровень", "Начало", "Длит.", "Мест", "Цена");

    private final YogaClassService classService;
    private final ConsoleInput input;

    public ClassMenu(YogaClassService classService, ConsoleInput input) {
        this.classService = classService;
        this.input = input;
    }

    public void show() {
        while (true) {
            ConsolePrinter.title("Расписание занятий");
            System.out.println("1. Всё расписание");
            System.out.println("2. Только предстоящие занятия");
            System.out.println("3. Добавить занятие");
            System.out.println("4. Найти занятие по ID (со свободными местами)");
            System.out.println("5. Изменить занятие");
            System.out.println("6. Удалить занятие");
            System.out.println("7. Поиск занятий (направление / инструктор)");
            System.out.println("8. Сортировка занятий");
            System.out.println("0. Назад в главное меню");

            int choice = input.readMenuChoice(0, 8);
            try {
                switch (choice) {
                    case 1 -> printAll();
                    case 2 -> ConsolePrinter.printTable("Предстоящие занятия", HEADER, classService.upcoming());
                    case 3 -> create();
                    case 4 -> findById();
                    case 5 -> update();
                    case 6 -> delete();
                    case 7 -> search();
                    case 8 -> sort();
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
        ConsolePrinter.printTable("Занятия", HEADER, classService.findAll());
    }

    private void create() {
        ConsolePrinter.section("Новое занятие");
        String title = input.readUntilValid(
                () -> input.readRequiredLine("Направление (например, Хатха-йога): "),
                YogaClassService::assertValidTitle);
        String instructor = input.readUntilValid(
                () -> input.readRequiredLine("Инструктор: "),
                YogaClassService::assertValidInstructor);
        ClassLevel level = input.readEnum("Уровень сложности:", ClassLevel.values());
        LocalDateTime startsAt = input.readDateTime("Дата и время начала");
        int duration = input.readUntilValid(
                () -> input.readInt("Длительность, мин: "),
                YogaClassService::assertValidDuration);
        int capacity = input.readUntilValid(
                () -> input.readInt("Вместимость, человек: "),
                YogaClassService::assertValidCapacity);
        BigDecimal price = input.readUntilValid(
                () -> input.readDecimal("Цена, руб: "),
                YogaClassService::assertValidPrice);

        YogaClass yogaClass = classService.create(title, instructor, level, startsAt, duration, capacity, price);
        ConsolePrinter.success("Занятие добавлено: " + yogaClass);
    }

    private void findById() {
        Integer id = input.readIdOrCancel("Введите ID занятия");
        if (id == null) {
            return;
        }
        YogaClass yogaClass = classService.getById(id);
        ConsolePrinter.printTable("Занятие #" + id, HEADER, List.of(yogaClass));
        ConsolePrinter.info("Свободных мест: " + classService.freeSlots(yogaClass) + " из " + yogaClass.getCapacity());
        ConsolePrinter.info("Статус: " + (yogaClass.isStarted() ? "занятие уже прошло/идёт" : "запись открыта"));
    }

    private void update() {
        printAll();
        Integer id = input.readIdOrCancel("ID изменяемого занятия");
        if (id == null) {
            return;
        }
        YogaClass yogaClass = classService.getById(id);
        ConsolePrinter.info("Пустой ввод оставляет прежнее значение.");

        String title = input.readUntilValid(
                () -> input.readLineOrKeep("Направление", yogaClass.getTitle()),
                YogaClassService::assertValidTitle);
        String instructor = input.readUntilValid(
                () -> input.readLineOrKeep("Инструктор", yogaClass.getInstructor()),
                YogaClassService::assertValidInstructor);
        ClassLevel level = input.confirm("Изменить уровень (сейчас " + yogaClass.getLevel().getTitle() + ")?")
                ? input.readEnum("Уровень сложности:", ClassLevel.values())
                : yogaClass.getLevel();
        LocalDateTime startsAt = input.readDateTimeOrKeep("Начало", yogaClass.getStartsAt());
        int duration = input.readUntilValid(
                () -> input.readIntOrKeep("Длительность, мин", yogaClass.getDurationMinutes()),
                YogaClassService::assertValidDuration);
        int capacity = input.readUntilValid(
                () -> input.readIntOrKeep("Вместимость", yogaClass.getCapacity()),
                YogaClassService::assertValidCapacity);
        BigDecimal price = input.readUntilValid(
                () -> input.readDecimalOrKeep("Цена, руб", yogaClass.getPrice()),
                YogaClassService::assertValidPrice);

        YogaClass updated = classService.update(id, title, instructor, level, startsAt, duration, capacity, price);
        ConsolePrinter.success("Занятие обновлено: " + updated);
    }

    private void delete() {
        printAll();
        Integer id = input.readIdOrCancel("ID удаляемого занятия");
        if (id == null) {
            return;
        }
        YogaClass yogaClass = classService.getById(id);
        if (!input.confirm("Удалить занятие «" + yogaClass.getTitle() + "»?")) {
            ConsolePrinter.info("Удаление отменено.");
            return;
        }
        classService.delete(id);
        ConsolePrinter.success("Занятие удалено.");
    }

    private void search() {
        String query = input.readRequiredLine("Строка поиска: ");
        ConsolePrinter.printTable("Результаты поиска по «" + query + "»", HEADER, classService.search(query));
    }

    private void sort() {
        ConsolePrinter.section("Сортировка занятий");
        System.out.println("1. По дате проведения");
        System.out.println("2. По цене (по убыванию)");
        System.out.println("3. По уровню сложности");
        System.out.println("0. Назад");

        int choice = input.readMenuChoice(0, 3);
        List<YogaClass> classes = classService.findAll();
        switch (choice) {
            case 1 -> ConsolePrinter.printTable("Занятия по дате", HEADER, classService.sortedByDate(classes));
            case 2 -> ConsolePrinter.printTable("Занятия по цене", HEADER, classService.sortedByPrice(classes));
            case 3 -> ConsolePrinter.printTable("Занятия по уровню", HEADER, classService.sortedByLevel(classes));
            default -> {
            }
        }
    }
}
