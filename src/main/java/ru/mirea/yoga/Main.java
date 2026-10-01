package ru.mirea.yoga;

import ru.mirea.yoga.exception.AppException;
import ru.mirea.yoga.ui.ConsoleApp;

/**
 * КР 1. Консольная информационная система «Йога-студия: запись на занятие».
 * Архитектура: Console UI → Service → Repository (JDBC) → PostgreSQL.
 * <p>
 * Класс Main отвечает только за запуск приложения и обработку фатальных ошибок.
 */
public class Main {

    public static void main(String[] args) {
        // Apache POI использует Log4j2; без этой настройки в консоль печатается
        // предупреждение об отсутствии реализации логирования.
        System.setProperty("log4j2.loggerContextFactory",
                "org.apache.logging.log4j.simple.SimpleLoggerContextFactory");
        System.setProperty("org.apache.logging.log4j.simplelog.StatusLogger.level", "OFF");

        try {
            new ConsoleApp().run();
        } catch (AppException e) {
            System.out.println();
            System.out.println("Приложение остановлено: " + e.getMessage());
            System.exit(1);
        } catch (RuntimeException e) {
            System.out.println();
            System.out.println("Критическая ошибка: " + e);
            System.exit(2);
        }
    }
}
