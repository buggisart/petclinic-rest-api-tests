package com.petclinic.api.owners;

import com.petclinic.api.data.Owners;
import com.petclinic.api.model.Owner;
import com.petclinic.api.model.OwnerRequest;
import com.petclinic.api.model.ProblemDetail;
import com.petclinic.api.support.ApiTestBase;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.provider.Arguments;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class OwnerTestBase extends ApiTestBase {

    protected static final int UNKNOWN_OWNER_ID = 999_999;

    private final Deque<Integer> ownerIdsToDelete = new ArrayDeque<>();

    @AfterEach
    void deleteOwnersCreatedByTest() {
        while (!ownerIdsToDelete.isEmpty()) {
            ownerApi.delete(ownerIdsToDelete.pop());
        }
    }

    protected Owner givenOwner() {
        return givenOwner(Owners.valid());
    }

    protected Owner givenOwner(OwnerRequest payload) {
        Response response = ownerApi.create(payload);
        assertThat(response.statusCode())
                .as("owner is created as test data, body=%s", response.asPrettyString())
                .isEqualTo(201);

        Owner created = response.as(Owner.class);
        deleteAfterTest(created.id());
        return created;
    }

    protected void deleteAfterTest(int ownerId) {
        ownerIdsToDelete.push(ownerId);
    }

    // Tests that delete an owner themselves call this, so that cleanup does not repeat the request
    protected void deletedByTest(int ownerId) {
        ownerIdsToDelete.remove(ownerId);
    }

    /* A payload the API is expected to reject can still be accepted, and then the owner it created
    has to be removed like any other test data. */
    protected void deleteIfCreated(Response response) {
        if (response.statusCode() == 201) {
            deleteAfterTest(response.as(Owner.class).id());
        }
    }

    /* GET, PUT and DELETE take the same path variable, so all three are checked against the same
    matrix of values that are not a positive integer. */
    static Stream<Arguments> invalidOwnerIds() {
        return Stream.of(
                Arguments.of("word", "abc"),
                Arguments.of("blank", " "),
                Arguments.of("negative number", "-1"),
                Arguments.of("fractional", "1.5"),
                Arguments.of("special character", "!"));
    }

    /* Neither PUT nor DELETE is declared on the collection, so a request without an identifier must
    not reach a handler at all: 404 for a path that does not exist and 405 for a method the path
    does not support are both correct answers, a server error is not. */
    protected static void assertCollectionPathRejected(Response response) {
        assertThat(response.statusCode())
                .as("body=%s", response.asPrettyString())
                .isIn(404, 405);
    }

    protected static void assertOwnerFields(Owner actual, OwnerRequest expected) {
        assertThat(actual.firstName()).isEqualTo(expected.firstName());
        assertThat(actual.lastName()).isEqualTo(expected.lastName());
        assertThat(actual.address()).isEqualTo(expected.address());
        assertThat(actual.city()).isEqualTo(expected.city());
        assertThat(actual.telephone()).isEqualTo(expected.telephone());
    }

    protected static ProblemDetail assertProblemDetail(Response response, int expectedStatus) {
        assertThat(response.statusCode())
                .as("body=%s", response.asPrettyString())
                .isEqualTo(expectedStatus);
        assertThat(response.body().asString())
                .as("OpenAPI declares readable and detailed error in the body of this response")
                .isNotBlank();

        ProblemDetail problem = response.as(ProblemDetail.class);
        assertThat(problem.status()).isEqualTo(expectedStatus);
        assertThat(problem.type()).isNotBlank();
        assertThat(problem.title()).isNotBlank();
        assertThat(problem.detail()).isNotBlank();
        assertThat(problem.timestamp()).isNotBlank();
        return problem;
    }
}
