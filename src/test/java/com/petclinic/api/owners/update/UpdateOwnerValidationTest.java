package com.petclinic.api.owners.update;

import com.petclinic.api.data.Owners;
import com.petclinic.api.model.Owner;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

@Epic("PetClinic REST")
@Feature("Owners")
@Story("Update")
class UpdateOwnerValidationTest extends OwnerTestBase {

    @ParameterizedTest(name = "without {0}")
    @MethodSource("com.petclinic.api.data.Owners#requiredFields")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("PUT /api/owners/{ownerId} rejects owner with a missing required field")
    void updateOwnerWithoutRequiredField(String field) {
        Owner created = givenOwner();

        Response response = ownerApi.update(created.id(), Owners.withoutField(field));

        ProblemDetail problem = assertProblemDetail(response, 400);
        assertThat(problem.schemaValidationErrors())
                .extracting(ValidationMessage::message)
                .as("the error has to name the missing field")
                .anyMatch(message -> message != null && message.contains(field));
    }

    @ParameterizedTest(name = "empty {0}")
    @MethodSource("com.petclinic.api.data.Owners#requiredFields")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("PUT /api/owners/{ownerId} rejects owner with an empty required field")
    void updateOwnerWithEmptyRequiredField(String field) {
        Owner created = givenOwner();

        Response response = ownerApi.update(created.id(), Owners.withEmptyField(field));

        ProblemDetail problem = assertProblemDetail(response, 400);
        assertThat(problem.schemaValidationErrors())
                .extracting(ValidationMessage::message)
                .as("the error has to name the empty field")
                .anyMatch(message -> message != null && message.contains(field));
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("PUT /api/owners/{ownerId} keeps owner unchanged after a rejected update")
    void rejectedUpdateKeepsOwnerUnchanged() {
        Owner created = givenOwner();

        ownerApi.update(created.id(), Owners.withEmptyField("firstName"));

        Response response = ownerApi.get(created.id());
        assertThat(response.statusCode()).isEqualTo(200);
        assertOwnerFields(response.as(Owner.class), created.asRequest());
    }
}
