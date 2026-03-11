package com.datapulse.qa.tests;

import com.datapulse.qa.base.BaseTest;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;

import static org.hamcrest.Matchers.*;

@Epic("DataPulse API")
@Feature("Reports")
@DisplayName("Reports API Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ReportsTest extends BaseTest {

    private static final String REPORTS = "/api/reports/";
    private static final String TRENDS  = "/api/reports/trends";

    private int validatedDatasetId;

    @BeforeEach
    void setUp() {
        validatedDatasetId = createValidatedDataset();
    }

    // ─────────────────────────────────────────────
    // GET /api/reports/{dataset_id} — JSON
    // ─────────────────────────────────────────────

    @Test @Order(1)
    @Story("Get Report")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Fetching a report for a validated dataset should return 200 with application/json content type.")
    @DisplayName("Report JSON: 200 with correct content type")
    void getReport_json_returns200() {
        withAuth()
        .when()
            .get(REPORTS + validatedDatasetId)
        .then()
            .statusCode(200)
            .contentType(containsString("application/json"));
    }

    @Test @Order(2)
    @Story("Get Report")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Report JSON must contain score, total_rules, passed_rules, failed_rules, and results.")
    @DisplayName("Report JSON: has all required fields")
    void getReport_json_hasRequiredFields() {
        withAuth()
        .when()
            .get(REPORTS + validatedDatasetId)
        .then()
            .statusCode(200)
            .body("score",        notNullValue())
            .body("total_rules",  notNullValue())
            .body("passed_rules", notNullValue())
            .body("failed_rules", notNullValue())
            .body("results",      notNullValue());
    }

    @Test @Order(3)
    @Story("Get Report")
    @Severity(SeverityLevel.CRITICAL)
    @Description("The quality score in the report must be a number between 0 and 100.")
    @DisplayName("Report JSON: score is valid (0–100)")
    void getReport_json_scoreIsValid() {
        withAuth()
        .when()
            .get(REPORTS + validatedDatasetId)
        .then()
            .statusCode(200)
            .body("score", allOf(greaterThanOrEqualTo(0f), lessThanOrEqualTo(100f)));
    }

    @Test @Order(4)
    @Story("Get Report")
    @Severity(SeverityLevel.NORMAL)
    @Description("passed_rules + failed_rules must equal total_rules — rule counts must be consistent.")
    @DisplayName("Report JSON: passed + failed = total_rules")
    void getReport_json_ruleCountsConsistent() {
        var body = withAuth()
            .get(REPORTS + validatedDatasetId)
            .then().statusCode(200).extract().body();

        int total  = body.path("total_rules");
        int passed = body.path("passed_rules");
        int failed = body.path("failed_rules");

        assert passed + failed == total
            : String.format("Inconsistent rule counts: passed(%d) + failed(%d) != total(%d)", passed, failed, total);
    }

    @Test @Order(5)
    @Story("Get Report")
    @Severity(SeverityLevel.NORMAL)
    @Description("The results list in the report must contain at least one entry.")
    @DisplayName("Report JSON: results list is non-empty")
    void getReport_json_resultsNonEmpty() {
        withAuth()
        .when()
            .get(REPORTS + validatedDatasetId)
        .then()
            .statusCode(200)
            .body("results", hasSize(greaterThan(0)));
    }

    // ─────────────────────────────────────────────
    // GET /api/reports/{id}?format=csv
    // ─────────────────────────────────────────────

    @Test @Order(6)
    @Story("CSV Export")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Requesting the report with ?format=csv should return 200 with text/csv content type.")
    @DisplayName("Report CSV: 200 with text/csv")
    void getReport_csv_returns200() {
        withAuth()
        .when()
            .get(REPORTS + validatedDatasetId + "?format=csv")
        .then()
            .statusCode(200)
            .contentType(containsString("text/csv"));
    }

    @Test @Order(7)
    @Story("CSV Export")
    @Severity(SeverityLevel.NORMAL)
    @Description("CSV response must include a Content-Disposition header indicating an attachment download.")
    @DisplayName("Report CSV: Content-Disposition attachment header present")
    void getReport_csv_hasContentDispositionHeader() {
        withAuth()
        .when()
            .get(REPORTS + validatedDatasetId + "?format=csv")
        .then()
            .statusCode(200)
            .header("Content-Disposition", containsStringIgnoringCase("attachment"));
    }

    @Test @Order(8)
    @Story("CSV Export")
    @Severity(SeverityLevel.NORMAL)
    @Description("The CSV body must be non-empty and contain at least Score or Rule headers.")
    @DisplayName("Report CSV: body contains data")
    void getReport_csv_bodyContainsData() {
        String csvBody = withAuth()
            .get(REPORTS + validatedDatasetId + "?format=csv")
            .then().statusCode(200)
            .extract().body().asString();

        assert csvBody != null && !csvBody.isEmpty() : "CSV body is empty";
        assert csvBody.contains("Score") || csvBody.contains("Rule")
            : "CSV body is missing expected column headers";
    }

    // ─────────────────────────────────────────────
    // Authorization
    // ─────────────────────────────────────────────

    @Test @Order(9)
    @Story("Get Report")
    @Severity(SeverityLevel.NORMAL)
    @Description("Requesting a report for a non-existent dataset should return 404.")
    @DisplayName("Report: dataset not found → 404")
    void getReport_notFound_returns404() {
        withAuth().get(REPORTS + "999999").then().statusCode(404);
    }

    @Test @Order(10)
    @Story("RBAC / User Isolation")
    @Severity(SeverityLevel.CRITICAL)
    @Description("A user should not be able to view another user's dataset report.")
    @DisplayName("Report: another user's dataset → 403")
    void getReport_anotherUser_returns403() {
        withSecondUserAuth().get(REPORTS + validatedDatasetId).then().statusCode(403);
    }

    @Test @Order(11)
    @Story("Authorization")
    @Severity(SeverityLevel.BLOCKER)
    @Description("GET /api/reports without a token should return 401.")
    @DisplayName("Report: no auth → 401")
    void getReport_noAuth_returns401() {
        withNoAuth().get(REPORTS + validatedDatasetId).then().statusCode(401);
    }

    // ─────────────────────────────────────────────
    // GET /api/reports/trends
    // ─────────────────────────────────────────────

    @Test @Order(12)
    @Story("Quality Trends")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Default GET /api/reports/trends should return 200.")
    @DisplayName("Trends: default request → 200")
    void trends_defaultRequest_returns200() {
        withAuth().get(TRENDS).then().statusCode(200);
    }

    @Test @Order(13)
    @Story("Quality Trends")
    @Severity(SeverityLevel.NORMAL)
    @Description("Filtering trends by dataset_id should return 200.")
    @DisplayName("Trends: filter by dataset_id → 200")
    void trends_filteredByDatasetId_returns200() {
        withAuth().get(TRENDS + "?dataset_id=" + validatedDatasetId).then().statusCode(200);
    }

    @Test @Order(14)
    @Story("Quality Trends")
    @Severity(SeverityLevel.MINOR)
    @Description("Using days=7 filter should return 200.")
    @DisplayName("Trends: days=7 → 200")
    void trends_customDays_returns200() {
        withAuth().get(TRENDS + "?days=7").then().statusCode(200);
    }

    @Test @Order(15)
    @Story("Quality Trends")
    @Severity(SeverityLevel.MINOR)
    @Description("Interval=day should return 200.")
    @DisplayName("Trends: interval=day → 200")
    void trends_intervalDay_returns200() {
        withAuth().get(TRENDS + "?interval=day").then().statusCode(200);
    }

    @Test @Order(16)
    @Story("Quality Trends")
    @Severity(SeverityLevel.MINOR)
    @Description("Interval=week should return 200.")
    @DisplayName("Trends: interval=week → 200")
    void trends_intervalWeek_returns200() {
        withAuth().get(TRENDS + "?interval=week").then().statusCode(200);
    }

    @Test @Order(17)
    @Story("Quality Trends")
    @Severity(SeverityLevel.MINOR)
    @Description("Interval=month should return 200.")
    @DisplayName("Trends: interval=month → 200")
    void trends_intervalMonth_returns200() {
        withAuth().get(TRENDS + "?interval=month").then().statusCode(200);
    }

    @Test @Order(18)
    @Story("Quality Trends")
    @Severity(SeverityLevel.NORMAL)
    @Description("An unrecognised interval value should return 400.")
    @DisplayName("Trends: invalid interval → 400")
    void trends_invalidInterval_returns400() {
        withAuth().get(TRENDS + "?interval=hourly").then().statusCode(400);
    }

    @Test @Order(19)
    @Story("Authorization")
    @Severity(SeverityLevel.BLOCKER)
    @Description("GET /api/reports/trends without a token should return 401.")
    @DisplayName("Trends: no auth → 401")
    void trends_noAuth_returns401() {
        withNoAuth().get(TRENDS).then().statusCode(401);
    }
}
