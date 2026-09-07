package com.petclinic.api.owners.create;

import com.petclinic.api.data.Owners;
import com.petclinic.api.model.Owner;
import com.petclinic.api.model.OwnerRequest;
import com.petclinic.api.model.ProblemDetail;
import com.petclinic.api.model.ValidationMessage;
import com.petclinic.api.owners.OwnerTestBase;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@Epic("PetClinic REST")
@Feature("Owners")
@Story("Create")
class CreateOwnerFieldFormatTest extends OwnerTestBase {

    @ParameterizedTest(name = "{0}")
    @MethodSource("valuesBreakingThePattern")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("POST /api/owners rejects a value that does not match the pattern of the field")
    @Description("""
            firstName and lastName allow letters with up to two separators, telephone allows digits
            only, and lastName additionally allows a dot at the end. PUT shares the same schema, so
            the patterns are covered here once instead of in both operations.""")
    void createOwnerWithValueBreakingPattern(String caseName, String field, String value) {
        Response response = ownerApi.create(Owners.withFieldValue(field, value));
        deleteIfCreated(response);

        ProblemDetail problem = assertProblemDetail(response, 400);
        assertThat(problem.schemaValidationErrors())
                .extracting(ValidationMessage::message)
                .as("the error has to name the field that broke the pattern")
                .anyMatch(message -> message != null && message.contains(field));
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("POST /api/owners accepts a dot at the end of lastName")
    @Description("""
            The pattern of lastName ends with an optional dot, unlike the pattern of firstName, so an
            abbreviated surname is valid data and not a defect.""")
    void createOwnerWithDotInLastName() {
        OwnerRequest payload = Owners.valid().withLastName("Lee.");

        Owner created = givenOwner(payload);

        assertOwnerFields(created, payload);
    }

    static Stream<Arguments> valuesBreakingThePattern() {
        return Stream.of(
                Arguments.of("digit in firstName", "firstName", "Ann1"),
                Arguments.of("special character in firstName", "firstName", "Ann@"),
                Arguments.of("dot in firstName", "firstName", "Ann."),
                Arguments.of("separator before firstName", "firstName", "-Ann"),
                Arguments.of("four words in firstName", "firstName", "Ann Marie Jo Lee"),
                Arguments.of("only a space in firstName", "firstName", " "),
                Arguments.of("digit in lastName", "lastName", "Lee1"),
                Arguments.of("space in telephone", "telephone", "12345 6789"),
                Arguments.of("plus in telephone", "telephone", "+1234567890")
        );
    }
}
