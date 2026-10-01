package ru.mirea.yoga.ui;

import ru.mirea.yoga.service.ClientService;
import ru.mirea.yoga.service.EnrollmentService;
import ru.mirea.yoga.service.YogaClassService;

/**
 * Раздел меню «Поиск». Реализовано четыре способа поиска по разным сущностям.
 */
public class SearchMenu {

    private final ClientService clientService;
    private final YogaClassService classService;
    private final EnrollmentService enrollmentService;
    private final ConsoleInput input;

    public SearchMenu(ClientService clientService,
                      YogaClassService classService,
                      EnrollmentService enrollmentService,
                      ConsoleInput input) {
        this.clientService = clientService;
        this.classService = classService;
        this.enrollmentService = enrollmentService;
        this.input = input;
    }

    public void show() {
        while (true) {
            ConsolePrinter.title("Поиск");
            System.out.println("1. Клиент по ФИО, телефону или e-mail");
            System.out.println("2. Занятие по направлению или инструктору");
            System.out.println("3. Записи по клиенту (ФИО / телефон)");
            System.out.println("4. Записи по занятию или инструктору");
            System.out.println("0. Назад в главное меню");

            int choice = input.readMenuChoice(0, 4);
            if (choice == 0) {
                return;
            }
            try {
                String query = input.readRequiredLine("Введите строку поиска: ");
                switch (choice) {
                    case 1 -> ConsolePrinter.printTable("Клиенты по запросу «" + query + "»",
                            ClientMenu.HEADER, clientService.search(query));
                    case 2 -> ConsolePrinter.printTable("Занятия по запросу «" + query + "»",
                            ClassMenu.HEADER, classService.search(query));
                    case 3 -> ConsolePrinter.printTable("Записи по клиенту «" + query + "»",
                            EnrollmentMenu.HEADER, enrollmentService.searchByClient(query));
                    case 4 -> ConsolePrinter.printTable("Записи по занятию «" + query + "»",
                            EnrollmentMenu.HEADER, enrollmentService.searchByClass(query));
                    default -> ConsolePrinter.error("Неизвестный пункт меню.");
                }
            } catch (ru.mirea.yoga.exception.AppException e) {
                ConsolePrinter.error(e.getMessage());
            }
            input.pause();
        }
    }
}
