package ru.itmo.web;

import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.ws.rs.BadRequestException;

// Validates the transport format; domain constraints belong to the service.
final class JsonFields {
    private JsonFields() {
    }

    static String string(JsonObject input, String field) {
        JsonValue value = required(input, field);
        if (value instanceof JsonString string) {
            return string.getString();
        }
        throw invalid(field, "ожидается строка");
    }

    static boolean bool(JsonObject input, String field) {
        JsonValue value = required(input, field);
        if (value == JsonValue.TRUE) {
            return true;
        }
        if (value == JsonValue.FALSE) {
            return false;
        }
        throw invalid(field, "ожидается true или false");
    }

    static JsonObject object(JsonObject input, String field) {
        JsonValue value = required(input, field);
        if (value instanceof JsonObject object) {
            return object;
        }
        throw invalid(field, "ожидается объект");
    }

    static Long optionalLong(JsonObject input, String field) {
        if (!input.containsKey(field) || input.isNull(field)) {
            return null;
        }
        return longValue(input, field);
    }

    static long longValue(JsonObject input, String field) {
        JsonValue value = required(input, field);
        try {
            if (value instanceof JsonNumber number) {
                return number.longValueExact();
            }
            if (value instanceof JsonString string) {
                return Long.parseLong(string.getString());
            }
        } catch (ArithmeticException | NumberFormatException error) {
            throw invalid(field, "ожидается целое число в диапазоне Long");
        }
        throw invalid(field, "ожидается целое число");
    }

    static int positiveId(JsonObject input, String field) {
        long id = longValue(input, field);
        if (id <= 0 || id > Integer.MAX_VALUE) {
            throw invalid(field, "ID должен быть положительным целым числом");
        }
        return (int) id;
    }

    static double decimal(JsonObject input, String field) {
        JsonValue value = required(input, field);
        if (value instanceof JsonNumber number) {
            double result = number.doubleValue();
            if (Double.isFinite(result)) {
                return result;
            }
        }
        throw invalid(field, "ожидается конечное число");
    }

    static float floatValue(JsonObject input, String field) {
        float result = (float) decimal(input, field);
        if (!Float.isFinite(result)) {
            throw invalid(field, "число выходит за диапазон Float");
        }
        return result;
    }

    static <E extends Enum<E>> E optionalEnum(JsonObject input, String field, Class<E> type) {
        if (!input.containsKey(field) || input.isNull(field)) {
            return null;
        }
        try {
            return Enum.valueOf(type, string(input, field));
        } catch (IllegalArgumentException error) {
            throw invalid(field, "неизвестное значение");
        }
    }

    static void reject(JsonObject input, String... fields) {
        for (String field : fields) {
            if (input.containsKey(field)) {
                throw invalid(field, "поле задаётся сервером");
            }
        }
    }

    private static JsonValue required(JsonObject input, String field) {
        if (input == null || !input.containsKey(field) || input.isNull(field)) {
            throw invalid(field, "поле обязательно");
        }
        return input.get(field);
    }

    private static BadRequestException invalid(String field, String message) {
        String label = switch (field) {
            case "name" -> "Имя";
            case "minutesOfWaiting" -> "Ожидание, минуты";
            case "realHero" -> "Герой";
            case "hasToothpick" -> "Есть зубочистка";
            case "impactSpeed" -> "Скорость удара";
            case "mood" -> "Настроение";
            case "weaponType" -> "Оружие";
            case "car" -> "Машина";
            case "coordinates" -> "Координаты";
            case "cool" -> "Крутая машина";
            default -> field;
        };
        return new BadRequestException("Поле «" + label + "»: " + message);
    }
}
