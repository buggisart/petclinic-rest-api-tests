package com.petclinic.api.support;

import com.petclinic.api.client.HealthApi;
import com.petclinic.api.client.OwnerApi;
import com.petclinic.api.config.PetclinicProperties;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class ApiTestBase {

    // The application is started before the tests, so this only covers the last seconds of its warm-up
    private static final Duration READINESS_TIMEOUT = Duration.ofSeconds(20);

    private static boolean applicationIsUp;
    private static String unreachableReason;

    @Autowired
    protected HealthApi healthApi;

    @Autowired
    protected OwnerApi ownerApi;

    @Autowired
    protected PetclinicProperties properties;

    @BeforeAll
    void applicationIsReady() {
        awaitApplication(properties.baseUrl());
    }

    /* The outcome is remembered for the whole run. Test classes execute in parallel, and without
    that every one of them would sit through the same timeout and report a copy of the same
    infrastructure error. */
    private static synchronized void awaitApplication(String baseUrl) {
        if (applicationIsUp) {
            return;
        }
        if (unreachableReason != null) {
            throw new IllegalStateException(unreachableReason);
        }
        try {
            Awaitility.await()
                    .atMost(READINESS_TIMEOUT)
                    .pollDelay(Duration.ZERO)
                    .pollInterval(Duration.ofSeconds(1))
                    .ignoreExceptions()
                    .until(() -> healthStatus(baseUrl) == 200);
        } catch (ConditionTimeoutException ex) {
            unreachableReason = "PetClinic REST is not reachable at " + baseUrl
                    + ". Start the application before the tests: docker compose up -d --wait."
                    + " If it listens on another address, pass it as -DbaseUrl=...";
            throw new IllegalStateException(unreachableReason, ex);
        }
        applicationIsUp = true;
    }

    private static int healthStatus(String baseUrl) {
        return RestAssured.given()
                .baseUri(baseUrl)
                .accept(ContentType.JSON)
                .get("/actuator/health")
                .statusCode();
    }
}
