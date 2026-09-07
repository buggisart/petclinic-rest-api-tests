package com.petclinic.api.owners.create;

import com.petclinic.api.data.Owners;
import com.petclinic.api.model.Owner;
import com.petclinic.api.model.OwnerRequest;
import com.petclinic.api.model.ProblemDetail;
import com.petclinic.api.model.ValidationMessage;
import com.petclinic.api.owners.OwnerTestBase;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@Epic("PetClinic REST")
@Feature("Owners")
@Story("Create")
@Tag("draft")
class CreateOwnerTelephoneLengthTest extends OwnerTestBase {

    private static final int REQUIRED_DIGITS = 10;

    private static final String DRAFT_NOTE = """
            DRAFT, expectations are not approved yet.

            These cases assume the requirement "telephone is exactly 10 digits", because that is
            what the application enforces. OpenAPI currently declares 1..20 digits, which allows
            values that are not phone numbers in any format and contradicts the implementation
            (see BUGS.md, BUG-3). The boundary cases below are therefore written against the
            expected requirement and are waiting for the requirement change to be confirmed.

            Until then the negative cases fail on purpose: the application answers 500 instead of
            400 for a telephone of a wrong length (see BUGS.md, BUG-4).""";

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("[DRAFT] POST /api/owners accepts telephone of 10 digits")
    @Description(DRAFT_NOTE)
    void createOwnerWithRequiredTelephoneLength() {
        OwnerRequest payload = Owners.withTelephoneDigits(REQUIRED_DIGITS);

        Response response = ownerApi.create(payload);
        assertThat(response.statusCode())
                .as("%d digits is the required length, body=%s", REQUIRED_DIGITS, response.asPrettyString())
                .isEqualTo(201);

        Owner created = response.as(Owner.class);
        deleteAfterTest(created.id());
        assertThat(created.telephone()).isEqualTo(payload.telephone());
    }

    @ParameterizedTest(name = "{0} digits")
    @ValueSource(ints = {REQUIRED_DIGITS - 1, REQUIRED_DIGITS + 1})
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("[DRAFT] POST /api/owners rejects telephone around the required length")
    @Description(DRAFT_NOTE)
    void rejectTelephoneAroundRequiredLength(int digits) {
        OwnerRequest payload = Owners.withTelephoneDigits(digits);

        Response response = ownerApi.create(payload);
        if (response.statusCode() == 201) {
            deleteAfterTest(response.as(Owner.class).id());
        }

        ProblemDetail problem = assertProblemDetail(response, 400);
        assertThat(problem.schemaValidationErrors())
                .isNotEmpty()
                .extracting(ValidationMessage::message)
                .anyMatch(message -> message != null && message.contains("telephone"));
    }
}
