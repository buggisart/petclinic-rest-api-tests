package com.petclinic.api.owners.update;

import com.petclinic.api.data.Owners;
import com.petclinic.api.model.ProblemDetail;
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

import static org.assertj.core.api.Assertions.assertThat;

@Epic("PetClinic REST")
@Feature("Owners")
@Story("Update")
class UpdateOwnerNotFoundTest extends OwnerTestBase {

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("PUT /api/owners/{ownerId} returns 404 for an owner that does not exist")
    @Description("""
            The 404 response declares a ProblemDetail body, so the test requires a readable reason
            naming the missing owner, not an empty body. See BUGS.md, BUG-6.""")
    void updateOwnerByUnknownId() {
        Response response = ownerApi.update(UNKNOWN_OWNER_ID, Owners.valid());

        ProblemDetail problem = assertProblemDetail(response, 404);
        assertThat(problem.detail())
                .as("the client has to learn which owner is missing")
                .contains(String.valueOf(UNKNOWN_OWNER_ID));
    }
}
