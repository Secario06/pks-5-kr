package ru.mirea.yoga.ui;

import ru.mirea.yoga.export.DataExporter;

import java.nio.file.Path;
import java.util.List;

/**
 * Раздел меню «Экспорт данных».
 * Список экспортёров хранится как {@code List<DataExporter>} — конкретная реализация
 * (Excel или CSV) выбирается во время выполнения, вызов метода одинаков (полиморфизм).
 */
public class ExportMenu {

    private static final String DEFAULT_DIRECTORY = "export";

    private final List<DataExporter> exporters;
    private final ConsoleInput input;

    public ExportMenu(List<DataExporter> exporters, ConsoleInput input) {
        this.exporters = exporters;
        this.input = input;
    }

    public void show() {
        ConsolePrinter.title("Экспорт данных");
        for (int i = 0; i < exporters.size(); i++) {
            System.out.println((i + 1) + ". " + exporters.get(i).formatName());
        }
        System.out.println("0. Назад в главное меню");

        int choice = input.readMenuChoice(0, exporters.size());
        if (choice == 0) {
            return;
        }

        DataExporter exporter = exporters.get(choice - 1);
        String directory = input.readLine("Каталог для файлов [" + DEFAULT_DIRECTORY + "]: ");
        Path target = Path.of(directory.isEmpty() ? DEFAULT_DIRECTORY : directory);

        try {
            List<Path> files = exporter.export(target);
            ConsolePrinter.success("Экспорт в формате " + exporter.formatName() + " выполнен.");
            for (Path file : files) {
                System.out.println("   " + file.toAbsolutePath());
            }
        } catch (ru.mirea.yoga.exception.AppException e) {
            ConsolePrinter.error(e.getMessage());
        }
        input.pause();
    }
}
