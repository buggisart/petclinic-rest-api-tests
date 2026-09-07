package com.petclinic.api.client;

import io.qameta.allure.Allure;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.springframework.stereotype.Component;

import static io.restassured.RestAssured.given;

@Component
public class HealthApi {

    private final RequestSpecification spec;

    public HealthApi(RequestSpecification spec) {
        this.spec = spec;
    }

    public Response get() {
        return Allure.step("GET /actuator/health", () -> given().spec(spec).get("/actuator/health"));
    }
}
