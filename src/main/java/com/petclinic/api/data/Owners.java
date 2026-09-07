package com.petclinic.api.data;

import com.petclinic.api.model.OwnerRequest;
import net.datafaker.Faker;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Owners {

    // Datafaker does not promise thread safety, and test classes run in parallel
    private static final ThreadLocal<Faker> FAKER = ThreadLocal.withInitial(Faker::new);

    /* Names come from the locale dictionaries: the values stay inside the OpenAPI patterns
    (^[\p{L}]+([ '-][\p{L}]+){0,2}$ and the same with an optional trailing dot, 1..30 characters),
    because a dictionary name is letters joined by a space, an apostrophe or a hyphen.
    address and city have no pattern in the schema, only maxLength 255 and 80.
    ^[0-9]*$, 1..20 characters. The application accepts 10 digits only, so shared test data
    sticks to that length; the length boundaries are covered by CreateOwnerTelephoneLengthTest*/
    private static final String TELEPHONE = "[0-9]{10}";

    // "maxLength" of every field of the OwnerFields schema
    private static final Map<String, Integer> MAX_LENGTH = Map.of(
            "firstName", 30,
            "lastName", 30,
            "address", 255,
            "city", 80,
            "telephone", 20);

    private Owners() {
    }

    public static OwnerRequest valid() {
        Faker faker = FAKER.get();
        return new OwnerRequest(
                faker.name().firstName(),
                faker.name().lastName(),
                faker.address().streetAddress(),
                faker.address().city(),
                faker.regexify(TELEPHONE)
        );
    }

    public static OwnerRequest withTelephoneDigits(int digits) {
        return valid().withTelephone(FAKER.get().regexify("[0-9]{" + digits + "}"));
    }



    public static Map<String, Object> withEmptyField(String field) {
        Map<String, Object> body = validAsMap();
        if (body.put(field, "") == null) {
            throw new IllegalArgumentException("Unknown owner field: " + field);
        }
        return body;
    }

    public static Map<String, Object> nonNumericTelephone() {
        Map<String, Object> body = validAsMap();
        body.put("telephone", "not-a-number");
        return body;
    }

    public static Map<String, Object> missingRequiredFields() {
        return Map.of();
    }

    public static List<String> requiredFields() {
        return List.of("firstName", "lastName", "address", "city", "telephone");
    }

    /* Fields whose length range can be tested as is. telephone is left to
    CreateOwnerTelephoneLengthTest: the application accepts one length only, see BUGS.md, BUG-3. */
    public static List<String> fieldsWithLengthRange() {
        return List.of("firstName", "lastName", "address", "city");
    }

    // "maxLength" of the OwnerFields schema, the minimum is 1 for every field
    public static int maxLength(String field) {
        Integer max = MAX_LENGTH.get(field);
        if (max == null) {
            throw new IllegalArgumentException("Unknown owner field: " + field);
        }
        return max;
    }

    /* Letters keep firstName and lastName inside their pattern, address and city have no pattern
    at all, and telephone is the only digits-only field. */
    public static OwnerRequest withFieldOfLength(String field, int length) {
        String characters = "telephone".equals(field) ? "[0-9]" : "[a-z]";
        return withField(field, FAKER.get().regexify(characters + "{" + length + "}"));
    }

    // For negative tests on JSON types, so the value is not limited to a String
    public static Map<String, Object> withFieldValue(String field, Object value) {
        Map<String, Object> body = validAsMap();
        if (body.put(field, value) == null) {
            throw new IllegalArgumentException("Unknown owner field: " + field);
        }
        return body;
    }

    public static Map<String, Object> withoutField(String field) {
        Map<String, Object> body = validAsMap();
        if (body.remove(field) == null) {
            throw new IllegalArgumentException("Unknown owner field: " + field);
        }
        return body;
    }

    private static OwnerRequest withField(String field, String value) {
        OwnerRequest owner = valid();
        return switch (field) {
            case "firstName" -> owner.withFirstName(value);
            case "lastName" -> owner.withLastName(value);
            case "address" -> owner.withAddress(value);
            case "city" -> owner.withCity(value);
            case "telephone" -> owner.withTelephone(value);
            default -> throw new IllegalArgumentException("Unknown owner field: " + field);
        };
    }

    private static Map<String, Object> validAsMap() {
        OwnerRequest owner = valid();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("firstName", owner.firstName());
        body.put("lastName", owner.lastName());
        body.put("address", owner.address());
        body.put("city", owner.city());
        body.put("telephone", owner.telephone());
        return body;
    }
}
