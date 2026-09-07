package com.petclinic.api.owners.create;

import com.petclinic.api.data.Owners;
import com.petclinic.api.model.ProblemDetail;
import com.petclinic.api.model.ValidationMessage;
import com.petclinic.api.owners.OwnerTestBase;
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

import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@Epic("PetClinic REST")
@Feature("Owners")
@Story("Create")
class CreateOwnerValidationTest extends OwnerTestBase {

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidPayloads")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("POST /api/owners rejects invalid owner")
    void createInvalidOwner(String caseName, Map<String, Object> payload) {
        Response response = ownerApi.create(payload);

        ProblemDetail problem = assertProblemDetail(response, 400);
        assertThat(problem.schemaValidationErrors())
                .isNotEmpty()
                .extracting(ValidationMessage::message)
                .allMatch(message -> message != null && !message.isBlank());
    }

    @ParameterizedTest(name = "without {0}")
    @MethodSource("com.petclinic.api.data.Owners#requiredFields")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("POST /api/owners rejects owner with a missing required field")
    void createOwnerWithoutRequiredField(String field) {
        Response response = ownerApi.create(Owners.withoutField(field));

        ProblemDetail problem = assertProblemDetail(response, 400);
        assertThat(problem.schemaValidationErrors())
                .extracting(ValidationMessage::message)
                .as("the error has to name the missing field")
                .anyMatch(message -> message != null && message.contains(field));
    }

    @ParameterizedTest(name = "empty {0}")
    @MethodSource("com.petclinic.api.data.Owners#requiredFields")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("POST /api/owners rejects owner with an empty required field")
    void createOwnerWithEmptyRequiredField(String field) {
        Response response = ownerApi.create(Owners.withEmptyField(field));

        ProblemDetail problem = assertProblemDetail(response, 400);
        assertThat(problem.schemaValidationErrors())
                .extracting(ValidationMessage::message)
                .as("the error has to name the empty field")
                .anyMatch(message -> message != null && message.contains(field));
    }

    static Stream<Arguments> invalidPayloads() {
        return Stream.of(
                Arguments.of("no fields at all", Owners.missingRequiredFields()),
                Arguments.of("non numeric telephone", Owners.nonNumericTelephone())
        );
    }
}
