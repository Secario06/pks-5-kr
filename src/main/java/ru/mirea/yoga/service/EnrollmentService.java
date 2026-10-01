package ru.mirea.yoga.service;

import ru.mirea.yoga.exception.BusinessRuleException;
import ru.mirea.yoga.exception.EntityNotFoundException;
import ru.mirea.yoga.exception.ValidationException;
import ru.mirea.yoga.model.ClassLevel;
import ru.mirea.yoga.model.Client;
import ru.mirea.yoga.model.Enrollment;
import ru.mirea.yoga.model.EnrollmentStatus;
import ru.mirea.yoga.model.YogaClass;
import ru.mirea.yoga.repository.ClientRepository;
import ru.mirea.yoga.repository.EnrollmentRepository;
import ru.mirea.yoga.repository.YogaClassRepository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Бизнес-логика записи клиентов на занятия — центральный сервис системы.
 * Реализует бизнес-правила предметной области: существование связанных объектов,
 * запрет повторной записи, контроль вместимости и корректность переходов статуса.
 */
public class EnrollmentService {

    private static final int MAX_COMMENT_LENGTH = 255;

    private final EnrollmentRepository enrollmentRepository;
    private final ClientRepository clientRepository;
    private final YogaClassRepository classRepository;

    public EnrollmentService(EnrollmentRepository enrollmentRepository,
                             ClientRepository clientRepository,
                             YogaClassRepository classRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.clientRepository = clientRepository;
        this.classRepository = classRepository;
    }

    /**
     * Создание записи на занятие.
     * <p>БП-1: клиент должен существовать.
     * <p>БП-2: занятие должно существовать.
     * <p>БП-3: нельзя записаться на занятие, которое уже началось.
     * <p>БП-4: клиент не может записаться на одно занятие дважды.
     * <p>БП-5: нельзя превысить вместимость зала.
     */
    public Enrollment create(int clientId, int classId, String comment) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new EntityNotFoundException("Клиент", clientId));           // БП-1

        YogaClass yogaClass = classRepository.findById(classId)
                .orElseThrow(() -> new EntityNotFoundException("Занятие", classId));            // БП-2

        if (yogaClass.isStarted()) {                                                            // БП-3
            throw new BusinessRuleException("занятие «" + yogaClass.getTitle() + "» уже началось ("
                    + yogaClass.getStartsAt().format(YogaClass.DATE_TIME) + "). Запись невозможна.");
        }

        if (enrollmentRepository.existsForClientAndClass(clientId, classId, 0)) {               // БП-4
            throw new BusinessRuleException("клиент «" + client.getFullName()
                    + "» уже записан на занятие «" + yogaClass.getTitle() + "».");
        }

        long occupied = enrollmentRepository.countOccupiedSlots(classId);
        if (occupied >= yogaClass.getCapacity()) {                                              // БП-5
            throw new BusinessRuleException("на занятии «" + yogaClass.getTitle() + "» нет свободных мест ("
                    + occupied + " из " + yogaClass.getCapacity() + ").");
        }

        assertValidComment(comment);

        Enrollment enrollment = new Enrollment(clientId, classId, EnrollmentStatus.CREATED, trim(comment));
        enrollment.setClient(client);
        enrollment.setYogaClass(yogaClass);
        enrollmentRepository.save(enrollment);
        return enrollment;
    }

    public List<Enrollment> findAll() {
        return enrollmentRepository.findAll();
    }

    public Enrollment getById(int id) {
        return enrollmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Запись на занятие", id));
    }

    /** Поиск записей по ФИО или телефону клиента. */
    public List<Enrollment> searchByClient(String query) {
        requireQuery(query);
        return enrollmentRepository.searchByClient(query);
    }

    /** Поиск записей по названию занятия или имени инструктора. */
    public List<Enrollment> searchByClass(String query) {
        requireQuery(query);
        return enrollmentRepository.searchByClass(query);
    }

    /** Фильтр по статусу записи. */
    public List<Enrollment> filterByStatus(EnrollmentStatus status) {
        return enrollmentRepository.findByStatus(status);
    }

    /** Фильтр по уровню сложности занятия. */
    public List<Enrollment> filterByLevel(ClassLevel level) {
        return enrollmentRepository.findByLevel(level);
    }

    /** Фильтр по клиенту. */
    public List<Enrollment> filterByClient(int clientId) {
        clientRepository.findById(clientId)
                .orElseThrow(() -> new EntityNotFoundException("Клиент", clientId));
        return enrollmentRepository.findByClientId(clientId);
    }

    /** Фильтр по диапазону дат проведения занятия. */
    public List<Enrollment> filterByPeriod(LocalDateTime from, LocalDateTime to) {
        if (from.isAfter(to)) {
            throw new ValidationException("начало периода не может быть позже его окончания.");
        }
        return enrollmentRepository.findByPeriod(from, to);
    }

    /** Активные записи на предстоящие занятия (демонстрация Stream API). */
    public List<Enrollment> activeUpcoming() {
        return enrollmentRepository.findAll().stream()
                .filter(enrollment -> enrollment.getStatus().occupiesSlot())
                .filter(enrollment -> enrollment.getYogaClass() != null && !enrollment.getYogaClass().isStarted())
                .sorted(Comparator.comparing(Enrollment::getClassStartsAt))
                .toList();
    }

    /**
     * Изменение записи: перенос на другое занятие и/или комментарий.
     * <p>БП-6: изменять запись в терминальном статусе (посещено/отменена/неявка) нельзя.
     */
    public Enrollment update(int id, int newClassId, String comment) {
        Enrollment enrollment = getById(id);

        if (enrollment.getStatus().isFinal()) {                                                 // БП-6
            throw new BusinessRuleException("запись в статусе " + enrollment.getStatus().getTitle()
                    + " изменять нельзя.");
        }

        YogaClass target = classRepository.findById(newClassId)
                .orElseThrow(() -> new EntityNotFoundException("Занятие", newClassId));

        if (newClassId != enrollment.getClassId()) {
            if (target.isStarted()) {
                throw new BusinessRuleException("нельзя перенести запись на уже начавшееся занятие.");
            }
            if (enrollmentRepository.existsForClientAndClass(enrollment.getClientId(), newClassId, id)) {
                throw new BusinessRuleException("клиент уже записан на занятие «" + target.getTitle() + "».");
            }
            if (enrollmentRepository.countOccupiedSlots(newClassId) >= target.getCapacity()) {
                throw new BusinessRuleException("на занятии «" + target.getTitle() + "» нет свободных мест.");
            }
        }

        assertValidComment(comment);

        enrollment.setClassId(newClassId);
        enrollment.setYogaClass(target);
        enrollment.setComment(trim(comment));

        if (!enrollmentRepository.update(enrollment)) {
            throw new EntityNotFoundException("Запись на занятие", id);
        }
        return enrollment;
    }

    /**
     * Смена статуса записи.
     * <p>БП-7: разрешены только переходы, описанные в {@link EnrollmentStatus}.
     * <p>БП-8: отметить посещение можно только после начала занятия.
     */
    public Enrollment changeStatus(int id, EnrollmentStatus newStatus) {
        Enrollment enrollment = getById(id);
        EnrollmentStatus current = enrollment.getStatus();

        if (current == newStatus) {
            throw new BusinessRuleException("запись уже находится в статусе " + newStatus.getTitle() + ".");
        }
        if (!current.canChangeTo(newStatus)) {                                                  // БП-7
            String allowed = current.allowedTransitions().isEmpty()
                    ? "переходы невозможны (статус финальный)"
                    : current.allowedTransitions().toString();
            throw new BusinessRuleException("переход " + current.name() + " → " + newStatus.name()
                    + " запрещён. Доступно: " + allowed + ".");
        }

        YogaClass yogaClass = enrollment.getYogaClass();
        boolean attendanceStatus = newStatus == EnrollmentStatus.ATTENDED || newStatus == EnrollmentStatus.NO_SHOW;
        if (attendanceStatus && yogaClass != null && !yogaClass.isStarted()) {                  // БП-8
            throw new BusinessRuleException("отметить посещение можно только после начала занятия ("
                    + yogaClass.getStartsAt().format(YogaClass.DATE_TIME) + ").");
        }

        enrollment.setStatus(newStatus);
        if (!enrollmentRepository.update(enrollment)) {
            throw new EntityNotFoundException("Запись на занятие", id);
        }
        return enrollment;
    }

    public void delete(int id) {
        getById(id);
        if (!enrollmentRepository.deleteById(id)) {
            throw new EntityNotFoundException("Запись на занятие", id);
        }
    }

    /** Сортировка по дате занятия. */
    public List<Enrollment> sortedByClassDate(List<Enrollment> enrollments) {
        return enrollments.stream()
                .sorted(Comparator.comparing(Enrollment::getClassStartsAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    /** Сортировка по ФИО клиента. */
    public List<Enrollment> sortedByClientName(List<Enrollment> enrollments) {
        return enrollments.stream()
                .sorted(Comparator.comparing(Enrollment::getClientName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /** Сортировка по статусу (в порядке жизненного цикла), затем по дате занятия. */
    public List<Enrollment> sortedByStatus(List<Enrollment> enrollments) {
        return enrollments.stream()
                .sorted(Comparator.comparing(Enrollment::getStatus)
                        .thenComparing(Enrollment::getClassStartsAt,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private void requireQuery(String query) {
        if (query == null || query.isBlank()) {
            throw new ValidationException("строка поиска не может быть пустой.");
        }
    }

    /**
     * Проверка комментария вынесена в публичный статический метод, чтобы консольное меню
     * могло переспрашивать поле сразу при ошибке (см. {@link
     * ru.mirea.yoga.ui.ConsoleInput#readUntilValid}).
     */
    public static void assertValidComment(String comment) {
        if (comment != null && comment.length() > MAX_COMMENT_LENGTH) {
            throw new ValidationException("комментарий не может быть длиннее "
                    + MAX_COMMENT_LENGTH + " символов.");
        }
    }

    private String trim(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
