package ru.mirea.yoga.ui;

import ru.mirea.yoga.exception.AppException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Безопасное чтение данных из консоли.
 * Любой некорректный ввод обрабатывается здесь: программа не завершается аварийно,
 * а повторно запрашивает значение с понятным сообщением об ошибке.
 */
public class ConsoleInput {

    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private static final String CANCEL = "0";

    private final Scanner scanner;

    public ConsoleInput(Scanner scanner) {
        this.scanner = scanner;
    }

    /** Чтение строки без проверок. */
    public String readLine(String prompt) {
        System.out.print(prompt);
        try {
            return scanner.nextLine().trim();
        } catch (NoSuchElementException e) {
            // поток ввода закрыт (например, Ctrl+D) — считаем это выходом
            return CANCEL;
        }
    }

    /** Чтение непустой строки. */
    public String readRequiredLine(String prompt) {
        while (true) {
            String value = readLine(prompt);
            if (!value.isEmpty()) {
                return value;
            }
            System.out.println("Ошибка: значение не может быть пустым.");
        }
    }

    /** Чтение строки с сохранением прежнего значения при пустом вводе (режим редактирования). */
    public String readLineOrKeep(String prompt, String currentValue) {
        String value = readLine(prompt + " [" + (currentValue == null ? "-" : currentValue) + "]: ");
        return value.isEmpty() ? currentValue : value;
    }

    /** Чтение целого числа с повтором при ошибке. */
    public int readInt(String prompt) {
        while (true) {
            String value = readLine(prompt);
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                System.out.println("Ошибка: требуется целое число (получено: '" + value + "').");
            }
        }
    }

    /** Чтение целого числа в заданном диапазоне (для пунктов меню). */
    public int readMenuChoice(int min, int max) {
        while (true) {
            int value = readInt("Выберите действие: ");
            if (value >= min && value <= max) {
                return value;
            }
            System.out.println("Ошибка: выберите пункт меню от " + min + " до " + max + ".");
        }
    }

    /** Чтение ID. Возвращает null, если пользователь ввёл 0 (отмена). */
    public Integer readIdOrCancel(String prompt) {
        while (true) {
            String value = readLine(prompt + " (0 — отмена): ");
            if (value.isEmpty() || CANCEL.equals(value)) {
                return null;
            }
            try {
                int id = Integer.parseInt(value);
                if (id <= 0) {
                    System.out.println("Ошибка: ID должен быть положительным числом.");
                    continue;
                }
                return id;
            } catch (NumberFormatException e) {
                System.out.println("Ошибка: ID должен быть целым числом (получено: '" + value + "').");
            }
        }
    }

    /** Чтение целого числа с значением по умолчанию (пустой ввод — оставить как есть). */
    public int readIntOrKeep(String prompt, int currentValue) {
        while (true) {
            String value = readLine(prompt + " [" + currentValue + "]: ");
            if (value.isEmpty()) {
                return currentValue;
            }
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                System.out.println("Ошибка: требуется целое число (получено: '" + value + "').");
            }
        }
    }

    /** Чтение денежной суммы. */
    public BigDecimal readDecimal(String prompt) {
        while (true) {
            String value = readLine(prompt).replace(',', '.');
            try {
                return new BigDecimal(value);
            } catch (NumberFormatException e) {
                System.out.println("Ошибка: требуется число, например 1200.50 (получено: '" + value + "').");
            }
        }
    }

    public BigDecimal readDecimalOrKeep(String prompt, BigDecimal currentValue) {
        while (true) {
            String value = readLine(prompt + " [" + currentValue + "]: ").replace(',', '.');
            if (value.isEmpty()) {
                return currentValue;
            }
            try {
                return new BigDecimal(value);
            } catch (NumberFormatException e) {
                System.out.println("Ошибка: требуется число, например 1200.50 (получено: '" + value + "').");
            }
        }
    }

    /** Чтение даты в формате дд.мм.гггг. */
    public LocalDate readDate(String prompt) {
        while (true) {
            String value = readLine(prompt + " (дд.мм.гггг): ");
            try {
                return LocalDate.parse(value, DATE);
            } catch (DateTimeParseException e) {
                System.out.println("Ошибка: дата должна быть в формате дд.мм.гггг, например 15.03.1998.");
            }
        }
    }

    public LocalDate readDateOrKeep(String prompt, LocalDate currentValue) {
        while (true) {
            String shown = currentValue == null ? "-" : currentValue.format(DATE);
            String value = readLine(prompt + " (дд.мм.гггг) [" + shown + "]: ");
            if (value.isEmpty()) {
                return currentValue;
            }
            try {
                return LocalDate.parse(value, DATE);
            } catch (DateTimeParseException e) {
                System.out.println("Ошибка: дата должна быть в формате дд.мм.гггг.");
            }
        }
    }

    /** Чтение даты и времени в формате дд.мм.гггг чч:мм. */
    public LocalDateTime readDateTime(String prompt) {
        while (true) {
            String value = readLine(prompt + " (дд.мм.гггг чч:мм): ");
            try {
                return LocalDateTime.parse(value, DATE_TIME);
            } catch (DateTimeParseException e) {
                System.out.println("Ошибка: требуется формат дд.мм.гггг чч:мм, например 20.10.2026 18:30.");
            }
        }
    }

    public LocalDateTime readDateTimeOrKeep(String prompt, LocalDateTime currentValue) {
        while (true) {
            String shown = currentValue == null ? "-" : currentValue.format(DATE_TIME);
            String value = readLine(prompt + " (дд.мм.гггг чч:мм) [" + shown + "]: ");
            if (value.isEmpty()) {
                return currentValue;
            }
            try {
                return LocalDateTime.parse(value, DATE_TIME);
            } catch (DateTimeParseException e) {
                System.out.println("Ошибка: требуется формат дд.мм.гггг чч:мм.");
            }
        }
    }

    /**
     * Чтение значения перечисления по номеру из списка.
     * Демонстрирует безопасную работу с enum: недопустимое значение выбрать нельзя.
     */
    public <E extends Enum<E>> E readEnum(String title, E[] values) {
        System.out.println(title);
        for (int i = 0; i < values.length; i++) {
            System.out.println("  " + (i + 1) + ". " + values[i]);
        }
        while (true) {
            int choice = readInt("Номер значения: ");
            if (choice >= 1 && choice <= values.length) {
                return values[choice - 1];
            }
            System.out.println("Ошибка: допустимы значения от 1 до " + values.length + ".");
        }
    }

    /** Запрос подтверждения действия. */
    public boolean confirm(String prompt) {
        while (true) {
            String value = readLine(prompt + " (да/нет): ").toLowerCase();
            if (value.equals("да") || value.equals("д") || value.equals("y") || value.equals("yes")) {
                return true;
            }
            if (value.equals("нет") || value.equals("н") || value.equals("n") || value.equals("no")) {
                return false;
            }
            System.out.println("Ошибка: введите «да» или «нет».");
        }
    }

    public void pause() {
        readLine("\nНажмите Enter для продолжения...");
    }

    /**
     * Читает значение через {@code reader} и сразу проверяет его через {@code validator}.
     * Если проверка не проходит (validator бросает {@link AppException}, обычно
     * ValidationException/BusinessRuleException из сервисного слоя), пользователю
     * показывается причина и поле запрашивается заново — остальные уже введённые
     * поля формы при этом не теряются.
     * <p>Пример: {@code input.readUntilValid(() -> input.readInt("Вместимость: "),
     * YogaClassService::assertValidCapacity)}.
     */
    public <T> T readUntilValid(Supplier<T> reader, Consumer<T> validator) {
        while (true) {
            T value = reader.get();
            try {
                validator.accept(value);
                return value;
            } catch (AppException e) {
                ConsolePrinter.error(e.getMessage());
            }
        }
    }
}
