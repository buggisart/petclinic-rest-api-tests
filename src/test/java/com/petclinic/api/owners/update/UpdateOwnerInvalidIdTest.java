package com.petclinic.api.owners.update;

import com.petclinic.api.data.Owners;
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
import org.junit.jupiter.params.provider.MethodSource;

@Epic("PetClinic REST")
@Feature("Owners")
@Story("Update")
class UpdateOwnerInvalidIdTest extends OwnerTestBase {

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidOwnerIds")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("PUT /api/owners/{ownerId} rejects an id that is not a positive integer")
    @Description("""
            The schema declares ownerId as int32 with minimum 0 and the operation declares a 400
            response, so a value that is not a positive integer is a bad request, not a server
            error. See BUGS.md, BUG-7.""")
    void updateOwnerByInvalidId(String caseName, String ownerId) {
        Response response = ownerApi.updateByRawId(ownerId, Owners.valid());

        assertProblemDetail(response, 400);
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("PUT /api/owners/ without an id is not found")
    @Description("""
            The specification has no PUT on the collection, so a request without an identifier
            addresses a resource that does not exist. See BUGS.md, BUG-7.""")
    void updateOwnerWithoutId() {
        Response response = ownerApi.updateByRawId("", Owners.valid());

        assertCollectionPathRejected(response);
    }
}
