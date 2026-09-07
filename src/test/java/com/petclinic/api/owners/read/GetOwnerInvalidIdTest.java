package com.petclinic.api.owners.read;

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

import static org.assertj.core.api.Assertions.assertThat;

@Epic("PetClinic REST")
@Feature("Owners")
@Story("Read")
class GetOwnerInvalidIdTest extends OwnerTestBase {

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidOwnerIds")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("GET /api/owners/{ownerId} rejects an id that is not a positive integer")
    @Description("""
            The schema declares ownerId as int32 with minimum 0 and the operation declares a 400
            response, so a value that is not a positive integer is a bad request, not a server
            error. See BUGS.md, BUG-7.""")
    void getOwnerByInvalidId(String caseName, String ownerId) {
        Response response = ownerApi.getByRawId(ownerId);

        assertProblemDetail(response, 400);
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("GET /api/owners/ without an id does not fail with a server error")
    @Description("""
            Unlike PUT and DELETE, GET is declared on the collection, so a trailing slash can be
            answered either with the list of owners or with a 404 for an unknown path. Both are
            defensible, a server error is not. See BUGS.md, BUG-7.""")
    void getOwnerWithoutId() {
        Response response = ownerApi.getByRawId("");

        assertThat(response.statusCode())
                .as("body=%s", response.asPrettyString())
                .isLessThan(500);
    }
}
