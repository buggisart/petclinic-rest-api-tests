package com.petclinic.api.owners.delete;

import com.petclinic.api.data.Owners;
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
@Story("Delete")
class DeleteOwnerHappyPathTest extends OwnerTestBase {

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("DELETE /api/owners/{ownerId} removes owner")
    @Description("""
            The operation declares 200 with the deleted Owner in the body, so that is what the test
            expects, even though 204 without a body looks like the more reasonable contract here. See
            BUGS.md, BUG-2.""")
    void deleteOwner() {
        OwnerRequest payload = Owners.valid();
        Owner created = givenOwner(payload);

        Response response = ownerApi.delete(created.id());
        deletedByTest(created.id());

        assertThat(response.statusCode())
                .as("body=%s", response.asPrettyString())
                .isEqualTo(200);

        Owner deleted = response.as(Owner.class);
        assertThat(deleted.id()).isEqualTo(created.id());
        assertOwnerFields(deleted, payload);
    }
}
