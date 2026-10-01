package ru.mirea.yoga.ui;

import ru.mirea.yoga.repository.DatabaseInspectRepository;
import ru.mirea.yoga.repository.DatabaseInspectRepository.TableDump;
import ru.mirea.yoga.util.DatabaseManager;

import java.util.List;
import java.util.Map;

/**
 * Раздел меню «Вывести таблицы базы данных»:
 * список таблиц схемы public и построчная выгрузка их содержимого.
 */
public class DatabaseMenu {

    private final DatabaseInspectRepository inspectRepository;
    private final ConsoleInput input;

    public DatabaseMenu(DatabaseInspectRepository inspectRepository, ConsoleInput input) {
        this.inspectRepository = inspectRepository;
        this.input = input;
    }

    public void show() {
        while (true) {
            ConsolePrinter.title("Таблицы базы данных");
            ConsolePrinter.info("Подключение: " + DatabaseManager.getUrl()
                    + " (пользователь " + DatabaseManager.getUser() + ")");
            System.out.println();
            System.out.println("1. Список таблиц схемы public");
            System.out.println("2. Вывести все таблицы с данными");
            System.out.println("3. Вывести одну таблицу");
            System.out.println("0. Назад в главное меню");

            int choice = input.readMenuChoice(0, 3);
            if (choice == 0) {
                return;
            }
            try {
                switch (choice) {
                    case 1 -> printTableList();
                    case 2 -> printAll();
                    case 3 -> printOne();
                    default -> ConsolePrinter.error("Неизвестный пункт меню.");
                }
            } catch (ru.mirea.yoga.exception.AppException e) {
                ConsolePrinter.error(e.getMessage());
            }
            input.pause();
        }
    }

    private void printTableList() {
        List<String> tables = inspectRepository.listTables();
        ConsolePrinter.section("Таблицы (всего: " + tables.size() + ")");
        tables.forEach(table -> System.out.println("  - " + table));
    }

    private void printAll() {
        Map<String, TableDump> dumps = inspectRepository.dumpAll();
        dumps.values().forEach(ConsolePrinter::printDump);
    }

    private void printOne() {
        printTableList();
        String table = input.readRequiredLine("Название таблицы: ");
        ConsolePrinter.printDump(inspectRepository.dump(table));
    }
}
