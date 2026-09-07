package com.petclinic.api.owners.create;

import com.petclinic.api.data.Owners;
import com.petclinic.api.owners.OwnerTestBase;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Epic("PetClinic REST")
@Feature("Owners")
@Story("Create")
class CreateOwnerFieldTypeTest extends OwnerTestBase {

    @ParameterizedTest(name = "{0}")
    @MethodSource("payloadsWithWrongTypes")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("POST /api/owners rejects a field whose JSON type is not a string")
    @Description("""
            Every field of OwnerFields is declared as string, so a number, a boolean, an array or an
            object in its place makes the request body invalid and the operation declares a 400 for
            it. See BUGS.md, BUG-9 for the values that are accepted instead and BUG-10 for the ones
            that end in a server error.""")
    void createOwnerWithWrongFieldType(String caseName, Map<String, Object> payload) {
        Response response = ownerApi.create(payload);
        deleteIfCreated(response);

        assertProblemDetail(response, 400);
    }

    /* The case name carries the defect number, because the cases of this test fail for two different
    reasons: a scalar is silently converted to a string, an array or an object is not readable at
    all. The only case without a number passes. */
    static Stream<Arguments> payloadsWithWrongTypes() {
        return Stream.of(
                Arguments.of("number in firstName", Owners.withFieldValue("firstName", 42)),
                Arguments.of("boolean in firstName (BUG-9)", Owners.withFieldValue("firstName", true)),
                Arguments.of("number in address (BUG-9)", Owners.withFieldValue("address", 42)),
                Arguments.of("number in telephone (BUG-9)", Owners.withFieldValue("telephone", 1_234_567_890)),
                Arguments.of("array in firstName (BUG-10)", Owners.withFieldValue("firstName", List.of("Ann"))),
                Arguments.of("object in city (BUG-10)", Owners.withFieldValue("city", Map.of()))
        );
    }
}
