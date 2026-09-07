package com.petclinic.api.owners.read;

import com.petclinic.api.data.Owners;
import com.petclinic.api.model.Owner;
import com.petclinic.api.model.OwnerRequest;
import com.petclinic.api.owners.OwnerTestBase;
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
@Story("Read")
class GetOwnerHappyPathTest extends OwnerTestBase {

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("GET /api/owners/{ownerId} returns created owner")
    void getOwnerById() {
        OwnerRequest payload = Owners.valid();
        Owner created = givenOwner(payload);

        Response response = ownerApi.get(created.id());
        assertThat(response.statusCode())
                .as("body=%s", response.asPrettyString())
                .isEqualTo(200);

        Owner fetched = response.as(Owner.class);
        assertThat(fetched.id()).isEqualTo(created.id());
        assertOwnerFields(fetched, payload);
    }
}
