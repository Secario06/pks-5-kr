package ru.mirea.yoga.ui;

import ru.mirea.yoga.model.ClassLevel;
import ru.mirea.yoga.model.EnrollmentStatus;
import ru.mirea.yoga.service.StatisticsService;

import java.util.Map;

/**
 * Раздел меню «Статистика»: сводные показатели работы студии.
 */
public class StatisticsMenu {

    private final StatisticsService statisticsService;
    private final ConsoleInput input;

    public StatisticsMenu(StatisticsService statisticsService, ConsoleInput input) {
        this.statisticsService = statisticsService;
        this.input = input;
    }

    public void show() {
        ConsolePrinter.title("Статистика йога-студии");
        try {
            StatisticsService.SystemStatistics stats = statisticsService.collect();

            System.out.printf("Всего клиентов:                        %d%n", stats.totalClients());
            System.out.printf("Всего занятий в расписании:            %d%n", stats.totalClasses());
            System.out.printf("Всего записей на занятия:              %d%n", stats.totalEnrollments());
            System.out.printf("Активных записей (будущие занятия):    %d%n", stats.activeUpcoming());
            System.out.printf("Средняя заполняемость занятий:         %.1f %%%n", stats.averageOccupancy());
            System.out.printf("Доля отменённых записей:               %.1f %%%n", stats.cancelRate());
            System.out.printf("Выручка по посещённым занятиям:        %s руб.%n", stats.revenue().toPlainString());
            System.out.printf("Самый активный клиент:                 %s%n", stats.mostActiveClient());

            ConsolePrinter.section("Записи по статусам");
            for (Map.Entry<EnrollmentStatus, Long> entry : stats.byStatus().entrySet()) {
                System.out.printf("  %-12s %-16s %d%n",
                        entry.getKey().name(), entry.getKey().getTitle(), entry.getValue());
            }

            ConsolePrinter.section("Занятия по уровням сложности");
            for (ClassLevel level : ClassLevel.values()) {
                System.out.printf("  %-14s %-16s %d%n",
                        level.name(), level.getTitle(), stats.classesByLevel().getOrDefault(level, 0L));
            }

            ConsolePrinter.section("Топ-3 популярных направления");
            if (stats.topClasses().isEmpty()) {
                System.out.println("  нет данных");
            } else {
                int position = 1;
                for (Map.Entry<String, Long> entry : stats.topClasses().entrySet()) {
                    System.out.printf("  %d. %-28s %d записей%n", position++, entry.getKey(), entry.getValue());
                }
            }
        } catch (ru.mirea.yoga.exception.AppException e) {
            ConsolePrinter.error(e.getMessage());
        }
        input.pause();
    }
}
