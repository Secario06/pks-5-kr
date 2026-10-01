package ru.mirea.yoga.ui;

import ru.mirea.yoga.model.Client;
import ru.mirea.yoga.service.ClientService;

import java.time.LocalDate;
import java.util.List;

/**
 * Раздел меню «Клиенты»: CRUD, поиск и сортировка клиентов студии.
 * Слой UI не содержит SQL и бизнес-проверок — только диалог с пользователем.
 */
public class ClientMenu {

    static final String HEADER = String.format("%-4s | %-28s | %-16s | %-26s | %-10s | %3s",
            "ID", "ФИО", "Телефон", "E-mail", "Рождение", "Возр");

    private final ClientService clientService;
    private final ConsoleInput input;

    public ClientMenu(ClientService clientService, ConsoleInput input) {
        this.clientService = clientService;
        this.input = input;
    }

    public void show() {
        while (true) {
            ConsolePrinter.title("Клиенты студии");
            System.out.println("1. Список всех клиентов");
            System.out.println("2. Добавить клиента");
            System.out.println("3. Найти клиента по ID");
            System.out.println("4. Изменить данные клиента");
            System.out.println("5. Удалить клиента");
            System.out.println("6. Поиск клиентов (ФИО / телефон / e-mail)");
            System.out.println("7. Сортировка клиентов");
            System.out.println("0. Назад в главное меню");

            int choice = input.readMenuChoice(0, 7);
            try {
                switch (choice) {
                    case 1 -> printAll();
                    case 2 -> create();
                    case 3 -> findById();
                    case 4 -> update();
                    case 5 -> delete();
                    case 6 -> search();
                    case 7 -> sort();
                    case 0 -> {
                        return;
                    }
                    default -> ConsolePrinter.error("Неизвестный пункт меню.");
                }
            } catch (ru.mirea.yoga.exception.AppException e) {
                ConsolePrinter.error(e.getMessage());
            }
            input.pause();
        }
    }

    private void printAll() {
        ConsolePrinter.printTable("Клиенты", HEADER, clientService.findAll());
    }

    private void create() {
        ConsolePrinter.section("Новый клиент");
        String fullName = input.readUntilValid(
                () -> input.readRequiredLine("ФИО: "),
                ClientService::assertValidFullName);
        String phone = input.readUntilValid(
                () -> ClientService.normalizePhone(input.readRequiredLine("Телефон (+7XXXXXXXXXX): ")),
                ClientService::assertValidPhone);
        String email = input.readUntilValid(
                () -> input.readLine("E-mail (можно пропустить): "),
                ClientService::assertValidEmail);
        LocalDate birthDate = input.readUntilValid(
                () -> input.readDate("Дата рождения"),
                ClientService::assertValidAge);

        Client client = clientService.create(fullName, phone, email, birthDate);
        ConsolePrinter.success("Клиент добавлен: " + client);
    }

    private void findById() {
        Integer id = input.readIdOrCancel("Введите ID клиента");
        if (id == null) {
            return;
        }
        Client client = clientService.getById(id);
        ConsolePrinter.printTable("Клиент #" + id, HEADER, List.of(client));
        ConsolePrinter.info("Зарегистрирован: "
                + (client.getRegisteredAt() == null ? "-" : client.getRegisteredAt().format(ConsoleInput.DATE_TIME)));
    }

    private void update() {
        printAll();
        Integer id = input.readIdOrCancel("ID изменяемого клиента");
        if (id == null) {
            return;
        }
        Client client = clientService.getById(id);
        ConsolePrinter.info("Пустой ввод оставляет прежнее значение.");

        String fullName = input.readUntilValid(
                () -> input.readLineOrKeep("ФИО", client.getFullName()),
                ClientService::assertValidFullName);
        String phone = input.readUntilValid(
                () -> ClientService.normalizePhone(input.readLineOrKeep("Телефон", client.getPhone())),
                ClientService::assertValidPhone);
        String email = input.readUntilValid(
                () -> input.readLineOrKeep("E-mail", client.getEmail()),
                ClientService::assertValidEmail);
        LocalDate birthDate = input.readUntilValid(
                () -> input.readDateOrKeep("Дата рождения", client.getBirthDate()),
                ClientService::assertValidAge);

        Client updated = clientService.update(id, fullName, phone, email, birthDate);
        ConsolePrinter.success("Данные обновлены: " + updated);
    }

    private void delete() {
        printAll();
        Integer id = input.readIdOrCancel("ID удаляемого клиента");
        if (id == null) {
            return;
        }
        Client client = clientService.getById(id);
        if (!input.confirm("Удалить клиента «" + client.getFullName() + "»?")) {
            ConsolePrinter.info("Удаление отменено.");
            return;
        }
        clientService.delete(id);
        ConsolePrinter.success("Клиент удалён.");
    }

    private void search() {
        String query = input.readRequiredLine("Строка поиска: ");
        ConsolePrinter.printTable("Результаты поиска по «" + query + "»", HEADER, clientService.search(query));
    }

    private void sort() {
        ConsolePrinter.section("Сортировка клиентов");
        System.out.println("1. По ФИО (А-Я)");
        System.out.println("2. По возрасту (от старших)");
        System.out.println("3. По дате регистрации (сначала новые)");
        System.out.println("0. Назад");

        int choice = input.readMenuChoice(0, 3);
        List<Client> clients = clientService.findAll();
        switch (choice) {
            case 1 -> ConsolePrinter.printTable("Клиенты по ФИО", HEADER, clientService.sortedByName(clients));
            case 2 -> ConsolePrinter.printTable("Клиенты по возрасту", HEADER, clientService.sortedByAge(clients));
            case 3 -> ConsolePrinter.printTable("Клиенты по дате регистрации", HEADER,
                    clientService.sortedByRegistration(clients));
            default -> {
            }
        }
    }
}
