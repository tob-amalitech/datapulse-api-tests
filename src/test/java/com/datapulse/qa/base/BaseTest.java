package com.datapulse.qa.base;

import io.qameta.allure.Step;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;

import java.io.File;
import java.util.UUID;

import static io.restassured.RestAssured.given;

/**
 * BaseTest — shared foundation for all DataPulse API test classes.
 *
 * Responsibilities:
 *  - Configure RestAssured base URI and Allure filter
 *  - Register + authenticate two users (primary + secondary for RBAC tests)
 *  - Provide helper methods used across all test classes
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseTest {

    // ── Config ────────────────────────────────────────────────────────────────
    protected static final String BASE_URL =
        System.getProperty("API_URL",
            System.getenv().getOrDefault("API_URL", "http://datapulse-alb-prod-253557199.eu-west-1.elb.amazonaws.com/"));

    protected static final String TEST_PASSWORD = "QaTest123";

    // Valid CSV content used across tests
    protected static final String VALID_CSV =
        "id,name,email,age,department\n" +
        "1,Alice,alice@test.com,30,Engineering\n" +
        "2,Bob,bob@test.com,28,Marketing\n" +
        "3,Carol,carol@test.com,35,Sales\n";

    // ── Auth tokens ───────────────────────────────────────────────────────────
    protected static String userToken;
    protected static String secondUserToken;

    // ─────────────────────────────────────────────────────────────────────────
    // Global Setup
    // ─────────────────────────────────────────────────────────────────────────

    @BeforeAll
    static void globalSetup() {
        RestAssured.baseURI = BASE_URL;

        // Attach Allure filter — captures every request/response in the report
        RestAssured.filters(
            new AllureRestAssured()
                .setRequestTemplate("http-request.ftl")
                .setResponseTemplate("http-response.ftl"),
            new RequestLoggingFilter(),
            new ResponseLoggingFilter()
        );

        setupUsers();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // User Registration
    // ─────────────────────────────────────────────────────────────────────────

    private static void setupUsers() {
        String email1 = "qa_" + UUID.randomUUID().toString().substring(0, 8) + "@datapulse.com";
        String email2 = "qa2_" + UUID.randomUUID().toString().substring(0, 8) + "@datapulse.com";

        userToken       = registerAndLogin(email1, TEST_PASSWORD, "QA Primary User");
        secondUserToken = registerAndLogin(email2, TEST_PASSWORD, "QA Secondary User");
    }

    @Step("Register user '{email}' and obtain JWT token")
    protected static String registerAndLogin(String email, String password, String fullName) {
        // Register — ignore 400 if user already exists
        given()
            .contentType(ContentType.JSON)
            .body(String.format(
                "{\"email\":\"%s\",\"password\":\"%s\",\"full_name\":\"%s\"}",
                email, password, fullName))
            .post("/api/auth/register");

        // Login and return token
        return given()
            .contentType(ContentType.JSON)
            .body(String.format("{\"email\":\"%s\",\"password\":\"%s\"}", email, password))
            .post("/api/auth/login")
            .then()
            .statusCode(200)
            .extract().path("access_token");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Request Spec Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /** Primary user — authenticated JSON requests */
    protected RequestSpecification withAuth() {
        return given()
            .header("Authorization", "Bearer " + userToken)
            .contentType(ContentType.JSON);
    }

    /** Second user — for RBAC/isolation tests */
    protected RequestSpecification withSecondUserAuth() {
        return given()
            .header("Authorization", "Bearer " + secondUserToken)
            .contentType(ContentType.JSON);
    }

    /** No auth — for 401 tests */
    protected RequestSpecification withNoAuth() {
        return given().contentType(ContentType.JSON);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Dataset Helpers
    // ─────────────────────────────────────────────────────────────────────────

    @Step("Upload default valid CSV dataset")
    protected int uploadDefaultDataset() {
        return given()
            .header("Authorization", "Bearer " + userToken)
            .multiPart("file", "test_data.csv", VALID_CSV.getBytes(), "text/csv")
            .post("/api/datasets/upload")
            .then()
            .statusCode(201)
            .extract().path("id");
    }

    @Step("Fetch dataset name for id={datasetId}")
    protected String getDatasetName(int datasetId) {
        return withAuth()
            .get("/api/datasets")
            .then()
            .extract()
            .path("datasets.find { it.id == " + datasetId + " }.name");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Rule Helpers
    // ─────────────────────────────────────────────────────────────────────────

    @Step("Create NOT_NULL rule on field '{fieldName}' for dataset '{datasetName}'")
    protected int createNotNullRule(String datasetName, String fieldName) {
        String body = String.format(
            "{\"name\":\"Rule %s\",\"dataset_type\":\"%s\",\"field_name\":\"%s\"," +
            "\"rule_type\":\"NOT_NULL\",\"severity\":\"HIGH\"}",
            UUID.randomUUID().toString().substring(0, 6), datasetName, fieldName);

        return withAuth()
            .body(body)
            .post("/api/rules")
            .then()
            .statusCode(201)
            .extract().path("id");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Checks Helpers
    // ─────────────────────────────────────────────────────────────────────────

    @Step("Run quality checks on dataset id={datasetId}")
    protected Response runChecks(int datasetId) {
        return withAuth().post("/api/checks/run/" + datasetId);
    }

    @Step("Upload dataset, create rule, and run checks — returns validated dataset id")
    protected int createValidatedDataset() {
        int datasetId   = uploadDefaultDataset();
        String dsName   = getDatasetName(datasetId);
        createNotNullRule(dsName, "name");
        runChecks(datasetId).then().statusCode(200);
        return datasetId;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Rule Payload Builder
    // ─────────────────────────────────────────────────────────────────────────

    protected String buildRulePayload(String datasetName, String ruleType,
                                       String fieldName, String params,
                                       String severity) {
        String name = "Rule_" + UUID.randomUUID().toString().substring(0, 6);
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(
            "{\"name\":\"%s\",\"dataset_type\":\"%s\",\"field_name\":\"%s\"," +
            "\"rule_type\":\"%s\",\"severity\":\"%s\"",
            name, datasetName, fieldName, ruleType, severity));
        if (params != null) {
            // Escape the inner JSON string for embedding
            sb.append(",\"parameters\":\"").append(params.replace("\"", "\\\"")).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    protected String buildRulePayload(String datasetName, String ruleType,
                                       String fieldName, String params) {
        return buildRulePayload(datasetName, ruleType, fieldName, params, "HIGH");
    }
}
