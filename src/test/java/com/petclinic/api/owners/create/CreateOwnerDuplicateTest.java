package com.petclinic.api.owners.create;

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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Epic("PetClinic REST")
@Feature("Owners")
@Story("Create")
class CreateOwnerDuplicateTest extends OwnerTestBase {

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("POST /api/owners twice with the same payload creates two owners")
    @Description("""
            OpenAPI declares no unique field for an owner and no 409 response, so two identical
            owners are a valid state: the same person may be registered twice by mistake, and the
            API has no way to tell that apart. The test pins that behaviour, so that adding a
            uniqueness rule later has to be a deliberate change of the contract.""")
    void createSameOwnerTwice() {
        OwnerRequest payload = Owners.valid();

        Owner first = givenOwner(payload);
        Owner second = givenOwner(payload);

        assertThat(second.id()).isNotEqualTo(first.id());
        assertOwnerFields(second, payload);
    }
}
