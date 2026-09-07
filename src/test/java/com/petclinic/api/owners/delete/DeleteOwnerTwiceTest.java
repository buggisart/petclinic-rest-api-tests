package com.petclinic.api.owners.delete;

import com.petclinic.api.model.Owner;
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

@Epic("PetClinic REST")
@Feature("Owners")
@Story("Delete")
class DeleteOwnerTwiceTest extends OwnerTestBase {

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("DELETE /api/owners/{ownerId} returns 404 on the second call")
    @Description("""
            The status of the first call is not asserted here, that is the job of
            DeleteOwnerHappyPathTest. This test only pins what happens to a repeated delete: the
            owner is already gone, so the second call has to report 404 with a ProblemDetail body,
            not delete something else or answer success. See BUGS.md, BUG-6.""")
    void deleteOwnerTwice() {
        Owner created = givenOwner();
        ownerApi.delete(created.id());
        deletedByTest(created.id());

        Response response = ownerApi.delete(created.id());

        assertProblemDetail(response, 404);
    }
}
