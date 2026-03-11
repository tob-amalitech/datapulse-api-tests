package com.datapulse.qa.tests;

import com.datapulse.qa.base.BaseTest;
import io.qameta.allure.*;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@Epic("DataPulse API")
@Feature("Authentication")
@DisplayName("Auth API Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AuthTest extends BaseTest {

    private static final String REGISTER = "/api/auth/register";
    private static final String LOGIN    = "/api/auth/login";

    // ─────────────────────────────────────────────
    // POST /api/auth/register
    // ─────────────────────────────────────────────

    @Test
    @Order(1)
    @Story("User Registration")
    @Severity(SeverityLevel.BLOCKER)
    @Description("A new user with a valid email and strong password should receive a 201 response containing a JWT bearer token.")
    @DisplayName("Register: valid user → 201 + JWT token")
    void register_validUser_returns201WithToken() {
        String email = "reg_" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";

        given()
            .contentType(ContentType.JSON)
            .body(String.format(
                "{\"email\":\"%s\",\"password\":\"ValidPass1\",\"full_name\":\"New User\"}", email))
        .when()
            .post(REGISTER)
        .then()
            .statusCode(201)
            .body("access_token", notNullValue())
            .body("token_type",   equalTo("bearer"))
            .body("access_token.length()", greaterThan(10));
    }

    @Test
    @Order(2)
    @Story("User Registration")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Registering with an email that already exists should return 400 with 'already registered' in the detail.")
    @DisplayName("Register: duplicate email → 400")
    void register_duplicateEmail_returns400() {
        String email = "dup_" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
        String body  = String.format(
            "{\"email\":\"%s\",\"password\":\"ValidPass1\",\"full_name\":\"User\"}", email);

        given().contentType(ContentType.JSON).body(body).post(REGISTER);

        given()
            .contentType(ContentType.JSON)
            .body(body)
        .when()
            .post(REGISTER)
        .then()
            .statusCode(400)
            .body("detail", containsStringIgnoringCase("already registered"));
    }

    @Test
    @Order(3)
    @Story("Password Validation")
    @Severity(SeverityLevel.NORMAL)
    @Description("Password shorter than 8 characters must be rejected with 422.")
    @DisplayName("Register: password too short → 422")
    void register_passwordTooShort_returns422() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"email\":\"short@test.com\",\"password\":\"Ab1\",\"full_name\":\"Short\"}")
        .when()
            .post(REGISTER)
        .then()
            .statusCode(422)
            .body(containsString("8 characters"));
    }

    @Test
    @Order(4)
    @Story("Password Validation")
    @Severity(SeverityLevel.NORMAL)
    @Description("Password with only digits and no letters must be rejected with 422.")
    @DisplayName("Register: password has no letter → 422")
    void register_passwordNoLetter_returns422() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"email\":\"nolet@test.com\",\"password\":\"12345678\",\"full_name\":\"NoLetter\"}")
        .when()
            .post(REGISTER)
        .then()
            .statusCode(422)
            .body(containsStringIgnoringCase("letter"));
    }

    @Test
    @Order(5)
    @Story("Password Validation")
    @Severity(SeverityLevel.NORMAL)
    @Description("Password with only letters and no digits must be rejected with 422.")
    @DisplayName("Register: password has no number → 422")
    void register_passwordNoNumber_returns422() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"email\":\"nonum@test.com\",\"password\":\"NoNumbers\",\"full_name\":\"NoNum\"}")
        .when()
            .post(REGISTER)
        .then()
            .statusCode(422)
            .body(containsStringIgnoringCase("number"));
    }

    @Test
    @Order(6)
    @Story("User Registration")
    @Severity(SeverityLevel.MINOR)
    @Description("Request body missing the email field should return 422.")
    @DisplayName("Register: missing email → 422")
    void register_missingEmail_returns422() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"password\":\"ValidPass1\",\"full_name\":\"No Email\"}")
        .when()
            .post(REGISTER)
        .then()
            .statusCode(422);
    }

    @Test
    @Order(7)
    @Story("User Registration")
    @Severity(SeverityLevel.MINOR)
    @Description("Request body missing full_name should return 422.")
    @DisplayName("Register: missing full_name → 422")
    void register_missingFullName_returns422() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"email\":\"noname@test.com\",\"password\":\"ValidPass1\"}")
        .when()
            .post(REGISTER)
        .then()
            .statusCode(422);
    }

    @Test
    @Order(8)
    @Story("User Registration")
    @Severity(SeverityLevel.MINOR)
    @Description("An empty JSON body should return 422.")
    @DisplayName("Register: empty body → 422")
    void register_emptyBody_returns422() {
        given()
            .contentType(ContentType.JSON)
            .body("{}")
        .when()
            .post(REGISTER)
        .then()
            .statusCode(422);
    }

    // ─────────────────────────────────────────────
    // Parameterized weak password tests
    // ─────────────────────────────────────────────

    @ParameterizedTest(name = "[{index}] Weak password: {1}")
    @CsvSource({
        "Ab1,       too short (under 8 chars)",
        "12345678,  no letter at all",
        "NoNumbers, no digit at all"
    })
    @Order(9)
    @Story("Password Validation")
    @Severity(SeverityLevel.NORMAL)
    @Description("Various weak passwords should all be rejected with 422.")
    @DisplayName("Register: weak passwords (parameterized) → 422")
    void register_weakPasswords_returns422(String password, String reason) {
        String email = UUID.randomUUID().toString().substring(0, 8) + "@test.com";
        given()
            .contentType(ContentType.JSON)
            .body(String.format(
                "{\"email\":\"%s\",\"password\":\"%s\",\"full_name\":\"WeakPass\"}",
                email, password.trim()))
        .when()
            .post(REGISTER)
        .then()
            .statusCode(422);
    }

    // ─────────────────────────────────────────────
    // POST /api/auth/login
    // ─────────────────────────────────────────────

    @Test
    @Order(10)
    @Story("User Login")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Valid credentials should return 200 with a JWT bearer token.")
    @DisplayName("Login: valid credentials → 200 + token")
    void login_validCredentials_returns200() {
        String email    = "login_" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
        String password = "LoginPass1";

        given().contentType(ContentType.JSON)
            .body(String.format(
                "{\"email\":\"%s\",\"password\":\"%s\",\"full_name\":\"Login User\"}",
                email, password))
            .post(REGISTER);

        given()
            .contentType(ContentType.JSON)
            .body(String.format("{\"email\":\"%s\",\"password\":\"%s\"}", email, password))
        .when()
            .post(LOGIN)
        .then()
            .statusCode(200)
            .body("access_token", notNullValue())
            .body("token_type",   equalTo("bearer"));
    }

    @Test
    @Order(11)
    @Story("User Login")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Providing an incorrect password should return 401 Unauthorized.")
    @DisplayName("Login: wrong password → 401")
    void login_wrongPassword_returns401() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"email\":\"qa@datapulse.com\",\"password\":\"WrongPass999\"}")
        .when()
            .post(LOGIN)
        .then()
            .statusCode(401);
    }

    @Test
    @Order(12)
    @Story("User Login")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Attempting to log in with a non-existent account should return 401.")
    @DisplayName("Login: non-existent user → 401")
    void login_nonExistentUser_returns401() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"email\":\"ghost@nowhere.com\",\"password\":\"GhostPass1\"}")
        .when()
            .post(LOGIN)
        .then()
            .statusCode(401);
    }

    @Test
    @Order(13)
    @Story("User Login")
    @Severity(SeverityLevel.MINOR)
    @Description("Login request missing the password field should return 422.")
    @DisplayName("Login: missing password → 422")
    void login_missingPassword_returns422() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"email\":\"qa@datapulse.com\"}")
        .when()
            .post(LOGIN)
        .then()
            .statusCode(422);
    }

    @Test
    @Order(14)
    @Story("User Login")
    @Severity(SeverityLevel.MINOR)
    @Description("An empty JSON body for login should return 422.")
    @DisplayName("Login: empty body → 422")
    void login_emptyBody_returns422() {
        given()
            .contentType(ContentType.JSON)
            .body("{}")
        .when()
            .post(LOGIN)
        .then()
            .statusCode(422);
    }
}
