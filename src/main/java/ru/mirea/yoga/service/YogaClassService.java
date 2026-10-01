package ru.mirea.yoga.service;

import ru.mirea.yoga.exception.BusinessRuleException;
import ru.mirea.yoga.exception.EntityNotFoundException;
import ru.mirea.yoga.exception.ValidationException;
import ru.mirea.yoga.model.ClassLevel;
import ru.mirea.yoga.model.YogaClass;
import ru.mirea.yoga.repository.EnrollmentRepository;
import ru.mirea.yoga.repository.YogaClassRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Бизнес-логика расписания занятий йога-студии.
 */
public class YogaClassService {

    private static final int MIN_DURATION = 30;
    private static final int MAX_DURATION = 180;
    private static final int MAX_CAPACITY = 40;
    private static final BigDecimal MAX_PRICE = new BigDecimal("50000");

    private final YogaClassRepository classRepository;
    private final EnrollmentRepository enrollmentRepository;

    public YogaClassService(YogaClassRepository classRepository, EnrollmentRepository enrollmentRepository) {
        this.classRepository = classRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    public YogaClass create(String title, String instructor, ClassLevel level, LocalDateTime startsAt,
                            int durationMinutes, int capacity, BigDecimal price) {
        validate(title, instructor, level, startsAt, durationMinutes, capacity, price);

        // БП-4: один инструктор не может вести два занятия одновременно
        if (classRepository.instructorBusy(instructor.trim(), startsAt, 0)) {
            throw new BusinessRuleException("инструктор " + instructor.trim()
                    + " уже ведёт занятие в " + startsAt.format(YogaClass.DATE_TIME) + ".");
        }

        YogaClass yogaClass = new YogaClass(title.trim(), instructor.trim(), level,
                startsAt, durationMinutes, capacity, price);
        classRepository.save(yogaClass);
        return yogaClass;
    }

    public List<YogaClass> findAll() {
        return classRepository.findAll();
    }

    public YogaClass getById(int id) {
        return classRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Занятие", id));
    }

    /** Поиск занятия по названию направления или имени инструктора. */
    public List<YogaClass> search(String query) {
        if (query == null || query.isBlank()) {
            throw new ValidationException("строка поиска не может быть пустой.");
        }
        return classRepository.search(query);
    }

    /** Фильтр по уровню сложности. */
    public List<YogaClass> filterByLevel(ClassLevel level) {
        return classRepository.findByLevel(level);
    }

    /** Фильтр по диапазону дат. */
    public List<YogaClass> filterByPeriod(LocalDateTime from, LocalDateTime to) {
        if (from.isAfter(to)) {
            throw new ValidationException("начало периода не может быть позже его окончания.");
        }
        return classRepository.findByPeriod(from, to);
    }

    /** Только будущие занятия — то, на что можно записаться (Stream API). */
    public List<YogaClass> upcoming() {
        return classRepository.findAll().stream()
                .filter(yogaClass -> !yogaClass.isStarted())
                .sorted(Comparator.comparing(YogaClass::getStartsAt))
                .toList();
    }

    public YogaClass update(int id, String title, String instructor, ClassLevel level, LocalDateTime startsAt,
                            int durationMinutes, int capacity, BigDecimal price) {
        YogaClass yogaClass = getById(id);
        validate(title, instructor, level, startsAt, durationMinutes, capacity, price);

        if (classRepository.instructorBusy(instructor.trim(), startsAt, id)) {
            throw new BusinessRuleException("инструктор " + instructor.trim()
                    + " уже ведёт занятие в " + startsAt.format(YogaClass.DATE_TIME) + ".");
        }

        // БП-5: вместимость нельзя опустить ниже числа уже занятых мест
        long occupied = enrollmentRepository.countOccupiedSlots(id);
        if (capacity < occupied) {
            throw new BusinessRuleException("на занятие уже записано " + occupied
                    + " человек — вместимость не может быть меньше.");
        }

        yogaClass.setTitle(title.trim());
        yogaClass.setInstructor(instructor.trim());
        yogaClass.setLevel(level);
        yogaClass.setStartsAt(startsAt);
        yogaClass.setDurationMinutes(durationMinutes);
        yogaClass.setCapacity(capacity);
        yogaClass.setPrice(price);

        if (!classRepository.update(yogaClass)) {
            throw new EntityNotFoundException("Занятие", id);
        }
        return yogaClass;
    }

    public void delete(int id) {
        YogaClass yogaClass = getById(id);
        long occupied = enrollmentRepository.countOccupiedSlots(id);
        if (occupied > 0) {
            throw new BusinessRuleException("на занятие «" + yogaClass.getTitle() + "» записано "
                    + occupied + " человек. Удаление запрещено.");
        }
        if (!classRepository.deleteById(id)) {
            throw new EntityNotFoundException("Занятие", id);
        }
    }

    /** Свободные места на занятии. */
    public long freeSlots(YogaClass yogaClass) {
        return yogaClass.getCapacity() - enrollmentRepository.countOccupiedSlots(yogaClass.getId());
    }

    /** Сортировка по дате и времени проведения. */
    public List<YogaClass> sortedByDate(List<YogaClass> classes) {
        return classes.stream()
                .sorted(Comparator.comparing(YogaClass::getStartsAt))
                .toList();
    }

    /** Сортировка по цене (по убыванию), затем по названию. */
    public List<YogaClass> sortedByPrice(List<YogaClass> classes) {
        return classes.stream()
                .sorted(Comparator.comparing(YogaClass::getPrice).reversed()
                        .thenComparing(YogaClass::getTitle))
                .toList();
    }

    /** Сортировка по уровню сложности (порядок констант enum), затем по дате. */
    public List<YogaClass> sortedByLevel(List<YogaClass> classes) {
        return classes.stream()
                .sorted(Comparator.comparing(YogaClass::getLevel)
                        .thenComparing(YogaClass::getStartsAt))
                .toList();
    }

    private void validate(String title, String instructor, ClassLevel level, LocalDateTime startsAt,
                          int durationMinutes, int capacity, BigDecimal price) {
        assertValidTitle(title);
        assertValidInstructor(instructor);
        if (level == null) {
            throw new ValidationException("уровень занятия обязателен. Допустимо: " + ClassLevel.allowed());
        }
        if (startsAt == null) {
            throw new ValidationException("дата и время проведения обязательны.");
        }
        assertValidDuration(durationMinutes);
        assertValidCapacity(capacity);
        assertValidPrice(price);
    }

    /**
     * Проверки отдельных полей вынесены в публичные статические методы, чтобы консольное
     * меню могло переспрашивать конкретное поле сразу при ошибке (см. {@link
     * ru.mirea.yoga.ui.ConsoleInput#readUntilValid}), не заставляя пользователя заново
     * вводить всю форму занятия целиком.
     */
    public static void assertValidTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new ValidationException("название занятия обязательно.");
        }
        if (title.trim().length() > 100) {
            throw new ValidationException("название занятия не может быть длиннее 100 символов.");
        }
    }

    public static void assertValidInstructor(String instructor) {
        if (instructor == null || instructor.isBlank()) {
            throw new ValidationException("имя инструктора обязательно.");
        }
    }

    public static void assertValidDuration(int durationMinutes) {
        if (durationMinutes < MIN_DURATION || durationMinutes > MAX_DURATION) {
            throw new ValidationException("длительность должна быть от " + MIN_DURATION
                    + " до " + MAX_DURATION + " минут.");
        }
    }

    public static void assertValidCapacity(int capacity) {
        if (capacity <= 0 || capacity > MAX_CAPACITY) {
            throw new ValidationException("вместимость должна быть от 1 до " + MAX_CAPACITY + " человек.");
        }
    }

    public static void assertValidPrice(BigDecimal price) {
        if (price == null || price.signum() < 0 || price.compareTo(MAX_PRICE) > 0) {
            throw new ValidationException("цена должна быть в диапазоне от 0 до " + MAX_PRICE + " руб.");
        }
    }
}
