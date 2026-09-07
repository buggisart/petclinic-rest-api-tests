package com.petclinic.api.owners.update;

import com.petclinic.api.model.Owner;
import com.petclinic.api.model.OwnerRequest;
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
class UpdateOwnerHappyPathTest extends OwnerTestBase {

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("PUT /api/owners/{ownerId} updates owner")
    @Description("""
            The operation declares 200 with the updated Owner in the body, so the response is
            asserted against the schema and not against the 204 the application returns today. See
            BUGS.md, BUG-1.""")
    void updateOwner() {
        Owner created = givenOwner();

        OwnerRequest updatedPayload = created.asRequest()
                .withAddress("Bauman 10")
                .withCity("Kazan")
                .withTelephone("8435550199");

        Response updateResponse = ownerApi.update(created.id(), updatedPayload);
        assertThat(updateResponse.statusCode())
                .as("body=%s", updateResponse.asPrettyString())
                .isEqualTo(200);

        Owner fromResponse = updateResponse.as(Owner.class);
        assertThat(fromResponse.id()).isEqualTo(created.id());
        assertOwnerFields(fromResponse, updatedPayload);

        Response getResponse = ownerApi.get(created.id());
        assertThat(getResponse.statusCode()).isEqualTo(200);

        Owner persisted = getResponse.as(Owner.class);
        assertThat(persisted.id()).isEqualTo(created.id());
        assertOwnerFields(persisted, updatedPayload);
    }
}
