package com.datapulse.qa.tests;

import com.datapulse.qa.base.BaseTest;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;

import java.util.UUID;

import static org.hamcrest.Matchers.*;

@Epic("DataPulse API")
@Feature("Validation Rules")
@DisplayName("Validation Rules API Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RulesTest extends BaseTest {

    private static final String RULES = "/api/rules";

    private int datasetId;
    private String datasetName;
    private int ruleId;

    @BeforeEach
    void setUp() {
        datasetId   = uploadDefaultDataset();
        datasetName = getDatasetName(datasetId);
        ruleId      = createNotNullRule(datasetName, "name");
    }

    // ─────────────────────────────────────────────
    // POST /api/rules
    // ─────────────────────────────────────────────

    @Test @Order(1)
    @Story("Create Rule")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Creating a NOT_NULL rule should return 201 with is_active=true and correct fields.")
    @DisplayName("Create: NOT_NULL rule → 201")
    void createRule_notNull_returns201() {
        withAuth()
            .body(buildRulePayload(datasetName, "NOT_NULL", "name", null))
        .when()
            .post(RULES)
        .then()
            .statusCode(201)
            .body("rule_type", equalTo("NOT_NULL"))
            .body("severity",  equalTo("HIGH"))
            .body("is_active", equalTo(true))
            .body("id",        notNullValue());
    }

    @Test @Order(2)
    @Story("Create Rule")
    @Severity(SeverityLevel.CRITICAL)
    @Description("DATA_TYPE rule with a valid expected_type parameter should return 201.")
    @DisplayName("Create: DATA_TYPE rule with params → 201")
    void createRule_dataType_returns201() {
        withAuth()
            .body(buildRulePayload(datasetName, "DATA_TYPE", "age", "{\"expected_type\":\"int\"}"))
        .when()
            .post(RULES)
        .then()
            .statusCode(201)
            .body("rule_type", equalTo("DATA_TYPE"));
    }

    @Test @Order(3)
    @Story("Create Rule")
    @Severity(SeverityLevel.CRITICAL)
    @Description("RANGE rule with valid min and max parameters should return 201.")
    @DisplayName("Create: RANGE rule with min/max → 201")
    void createRule_range_returns201() {
        withAuth()
            .body(buildRulePayload(datasetName, "RANGE", "age", "{\"min\":0,\"max\":120}"))
        .when()
            .post(RULES)
        .then()
            .statusCode(201)
            .body("rule_type", equalTo("RANGE"));
    }

    @Test @Order(4)
    @Story("Create Rule")
    @Severity(SeverityLevel.CRITICAL)
    @Description("REGEX rule with a valid regex pattern parameter should return 201.")
    @DisplayName("Create: REGEX rule with pattern → 201")
    void createRule_regex_returns201() {
        withAuth()
            .body(buildRulePayload(datasetName, "REGEX", "email",
                "{\"pattern\":\"^[\\\\w.+-]+@[\\\\w-]+\\\\.[\\\\w.]+$\"}"))
        .when()
            .post(RULES)
        .then()
            .statusCode(201)
            .body("rule_type", equalTo("REGEX"));
    }

    @Test @Order(5)
    @Story("Create Rule")
    @Severity(SeverityLevel.CRITICAL)
    @Description("UNIQUE rule requires no parameters and should return 201.")
    @DisplayName("Create: UNIQUE rule → 201")
    void createRule_unique_returns201() {
        withAuth()
            .body(buildRulePayload(datasetName, "UNIQUE", "id", null))
        .when()
            .post(RULES)
        .then()
            .statusCode(201)
            .body("rule_type", equalTo("UNIQUE"));
    }

    @Test @Order(6)
    @Story("Rule Validation")
    @Severity(SeverityLevel.NORMAL)
    @Description("An unrecognised rule_type should return 400.")
    @DisplayName("Create: invalid rule_type → 400")
    void createRule_invalidType_returns400() {
        withAuth()
            .body(buildRulePayload(datasetName, "FAKE_TYPE", "name", null))
        .when()
            .post(RULES)
        .then()
            .statusCode(400);
    }

    @Test @Order(7)
    @Story("Rule Validation")
    @Severity(SeverityLevel.NORMAL)
    @Description("An invalid severity value (e.g. CRITICAL) should return 400.")
    @DisplayName("Create: invalid severity → 400")
    void createRule_invalidSeverity_returns400() {
        withAuth()
            .body(buildRulePayload(datasetName, "NOT_NULL", "name", null, "CRITICAL"))
        .when()
            .post(RULES)
        .then()
            .statusCode(400);
    }

    @Test @Order(8)
    @Story("Rule Validation")
    @Severity(SeverityLevel.NORMAL)
    @Description("DATA_TYPE rule without the required parameters field should return 400.")
    @DisplayName("Create: DATA_TYPE without params → 400")
    void createRule_dataTypeNoParams_returns400() {
        withAuth()
            .body(buildRulePayload(datasetName, "DATA_TYPE", "age", null))
        .when()
            .post(RULES)
        .then()
            .statusCode(400);
    }

    @Test @Order(9)
    @Story("Rule Validation")
    @Severity(SeverityLevel.NORMAL)
    @Description("RANGE rule where min is greater than or equal to max should return 400.")
    @DisplayName("Create: RANGE min >= max → 400")
    void createRule_rangeMinGtMax_returns400() {
        withAuth()
            .body(buildRulePayload(datasetName, "RANGE", "age", "{\"min\":100,\"max\":10}"))
        .when()
            .post(RULES)
        .then()
            .statusCode(400);
    }

    @Test @Order(10)
    @Story("Rule Validation")
    @Severity(SeverityLevel.MINOR)
    @Description("An empty or whitespace-only field_name should return 400.")
    @DisplayName("Create: empty field_name → 400")
    void createRule_emptyFieldName_returns400() {
        String body = String.format(
            "{\"name\":\"Rule\",\"dataset_type\":\"%s\",\"field_name\":\"   \"," +
            "\"rule_type\":\"NOT_NULL\",\"severity\":\"HIGH\"}", datasetName);
        withAuth().body(body).post(RULES).then().statusCode(400);
    }

    @Test @Order(11)
    @Story("Authorization")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Creating a rule without an Authorization header should return 401.")
    @DisplayName("Create: no auth → 401")
    void createRule_noAuth_returns401() {
        withNoAuth()
            .body(buildRulePayload(datasetName, "NOT_NULL", "name", null))
        .when()
            .post(RULES)
        .then()
            .statusCode(401);
    }

    // ─────────────────────────────────────────────
    // GET /api/rules
    // ─────────────────────────────────────────────

    @Test @Order(12)
    @Story("List Rules")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Authenticated GET /api/rules should return 200 with an array of rules.")
    @DisplayName("List rules: authenticated → 200 with list")
    void listRules_returns200() {
        withAuth().get(RULES).then()
            .statusCode(200)
            .body("$", hasSize(greaterThanOrEqualTo(1)));
    }

    @Test @Order(13)
    @Story("List Rules")
    @Severity(SeverityLevel.NORMAL)
    @Description("The rule created in setUp should appear in the list for the owning user.")
    @DisplayName("List rules: created rule appears in list")
    void listRules_containsCreatedRule() {
        withAuth().get(RULES).then()
            .statusCode(200)
            .body("id", hasItem(ruleId));
    }

    @Test @Order(14)
    @Story("Authorization")
    @Severity(SeverityLevel.BLOCKER)
    @Description("GET /api/rules without a token should return 401.")
    @DisplayName("List rules: no auth → 401")
    void listRules_noAuth_returns401() {
        withNoAuth().get(RULES).then().statusCode(401);
    }

    @Test @Order(15)
    @Story("RBAC / User Isolation")
    @Severity(SeverityLevel.CRITICAL)
    @Description("A second user should not see rules created by the primary user.")
    @DisplayName("List rules: user isolation → second user cannot see first user's rules")
    void listRules_userIsolation() {
        withSecondUserAuth().get(RULES).then()
            .statusCode(200)
            .body("id", not(hasItem(ruleId)));
    }

    // ─────────────────────────────────────────────
    // PATCH /api/rules/{id}
    // ─────────────────────────────────────────────

    @Test @Order(16)
    @Story("Update Rule")
    @Severity(SeverityLevel.CRITICAL)
    @Description("The rule owner should be able to update the rule's name.")
    @DisplayName("Update: change name → 200")
    void updateRule_name_returns200() {
        String newName = "Updated_" + UUID.randomUUID().toString().substring(0, 6);
        withAuth()
            .body("{\"name\":\"" + newName + "\"}")
            .patch(RULES + "/" + ruleId)
        .then()
            .statusCode(200)
            .body("name", equalTo(newName));
    }

    @Test @Order(17)
    @Story("Update Rule")
    @Severity(SeverityLevel.NORMAL)
    @Description("The rule owner should be able to change the severity.")
    @DisplayName("Update: change severity → 200")
    void updateRule_severity_returns200() {
        withAuth()
            .body("{\"severity\":\"LOW\"}")
            .patch(RULES + "/" + ruleId)
        .then()
            .statusCode(200)
            .body("severity", equalTo("LOW"));
    }

    @Test @Order(18)
    @Story("Update Rule")
    @Severity(SeverityLevel.NORMAL)
    @Description("The rule owner should be able to deactivate a rule by setting is_active=false.")
    @DisplayName("Update: deactivate rule → 200")
    void updateRule_deactivate_returns200() {
        withAuth()
            .body("{\"is_active\":false}")
            .patch(RULES + "/" + ruleId)
        .then()
            .statusCode(200)
            .body("is_active", equalTo(false));
    }

    @Test @Order(19)
    @Story("Rule Validation")
    @Severity(SeverityLevel.NORMAL)
    @Description("Updating a rule with an invalid severity should return 400.")
    @DisplayName("Update: invalid severity → 400")
    void updateRule_invalidSeverity_returns400() {
        withAuth()
            .body("{\"severity\":\"EXTREME\"}")
            .patch(RULES + "/" + ruleId)
        .then()
            .statusCode(400);
    }

    @Test @Order(20)
    @Story("Update Rule")
    @Severity(SeverityLevel.NORMAL)
    @Description("Attempting to update a rule that does not exist should return 404.")
    @DisplayName("Update: non-existent rule → 404")
    void updateRule_notFound_returns404() {
        withAuth()
            .body("{\"name\":\"Ghost\"}")
            .patch(RULES + "/999999")
        .then()
            .statusCode(404);
    }

    @Test @Order(21)
    @Story("RBAC / User Isolation")
    @Severity(SeverityLevel.CRITICAL)
    @Description("A user should not be able to update a rule they do not own.")
    @DisplayName("Update: another user's rule → 403")
    void updateRule_anotherUser_returns403() {
        withSecondUserAuth()
            .body("{\"name\":\"Hijacked\"}")
            .patch(RULES + "/" + ruleId)
        .then()
            .statusCode(403);
    }

    @Test @Order(22)
    @Story("Authorization")
    @Severity(SeverityLevel.BLOCKER)
    @Description("PATCH without a token should return 401.")
    @DisplayName("Update: no auth → 401")
    void updateRule_noAuth_returns401() {
        withNoAuth()
            .body("{\"name\":\"No Auth\"}")
            .patch(RULES + "/" + ruleId)
        .then()
            .statusCode(401);
    }

    // ─────────────────────────────────────────────
    // DELETE /api/rules/{id}
    // ─────────────────────────────────────────────

    @Test @Order(23)
    @Story("Delete Rule")
    @Severity(SeverityLevel.CRITICAL)
    @Description("The rule owner should be able to soft-delete their rule, returning 204 No Content.")
    @DisplayName("Delete: owner deletes rule → 204")
    void deleteRule_owner_returns204() {
        int newRule = createNotNullRule(datasetName, "email");
        withAuth().delete(RULES + "/" + newRule).then().statusCode(204);
    }

    @Test @Order(24)
    @Story("Delete Rule")
    @Severity(SeverityLevel.NORMAL)
    @Description("After deletion, the rule should no longer appear in the list.")
    @DisplayName("Delete: deleted rule disappears from list")
    void deleteRule_removedFromList() {
        int newRule = createNotNullRule(datasetName, "department");
        withAuth().delete(RULES + "/" + newRule);
        withAuth().get(RULES).then()
            .statusCode(200)
            .body("id", not(hasItem(newRule)));
    }

    @Test @Order(25)
    @Story("Delete Rule")
    @Severity(SeverityLevel.MINOR)
    @Description("Deleting a non-existent rule ID should return 404.")
    @DisplayName("Delete: non-existent rule → 404")
    void deleteRule_notFound_returns404() {
        withAuth().delete(RULES + "/999999").then().statusCode(404);
    }

    @Test @Order(26)
    @Story("RBAC / User Isolation")
    @Severity(SeverityLevel.CRITICAL)
    @Description("A user should not be able to delete another user's rule.")
    @DisplayName("Delete: another user's rule → 403")
    void deleteRule_anotherUser_returns403() {
        withSecondUserAuth().delete(RULES + "/" + ruleId).then().statusCode(403);
    }

    @Test @Order(27)
    @Story("Authorization")
    @Severity(SeverityLevel.BLOCKER)
    @Description("DELETE without a token should return 401.")
    @DisplayName("Delete: no auth → 401")
    void deleteRule_noAuth_returns401() {
        withNoAuth().delete(RULES + "/" + ruleId).then().statusCode(401);
    }
}
