package ru.mirea.yoga.service;

import ru.mirea.yoga.model.ClassLevel;
import ru.mirea.yoga.model.Enrollment;
import ru.mirea.yoga.model.EnrollmentStatus;
import ru.mirea.yoga.model.YogaClass;
import ru.mirea.yoga.repository.ClientRepository;
import ru.mirea.yoga.repository.EnrollmentRepository;
import ru.mirea.yoga.repository.YogaClassRepository;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Сбор статистики по системе.
 * Данные читаются репозиториями, а агрегация выполняется средствами
 * Java Collections Framework и Stream API.
 */
public class StatisticsService {

    private static final int TOP_SIZE = 3;

    private final ClientRepository clientRepository;
    private final YogaClassRepository classRepository;
    private final EnrollmentRepository enrollmentRepository;

    public StatisticsService(ClientRepository clientRepository,
                             YogaClassRepository classRepository,
                             EnrollmentRepository enrollmentRepository) {
        this.clientRepository = clientRepository;
        this.classRepository = classRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    public SystemStatistics collect() {
        long totalClients = clientRepository.count();
        List<YogaClass> classes = classRepository.findAll();
        List<Enrollment> enrollments = enrollmentRepository.findAll();

        Map<EnrollmentStatus, Long> byStatus = enrollmentRepository.countByStatus();

        long activeUpcoming = enrollments.stream()
                .filter(enrollment -> enrollment.getStatus().occupiesSlot())
                .filter(enrollment -> enrollment.getYogaClass() != null && !enrollment.getYogaClass().isStarted())
                .count();

        // Заполняемость: занятые места / вместимость по всем занятиям расписания
        Map<Integer, Long> occupiedByClass = enrollments.stream()
                .filter(enrollment -> enrollment.getStatus().occupiesSlot())
                .collect(Collectors.groupingBy(Enrollment::getClassId, Collectors.counting()));

        int totalCapacity = classes.stream().mapToInt(YogaClass::getCapacity).sum();
        long totalOccupied = occupiedByClass.values().stream().mapToLong(Long::longValue).sum();
        double averageOccupancy = totalCapacity == 0 ? 0.0 : (totalOccupied * 100.0) / totalCapacity;

        // Выручка: сумма цен занятий по фактически посещённым записям
        BigDecimal revenue = enrollments.stream()
                .filter(enrollment -> enrollment.getStatus() == EnrollmentStatus.ATTENDED)
                .map(enrollment -> enrollment.getYogaClass() == null
                        ? BigDecimal.ZERO : enrollment.getYogaClass().getPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long cancelled = byStatus.getOrDefault(EnrollmentStatus.CANCELLED, 0L);
        double cancelRate = enrollments.isEmpty() ? 0.0 : (cancelled * 100.0) / enrollments.size();

        Map<String, Long> topClasses = enrollments.stream()
                .collect(Collectors.groupingBy(Enrollment::getClassTitle, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(TOP_SIZE)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));

        Map<ClassLevel, Long> classesByLevel = classes.stream()
                .collect(Collectors.groupingBy(YogaClass::getLevel, () -> new EnumMap<>(ClassLevel.class),
                        Collectors.counting()));

        String mostActiveClient = enrollments.stream()
                .collect(Collectors.groupingBy(Enrollment::getClientName, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.<String, Long>comparingByValue()
                        .thenComparing(Map.Entry.comparingByKey(Comparator.reverseOrder())))
                .map(entry -> entry.getKey() + " (" + entry.getValue() + " записей)")
                .orElse("нет данных");

        return new SystemStatistics(totalClients, classes.size(), enrollments.size(), byStatus,
                activeUpcoming, averageOccupancy, revenue, cancelRate,
                topClasses, classesByLevel, mostActiveClient);
    }

    /** Неизменяемый контейнер собранных показателей. */
    public record SystemStatistics(long totalClients,
                                   long totalClasses,
                                   long totalEnrollments,
                                   Map<EnrollmentStatus, Long> byStatus,
                                   long activeUpcoming,
                                   double averageOccupancy,
                                   BigDecimal revenue,
                                   double cancelRate,
                                   Map<String, Long> topClasses,
                                   Map<ClassLevel, Long> classesByLevel,
                                   String mostActiveClient) {
    }
}
