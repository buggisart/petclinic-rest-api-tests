package com.petclinic.api.owners;

import com.petclinic.api.data.Owners;
import com.petclinic.api.model.Owner;
import com.petclinic.api.model.OwnerRequest;
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
@Story("CRUD flow")
class OwnerCrudFlowTest extends OwnerTestBase {

    @Test
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Owner lives through create, read, update and delete")
    @Description("""
            One owner is followed through the whole lifecycle: it becomes readable by id, carries
            the new data after an update and disappears after a delete. Every status comes from the
            specification, so the flow stops on the code PUT returns today and reaches the delete
            steps only once that is fixed, see BUGS.md, BUG-1 and BUG-2.""")
    void ownerCrudFlow() {
        OwnerRequest payload = Owners.valid();

        Response createResponse = ownerApi.create(payload);
        assertThat(createResponse.statusCode())
                .as("body=%s", createResponse.asPrettyString())
                .isEqualTo(201);

        Owner created = createResponse.as(Owner.class);
        deleteAfterTest(created.id());
        assertThat(created.id()).isNotNull().isPositive();

        Response afterCreate = ownerApi.get(created.id());
        assertThat(afterCreate.statusCode())
                .as("body=%s", afterCreate.asPrettyString())
                .isEqualTo(200);
        assertOwnerFields(afterCreate.as(Owner.class), payload);

        OwnerRequest updatedPayload = Owners.valid();
        Response updateResponse = ownerApi.update(created.id(), updatedPayload);
        assertThat(updateResponse.statusCode())
                .as("body=%s", updateResponse.asPrettyString())
                .isEqualTo(200);

        Response afterUpdate = ownerApi.get(created.id());
        assertThat(afterUpdate.statusCode())
                .as("body=%s", afterUpdate.asPrettyString())
                .isEqualTo(200);

        Owner updated = afterUpdate.as(Owner.class);
        assertThat(updated.id()).isEqualTo(created.id());
        assertOwnerFields(updated, updatedPayload);

        Response deleteResponse = ownerApi.delete(created.id());
        deletedByTest(created.id());
        assertThat(deleteResponse.statusCode())
                .as("body=%s", deleteResponse.asPrettyString())
                .isEqualTo(200);

        Response afterDelete = ownerApi.get(created.id());
        assertThat(afterDelete.statusCode())
                .as("body=%s", afterDelete.asPrettyString())
                .isEqualTo(404);
    }
}
