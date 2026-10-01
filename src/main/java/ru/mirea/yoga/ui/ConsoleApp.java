package ru.mirea.yoga.ui;

import ru.mirea.yoga.exception.AppException;
import ru.mirea.yoga.export.CsvExporter;
import ru.mirea.yoga.export.DataExporter;
import ru.mirea.yoga.export.ExcelExporter;
import ru.mirea.yoga.repository.ClientRepository;
import ru.mirea.yoga.repository.DatabaseInspectRepository;
import ru.mirea.yoga.repository.EnrollmentRepository;
import ru.mirea.yoga.repository.YogaClassRepository;
import ru.mirea.yoga.service.ClientService;
import ru.mirea.yoga.service.EnrollmentService;
import ru.mirea.yoga.service.StatisticsService;
import ru.mirea.yoga.service.YogaClassService;
import ru.mirea.yoga.util.DatabaseManager;

import java.util.List;
import java.util.Scanner;

/**
 * Точка сборки приложения и главное меню.
 * Здесь создаются объекты всех слоёв (репозитории → сервисы → меню) и связываются между собой.
 */
public class ConsoleApp {

    private final ConsoleInput input;
    private final ClientMenu clientMenu;
    private final ClassMenu classMenu;
    private final EnrollmentMenu enrollmentMenu;
    private final SearchMenu searchMenu;
    private final FilterMenu filterMenu;
    private final StatisticsMenu statisticsMenu;
    private final ExportMenu exportMenu;
    private final DatabaseMenu databaseMenu;

    public ConsoleApp() {
        this.input = new ConsoleInput(new Scanner(System.in));

        // Слой доступа к данным
        ClientRepository clientRepository = new ClientRepository();
        YogaClassRepository classRepository = new YogaClassRepository();
        EnrollmentRepository enrollmentRepository = new EnrollmentRepository();
        DatabaseInspectRepository inspectRepository = new DatabaseInspectRepository();

        // Слой бизнес-логики
        ClientService clientService = new ClientService(clientRepository, enrollmentRepository);
        YogaClassService classService = new YogaClassService(classRepository, enrollmentRepository);
        EnrollmentService enrollmentService =
                new EnrollmentService(enrollmentRepository, clientRepository, classRepository);
        StatisticsService statisticsService =
                new StatisticsService(clientRepository, classRepository, enrollmentRepository);

        // Экспортёры хранятся через интерфейс — это позволяет добавлять форматы без правки меню
        List<DataExporter> exporters = List.of(
                new ExcelExporter(clientRepository, classRepository, enrollmentRepository, statisticsService),
                new CsvExporter(clientRepository, classRepository, enrollmentRepository));

        // Слой пользовательского интерфейса
        this.clientMenu = new ClientMenu(clientService, input);
        this.classMenu = new ClassMenu(classService, input);
        this.enrollmentMenu = new EnrollmentMenu(enrollmentService, classService, input);
        this.searchMenu = new SearchMenu(clientService, classService, enrollmentService, input);
        this.filterMenu = new FilterMenu(enrollmentService, classService, input);
        this.statisticsMenu = new StatisticsMenu(statisticsService, input);
        this.exportMenu = new ExportMenu(exporters, input);
        this.databaseMenu = new DatabaseMenu(inspectRepository, input);
    }

    /** Главный цикл приложения: программа не завершается после одного действия. */
    public void run() {
        checkDatabase();

        while (true) {
            printMainMenu();
            int choice = input.readMenuChoice(0, 8);
            try {
                switch (choice) {
                    case 1 -> clientMenu.show();
                    case 2 -> classMenu.show();
                    case 3 -> enrollmentMenu.show();
                    case 4 -> searchMenu.show();
                    case 5 -> filterMenu.show();
                    case 6 -> statisticsMenu.show();
                    case 7 -> exportMenu.show();
                    case 8 -> databaseMenu.show();
                    case 0 -> {
                        ConsolePrinter.info("\nРабота завершена. До свидания!");
                        return;
                    }
                    default -> ConsolePrinter.error("Неизвестный пункт меню.");
                }
            } catch (AppException e) {
                // Прикладные ошибки перехватываются здесь: приложение продолжает работу
                ConsolePrinter.error(e.getMessage());
                input.pause();
            } catch (RuntimeException e) {
                ConsolePrinter.error("Непредвиденная ошибка: " + e);
                input.pause();
            }
        }
    }

    private void printMainMenu() {
        ConsolePrinter.title("Информационная система «Йога-студия»");
        System.out.println("1. Клиенты");
        System.out.println("2. Расписание занятий");
        System.out.println("3. Записи на занятия");
        System.out.println("4. Поиск");
        System.out.println("5. Фильтрация");
        System.out.println("6. Статистика");
        System.out.println("7. Экспорт данных");
        System.out.println("8. Вывести таблицы базы данных");
        System.out.println("0. Выход");
        System.out.println();
    }

    private void checkDatabase() {
        try {
            DatabaseManager.checkConnection();
            ConsolePrinter.success("Подключение к базе данных установлено: " + DatabaseManager.getUrl());
        } catch (AppException e) {
            ConsolePrinter.error(e.getMessage());
            ConsolePrinter.info("Проверьте настройки в src/main/resources/application.properties "
                    + "и выполните скрипт database/schema.sql.");
            throw e;
        }
    }
}
