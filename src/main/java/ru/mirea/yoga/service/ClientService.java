package ru.mirea.yoga.service;

import ru.mirea.yoga.exception.BusinessRuleException;
import ru.mirea.yoga.exception.EntityNotFoundException;
import ru.mirea.yoga.exception.ValidationException;
import ru.mirea.yoga.model.Client;
import ru.mirea.yoga.repository.ClientRepository;
import ru.mirea.yoga.repository.EnrollmentRepository;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Бизнес-логика работы с клиентами студии.
 * Здесь сосредоточены проверки и правила; SQL находится только в репозитории.
 */
public class ClientService {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+7\\d{10}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern NAME_PATTERN = Pattern.compile("^[А-Яа-яЁёA-Za-z\\s-]{3,100}$");

    private static final int MIN_AGE = 14;
    private static final int MAX_AGE = 100;

    private final ClientRepository clientRepository;
    private final EnrollmentRepository enrollmentRepository;

    public ClientService(ClientRepository clientRepository, EnrollmentRepository enrollmentRepository) {
        this.clientRepository = clientRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    /** Создание клиента с полной проверкой данных. */
    public Client create(String fullName, String phone, String email, LocalDate birthDate) {
        String normalizedPhone = normalizePhone(phone);
        validate(fullName, normalizedPhone, email, birthDate);

        // БП-1: телефон клиента уникален в пределах студии
        if (clientRepository.existsByPhone(normalizedPhone, 0)) {
            throw new BusinessRuleException("клиент с телефоном " + normalizedPhone + " уже зарегистрирован.");
        }
        // БП-2: e-mail, если указан, тоже уникален
        if (clientRepository.existsByEmail(email, 0)) {
            throw new BusinessRuleException("клиент с e-mail " + email + " уже зарегистрирован.");
        }

        Client client = new Client(fullName.trim(), normalizedPhone, normalizeEmail(email), birthDate);
        clientRepository.save(client);
        return client;
    }

    public List<Client> findAll() {
        return clientRepository.findAll();
    }

    /** Получение клиента по ID; если записи нет — собственное исключение. */
    public Client getById(int id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Клиент", id));
    }

    /** Поиск клиента по ФИО, телефону или e-mail. */
    public List<Client> search(String query) {
        if (query == null || query.isBlank()) {
            throw new ValidationException("строка поиска не может быть пустой.");
        }
        return clientRepository.search(query);
    }

    /** Изменение данных клиента. */
    public Client update(int id, String fullName, String phone, String email, LocalDate birthDate) {
        Client client = getById(id);
        String normalizedPhone = normalizePhone(phone);
        validate(fullName, normalizedPhone, email, birthDate);

        if (clientRepository.existsByPhone(normalizedPhone, id)) {
            throw new BusinessRuleException("телефон " + normalizedPhone + " занят другим клиентом.");
        }
        if (clientRepository.existsByEmail(email, id)) {
            throw new BusinessRuleException("e-mail " + email + " занят другим клиентом.");
        }

        client.setFullName(fullName.trim());
        client.setPhone(normalizedPhone);
        client.setEmail(normalizeEmail(email));
        client.setBirthDate(birthDate);

        if (!clientRepository.update(client)) {
            throw new EntityNotFoundException("Клиент", id);
        }
        return client;
    }

    /** Удаление клиента. */
    public void delete(int id) {
        Client client = getById(id);
        // БП-3: нельзя удалить клиента, у которого есть активные записи на занятия
        long active = enrollmentRepository.countActiveByClientId(id);
        if (active > 0) {
            throw new BusinessRuleException("у клиента «" + client.getFullName() + "» есть " + active
                    + " активных записей. Сначала отмените их.");
        }
        if (!clientRepository.deleteById(id)) {
            throw new EntityNotFoundException("Клиент", id);
        }
    }

    /** Сортировка по ФИО (Java Collections Framework + Comparator). */
    public List<Client> sortedByName(List<Client> clients) {
        return clients.stream()
                .sorted(Comparator.comparing(Client::getFullName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /** Сортировка по возрасту (от старших к младшим). */
    public List<Client> sortedByAge(List<Client> clients) {
        return clients.stream()
                .sorted(Comparator.comparingInt(Client::getAge).reversed()
                        .thenComparing(Client::getFullName))
                .toList();
    }

    /** Сортировка по дате регистрации (сначала новые). */
    public List<Client> sortedByRegistration(List<Client> clients) {
        return clients.stream()
                .sorted(Comparator.comparing(Client::getRegisteredAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    /** Проверка данных клиента — общая для создания и изменения. */
    private void validate(String fullName, String phone, String email, LocalDate birthDate) {
        assertValidFullName(fullName);
        assertValidPhone(phone);
        assertValidEmail(email);
        assertValidAge(birthDate);
    }

    /**
     * Проверки отдельных полей вынесены в публичные статические методы, чтобы консольное
     * меню могло переспрашивать конкретное поле сразу при ошибке (см. {@link
     * ru.mirea.yoga.ui.ConsoleInput#readUntilValid}), не заставляя пользователя заново
     * вводить всю форму клиента целиком.
     */
    public static void assertValidFullName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            throw new ValidationException("ФИО клиента обязательно для заполнения.");
        }
        if (!NAME_PATTERN.matcher(fullName.trim()).matches()) {
            throw new ValidationException("ФИО должно содержать от 3 до 100 букв, пробелов или дефисов.");
        }
    }

    /** Проверяет уже нормализованный телефон (см. {@link #normalizePhone}). */
    public static void assertValidPhone(String phone) {
        if (phone == null || !PHONE_PATTERN.matcher(phone).matches()) {
            throw new ValidationException("телефон должен быть в формате +7XXXXXXXXXX.");
        }
    }

    public static void assertValidEmail(String email) {
        if (email != null && !email.isBlank() && !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new ValidationException("некорректный формат e-mail: " + email);
        }
    }

    public static void assertValidAge(LocalDate birthDate) {
        if (birthDate == null) {
            throw new ValidationException("дата рождения обязательна.");
        }
        if (birthDate.isAfter(LocalDate.now())) {
            throw new ValidationException("дата рождения не может быть в будущем.");
        }
        int age = java.time.Period.between(birthDate, LocalDate.now()).getYears();
        if (age < MIN_AGE || age > MAX_AGE) {
            throw new ValidationException("возраст клиента должен быть от " + MIN_AGE + " до " + MAX_AGE
                    + " лет (получено: " + age + ").");
        }
    }

    /** Приведение телефона к единому формату +7XXXXXXXXXX. */
    public static String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        String digits = phone.replaceAll("[^\\d+]", "");
        if (digits.startsWith("8") && digits.length() == 11) {
            return "+7" + digits.substring(1);
        }
        if (digits.startsWith("7") && digits.length() == 11) {
            return "+" + digits;
        }
        return digits;
    }

    private String normalizeEmail(String email) {
        return (email == null || email.isBlank()) ? null : email.trim().toLowerCase();
    }
}
