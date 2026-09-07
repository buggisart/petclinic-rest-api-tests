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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

@Epic("PetClinic REST")
@Feature("Owners")
@Story("Create")
class CreateOwnerFieldLengthTest extends OwnerTestBase {

    @ParameterizedTest(name = "{0} of one character")
    @MethodSource("com.petclinic.api.data.Owners#fieldsWithLengthRange")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("POST /api/owners accepts the shortest value the schema allows")
    @Description("""
            The schema allows a single character in every field. For address that lower bound looks
            like a placeholder rather than a requirement, so the test fixes the contract as it is
            written today and the expectation changes once the requirement is clarified, see
            BUGS.md, BUG-12.""")
    void createOwnerWithShortestValue(String field) {
        OwnerRequest payload = Owners.withFieldOfLength(field, 1);

        Response response = ownerApi.create(payload);
        assertThat(response.statusCode())
                .as("body=%s", response.asPrettyString())
                .isEqualTo(201);

        Owner created = response.as(Owner.class);
        deleteAfterTest(created.id());
        assertOwnerFields(created, payload);
    }

    @ParameterizedTest(name = "{0} of the maximum length")
    @MethodSource("com.petclinic.api.data.Owners#fieldsWithLengthRange")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("POST /api/owners accepts the longest value the schema allows")
    void createOwnerWithLongestValue(String field) {
        OwnerRequest payload = Owners.withFieldOfLength(field, Owners.maxLength(field));

        Response response = ownerApi.create(payload);
        assertThat(response.statusCode())
                .as("body=%s", response.asPrettyString())
                .isEqualTo(201);

        Owner created = response.as(Owner.class);
        deleteAfterTest(created.id());
        assertOwnerFields(created, payload);
    }

    @ParameterizedTest(name = "{0} longer than the maximum by one character")
    @MethodSource("com.petclinic.api.data.Owners#fieldsWithLengthRange")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("POST /api/owners rejects a value longer than the schema allows")
    @Description("""
            PUT shares the same schema, so the boundaries are covered here once instead of in both
            operations.""")
    void createOwnerWithTooLongValue(String field) {
        Response response = ownerApi.create(Owners.withFieldOfLength(field, Owners.maxLength(field) + 1));
        deleteIfCreated(response);

        ProblemDetail problem = assertProblemDetail(response, 400);
        assertThat(problem.schemaValidationErrors())
                .extracting(ValidationMessage::message)
                .as("the error has to name the too long field")
                .anyMatch(message -> message != null && message.contains(field));
    }
}
