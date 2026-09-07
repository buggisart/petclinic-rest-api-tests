package com.petclinic.api.health;

import com.petclinic.api.model.HealthResponse;
import com.petclinic.api.support.ApiTestBase;
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
@Feature("Health")
class HealthCheckTest extends ApiTestBase {

    @Test
    @Story("Actuator")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("GET /actuator/health returns 200 and status UP")
    void healthIsUp() {
        Response response = healthApi.get();

        assertThat(response.statusCode()).isEqualTo(200);

        HealthResponse body = response.as(HealthResponse.class);
        assertThat(body.status()).isEqualTo("UP");
    }
}
