package com.petclinic.api.owners.create;

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
@Story("Create")
class CreateOwnerHappyPathTest extends OwnerTestBase {

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("POST /api/owners creates owner")
    void createOwner() {
        OwnerRequest payload = Owners.valid();

        Response response = ownerApi.create(payload);
        assertThat(response.statusCode())
                .as("body=%s", response.asPrettyString())
                .isEqualTo(201);

        Owner created = response.as(Owner.class);
        assertThat(created.id()).isNotNull().isPositive();
        deleteAfterTest(created.id());

        assertOwnerFields(created, payload);

        Response getResponse = ownerApi.get(created.id());
        assertThat(getResponse.statusCode()).isEqualTo(200);

        Owner persisted = getResponse.as(Owner.class);
        assertThat(persisted.id()).isEqualTo(created.id());
        assertOwnerFields(persisted, payload);
    }
}
