package com.datapulse.qa.tests;

import com.datapulse.qa.base.BaseTest;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;

import static org.hamcrest.Matchers.*;

@Epic("DataPulse API")
@Feature("Quality Checks")
@DisplayName("Quality Checks API Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ChecksTest extends BaseTest {

    private static final String RUN     = "/api/checks/run/";
    private static final String RESULTS = "/api/checks/results/";

    private int datasetId;

    @BeforeEach
    void setUp() {
        datasetId = uploadDefaultDataset();
        String dsName = getDatasetName(datasetId);
        createNotNullRule(dsName, "name");
    }

    // ─────────────────────────────────────────────
    // POST /api/checks/run/{dataset_id}
    // ─────────────────────────────────────────────

    @Test @Order(1)
    @Story("Run Checks")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Running checks on a dataset with a matching rule should return 200.")
    @DisplayName("Run checks: valid dataset + rule → 200")
    void runChecks_validDataset_returns200() {
        withAuth().post(RUN + datasetId).then().statusCode(200);
    }

    @Test @Order(2)
    @Story("Run Checks")
    @Severity(SeverityLevel.CRITICAL)
    @Description("The check result must contain dataset_id, status=VALIDATED, score, results_summary, and execution_time_seconds.")
    @DisplayName("Run checks: response has all expected fields")
    void runChecks_responseHasExpectedFields() {
        withAuth()
        .when()
            .post(RUN + datasetId)
        .then()
            .statusCode(200)
            .body("dataset_id",             equalTo(datasetId))
            .body("status",                 equalTo("VALIDATED"))
            .body("score",                  notNullValue())
            .body("results_summary",        notNullValue())
            .body("execution_time_seconds", notNullValue());
    }

    @Test @Order(3)
    @Story("Run Checks")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Quality score in the response must be a number between 0 and 100 inclusive.")
    @DisplayName("Run checks: quality score is 0–100")
    void runChecks_scoreIsValidRange() {
        float score = withAuth()
            .post(RUN + datasetId)
            .then()
            .statusCode(200)
            .extract().path("score");

        assert score >= 0 && score <= 100
            : "Score out of expected range [0,100]: " + score;
    }

    @Test @Order(4)
    @Story("Run Checks")
    @Severity(SeverityLevel.NORMAL)
    @Description("Re-running checks on an already validated dataset should still return 200 with VALIDATED status (idempotent).")
    @DisplayName("Run checks: re-run is idempotent → 200")
    void runChecks_rerun_isIdempotent() {
        withAuth().post(RUN + datasetId).then().statusCode(200);
        withAuth()
        .when()
            .post(RUN + datasetId)
        .then()
            .statusCode(200)
            .body("status", equalTo("VALIDATED"));
    }

    @Test @Order(5)
    @Story("Run Checks")
    @Severity(SeverityLevel.NORMAL)
    @Description("Running checks on a non-existent dataset ID should return 404.")
    @DisplayName("Run checks: dataset not found → 404")
    void runChecks_datasetNotFound_returns404() {
        withAuth().post(RUN + "999999").then().statusCode(404);
    }

    @Test @Order(6)
    @Story("RBAC / User Isolation")
    @Severity(SeverityLevel.CRITICAL)
    @Description("A user should not be able to run checks on another user's dataset.")
    @DisplayName("Run checks: another user's dataset → 403")
    void runChecks_anotherUsersDataset_returns403() {
        withSecondUserAuth().post(RUN + datasetId).then().statusCode(403);
    }

    @Test @Order(7)
    @Story("Authorization")
    @Severity(SeverityLevel.BLOCKER)
    @Description("POST /api/checks/run without an Authorization header should return 401.")
    @DisplayName("Run checks: no auth → 401")
    void runChecks_noAuth_returns401() {
        withNoAuth().post(RUN + datasetId).then().statusCode(401);
    }

    // ─────────────────────────────────────────────
    // GET /api/checks/results/{dataset_id}
    // ─────────────────────────────────────────────

    @Test @Order(8)
    @Story("Check Results")
    @Severity(SeverityLevel.CRITICAL)
    @Description("After running checks, GET /api/checks/results/{id} should return 200 with a non-empty list.")
    @DisplayName("Get results: after run → 200 with list")
    void getResults_afterRun_returns200() {
        withAuth().post(RUN + datasetId).then().statusCode(200);

        withAuth()
        .when()
            .get(RESULTS + datasetId)
        .then()
            .statusCode(200)
            .body("$", hasSize(greaterThan(0)));
    }

    @Test @Order(9)
    @Story("Check Results")
    @Severity(SeverityLevel.NORMAL)
    @Description("Each result item must contain rule_id, passed, failed_rows, and total_rows.")
    @DisplayName("Get results: each item has required fields")
    void getResults_eachItemHasRequiredFields() {
        withAuth().post(RUN + datasetId);

        withAuth()
        .when()
            .get(RESULTS + datasetId)
        .then()
            .statusCode(200)
            .body("[0].rule_id",     notNullValue())
            .body("[0].passed",      notNullValue())
            .body("[0].failed_rows", notNullValue())
            .body("[0].total_rows",  notNullValue());
    }

    @Test @Order(10)
    @Story("Check Results")
    @Severity(SeverityLevel.MINOR)
    @Description("Getting results for a non-existent dataset should return 404.")
    @DisplayName("Get results: dataset not found → 404")
    void getResults_notFound_returns404() {
        withAuth().get(RESULTS + "999999").then().statusCode(404);
    }

    @Test @Order(11)
    @Story("RBAC / User Isolation")
    @Severity(SeverityLevel.CRITICAL)
    @Description("A user should not be able to view check results belonging to another user's dataset.")
    @DisplayName("Get results: another user → 403")
    void getResults_anotherUser_returns403() {
        withAuth().post(RUN + datasetId);
        withSecondUserAuth().get(RESULTS + datasetId).then().statusCode(403);
    }

    @Test @Order(12)
    @Story("Authorization")
    @Severity(SeverityLevel.BLOCKER)
    @Description("GET /api/checks/results without a token should return 401.")
    @DisplayName("Get results: no auth → 401")
    void getResults_noAuth_returns401() {
        withNoAuth().get(RESULTS + datasetId).then().statusCode(401);
    }
}
