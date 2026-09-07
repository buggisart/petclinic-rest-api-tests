package com.petclinic.api.client;

import com.petclinic.api.model.OwnerRequest;
import io.qameta.allure.Allure;
import io.restassured.http.ContentType;
import io.restassured.http.Method;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.springframework.stereotype.Component;

import java.util.Map;

import static io.restassured.RestAssured.given;

@Component
public class OwnerApi {

    private static final String OWNERS = "/api/owners";
    private static final String OWNER_BY_ID = OWNERS + "/{ownerId}";
    private static final String OWNER_ID = "ownerId";

    private final RequestSpecification spec;

    public OwnerApi(RequestSpecification spec) {
        this.spec = spec;
    }

    public Response create(OwnerRequest owner) {
        return post(owner);
    }

    // For negative tests, so those payloads are passed as a map of raw JSON fields.
    public Response create(Map<String, Object> body) {
        return post(body);
    }

    /* The body goes out exactly as given, which lets tests send something that is not a valid JSON
    object at all, or send a valid one under the wrong media type. */
    public Response createRaw(ContentType contentType, String body) {
        return Allure.step("%s %s".formatted(Method.POST, OWNERS), () ->
                given().spec(spec)
                        .contentType(contentType)
                        .body(body)
                        .request(Method.POST, OWNERS));
    }

    public Response get(int ownerId) {
        return getByRawId(String.valueOf(ownerId));
    }

    public Response update(int ownerId, OwnerRequest owner) {
        return put(String.valueOf(ownerId), owner);
    }

    public Response update(int ownerId, Map<String, Object> body) {
        return put(String.valueOf(ownerId), body);
    }

    public Response delete(int ownerId) {
        return deleteByRawId(String.valueOf(ownerId));
    }

    // The *ByRawId methods put the value into the path as is, which lets tests send identifiers
    // the API does not accept, for example a word or a blank string.
    public Response getByRawId(String ownerId) {
        return send(Method.GET, ownerId);
    }

    public Response updateByRawId(String ownerId, OwnerRequest owner) {
        return put(ownerId, owner);
    }

    public Response deleteByRawId(String ownerId) {
        return send(Method.DELETE, ownerId);
    }

    private Response post(Object body) {
        return Allure.step("%s %s".formatted(Method.POST, OWNERS), () ->
                given().spec(spec)
                        .body(body)
                        .request(Method.POST, OWNERS));
    }

    // GET and DELETE differ only by the method, the request itself is the same
    private Response send(Method method, String ownerId) {
        return Allure.step(step(method, ownerId), () ->
                given().spec(spec)
                        .pathParam(OWNER_ID, ownerId)
                        .request(method, OWNER_BY_ID));
    }

    private Response put(String ownerId, Object body) {
        return Allure.step(step(Method.PUT, ownerId), () ->
                given().spec(spec)
                        .pathParam(OWNER_ID, ownerId)
                        .body(body)
                        .request(Method.PUT, OWNER_BY_ID));
    }

    private static String step(Method method, String ownerId) {
        return "%s %s/%s".formatted(method, OWNERS, ownerId);
    }
}
