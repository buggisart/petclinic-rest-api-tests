package com.petclinic.api.owners.create;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petclinic.api.data.Owners;
import com.petclinic.api.owners.OwnerTestBase;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

@Epic("PetClinic REST")
@Feature("Owners")
@Story("Create")
class CreateOwnerMalformedBodyTest extends OwnerTestBase {

    @Autowired
    private ObjectMapper objectMapper;

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "empty body                 | ''",
            "broken json                | '{'",
            "array instead of an object | '[]'",
            "json string                | '\"owner\"'"
    })
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("POST /api/owners rejects a body it cannot read")
    @Description("""
            The request body is declared as an OwnerFields object, so anything the server cannot read
            as one is an error of the client. See BUGS.md, BUG-10.""")
    void createOwnerWithUnreadableBody(String caseName, String body) {
        Response response = ownerApi.createRaw(ContentType.JSON, body);

        assertProblemDetail(response, 400);
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("POST /api/owners rejects a body sent as text/plain")
    @Description("""
            The operation consumes application/json only, so a valid payload under another media type
            has to be answered with 415 Unsupported Media Type. See BUGS.md, BUG-11.""")
    void createOwnerWithUnsupportedMediaType() throws JsonProcessingException {
        String validJson = objectMapper.writeValueAsString(Owners.valid());

        Response response = ownerApi.createRaw(ContentType.TEXT, validJson);

        assertThat(response.statusCode())
                .as("body=%s", response.asPrettyString())
                .isEqualTo(415);
    }
}
