package com.datapulse.qa.tests;

import com.datapulse.qa.base.BaseTest;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@Epic("DataPulse API")
@Feature("Dataset Upload")
@DisplayName("Dataset Upload API Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UploadTest extends BaseTest {

    private static final String UPLOAD = "/api/datasets/upload";
    private static final String LIST   = "/api/datasets";

    private static final String VALID_JSON =
        "[{\"id\":1,\"name\":\"Alice\",\"age\":30}," +
         "{\"id\":2,\"name\":\"Bob\",\"age\":25}]";

    // ─────────────────────────────────────────────
    // POST /api/datasets/upload
    // ─────────────────────────────────────────────

    @Test @Order(1)
    @Story("Upload CSV")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Uploading a valid CSV should return 201 with correct row/column counts and PENDING status.")
    @DisplayName("Upload: valid CSV → 201 with metadata")
    void upload_validCsv_returns201() {
        given()
            .header("Authorization", "Bearer " + userToken)
            .multiPart("file", "employees.csv", VALID_CSV.getBytes(), "text/csv")
        .when()
            .post(UPLOAD)
        .then()
            .statusCode(201)
            .body("file_type",    equalTo("csv"))
            .body("row_count",    equalTo(3))
            .body("column_count", equalTo(5))
            .body("status",       equalTo("PENDING"))
            .body("id",           notNullValue());
    }

    @Test @Order(2)
    @Story("Upload JSON")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Uploading a valid JSON array file should return 201 with json file_type.")
    @DisplayName("Upload: valid JSON → 201")
    void upload_validJson_returns201() {
        given()
            .header("Authorization", "Bearer " + userToken)
            .multiPart("file", "data.json", VALID_JSON.getBytes(), "application/json")
        .when()
            .post(UPLOAD)
        .then()
            .statusCode(201)
            .body("file_type", equalTo("json"))
            .body("row_count", equalTo(2));
    }

    @Test @Order(3)
    @Story("File Validation")
    @Severity(SeverityLevel.NORMAL)
    @Description("Uploading a .txt file should be rejected with 400 — only CSV and JSON are supported.")
    @DisplayName("Upload: .txt file → 400 unsupported type")
    void upload_txtFile_returns400() {
        given()
            .header("Authorization", "Bearer " + userToken)
            .multiPart("file", "notes.txt", "some text content".getBytes(), "text/plain")
        .when()
            .post(UPLOAD)
        .then()
            .statusCode(400)
            .body("detail", anyOf(
                containsStringIgnoringCase("unsupported"),
                containsStringIgnoringCase("txt")
            ));
    }

    @Test @Order(4)
    @Story("File Validation")
    @Severity(SeverityLevel.NORMAL)
    @Description("Uploading a zero-byte file should be rejected with 400 and an 'empty' error detail.")
    @DisplayName("Upload: empty file → 400")
    void upload_emptyFile_returns400() {
        given()
            .header("Authorization", "Bearer " + userToken)
            .multiPart("file", "empty.csv", new byte[0], "text/csv")
        .when()
            .post(UPLOAD)
        .then()
            .statusCode(400)
            .body("detail", containsStringIgnoringCase("empty"));
    }

    @Test @Order(5)
    @Story("File Validation")
    @Severity(SeverityLevel.NORMAL)
    @Description("A CSV file containing only headers and no data rows should return 400.")
    @DisplayName("Upload: headers-only CSV → 400")
    void upload_headersOnlyCsv_returns400() {
        given()
            .header("Authorization", "Bearer " + userToken)
            .multiPart("file", "headers_only.csv", "id,name,email\n".getBytes(), "text/csv")
        .when()
            .post(UPLOAD)
        .then()
            .statusCode(400);
    }

    @Test @Order(6)
    @Story("File Validation")
    @Severity(SeverityLevel.MINOR)
    @Description("Uploading a binary/corrupt file disguised as CSV should return 400.")
    @DisplayName("Upload: malformed binary CSV → 400")
    void upload_malformedCsv_returns400() {
        byte[] garbage = {0x00, 0x01, 0x02, (byte) 0xFF, (byte) 0xFE};
        given()
            .header("Authorization", "Bearer " + userToken)
            .multiPart("file", "corrupt.csv", garbage, "text/csv")
        .when()
            .post(UPLOAD)
        .then()
            .statusCode(400);
    }

    @Test @Order(7)
    @Story("Authorization")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Upload request without an Authorization header should return 401.")
    @DisplayName("Upload: no auth → 401")
    void upload_noAuth_returns401() {
        given()
            .multiPart("file", "test.csv", VALID_CSV.getBytes(), "text/csv")
        .when()
            .post(UPLOAD)
        .then()
            .statusCode(401);
    }

    @Test @Order(8)
    @Story("Authorization")
    @Severity(SeverityLevel.CRITICAL)
    @Description("An expired or tampered JWT token should be rejected with 401.")
    @DisplayName("Upload: invalid token → 401")
    void upload_invalidToken_returns401() {
        given()
            .header("Authorization", "Bearer this.is.not.a.valid.token")
            .multiPart("file", "test.csv", VALID_CSV.getBytes(), "text/csv")
        .when()
            .post(UPLOAD)
        .then()
            .statusCode(401);
    }

    @Test @Order(9)
    @Story("Upload CSV")
    @Severity(SeverityLevel.NORMAL)
    @Description("Upload response must include id, name, file_type, row_count, column_count, and status fields.")
    @DisplayName("Upload: response contains all required schema fields")
    void upload_responseHasAllFields() {
        given()
            .header("Authorization", "Bearer " + userToken)
            .multiPart("file", "fields_check.csv", VALID_CSV.getBytes(), "text/csv")
        .when()
            .post(UPLOAD)
        .then()
            .statusCode(201)
            .body("id",           notNullValue())
            .body("name",         notNullValue())
            .body("file_type",    notNullValue())
            .body("row_count",    notNullValue())
            .body("column_count", notNullValue())
            .body("status",       notNullValue());
    }

    // ─────────────────────────────────────────────
    // GET /api/datasets
    // ─────────────────────────────────────────────

    @Test @Order(10)
    @Story("List Datasets")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Authenticated GET /api/datasets should return 200 with a datasets array and total count.")
    @DisplayName("List datasets: authenticated → 200 with structure")
    void listDatasets_authenticated_returns200() {
        uploadDefaultDataset();

        withAuth()
        .when()
            .get(LIST)
        .then()
            .statusCode(200)
            .body("datasets", notNullValue())
            .body("total",    greaterThanOrEqualTo(1))
            .body("datasets", hasSize(greaterThanOrEqualTo(1)));
    }

    @Test @Order(11)
    @Story("List Datasets")
    @Severity(SeverityLevel.NORMAL)
    @Description("The dataset that was just uploaded should appear in the list response.")
    @DisplayName("List datasets: uploaded dataset appears in list")
    void listDatasets_containsUploadedDataset() {
        int id = uploadDefaultDataset();

        withAuth()
        .when()
            .get(LIST)
        .then()
            .statusCode(200)
            .body("datasets.id", hasItem(id));
    }

    @Test @Order(12)
    @Story("Authorization")
    @Severity(SeverityLevel.BLOCKER)
    @Description("GET /api/datasets without a token should return 401.")
    @DisplayName("List datasets: no auth → 401")
    void listDatasets_noAuth_returns401() {
        withNoAuth().get(LIST).then().statusCode(401);
    }

    @Test @Order(13)
    @Story("List Datasets")
    @Severity(SeverityLevel.MINOR)
    @Description("Using a skip value larger than the total dataset count should return an empty list.")
    @DisplayName("List datasets: skip=999 → empty list")
    void listDatasets_highSkip_returnsEmptyList() {
        withAuth()
        .when()
            .get(LIST + "?skip=999")
        .then()
            .statusCode(200)
            .body("datasets", hasSize(0));
    }

    @Test @Order(14)
    @Story("RBAC / User Isolation")
    @Severity(SeverityLevel.CRITICAL)
    @Description("A regular user should not be able to see datasets uploaded by another user.")
    @DisplayName("List datasets: user isolation — second user cannot see first user's datasets")
    void listDatasets_userIsolation() {
        int id = uploadDefaultDataset();

        withSecondUserAuth()
        .when()
            .get(LIST)
        .then()
            .statusCode(200)
            .body("datasets.id", not(hasItem(id)));
    }
}
