# DataPulse API Test Suite — REST Assured + Allure

Black-box API tests for the DataPulse backend using **REST Assured 5.4**, **JUnit 5**, and **Allure Reports**.

---

## ⚠️ Prerequisites — Start Docker First!

Before running any tests, the DataPulse backend must be running. From the **DataPulse_Team8** project root:

```bash
docker-compose up --build
```

Wait until you see the backend is healthy — you can verify it at:
```
http://localhost:8000/health   → {"status": "healthy"}
http://localhost:8000/docs     → Swagger UI
```

---

## Project Structure

```
datapulse-api-tests/
├── pom.xml
├── README.md
├── test-data/
│   ├── valid_data.csv
│   └── invalid_data.csv
└── src/test/java/com/datapulse/qa/
    ├── base/
    │   └── BaseTest.java          ← RestAssured config, auth, shared helpers
    └── tests/
        ├── AuthTest.java          ← POST /api/auth/register + /login  (14 tests)
        ├── UploadTest.java        ← POST /api/datasets/upload + GET   (14 tests)
        ├── RulesTest.java         ← POST/GET/PATCH/DELETE /api/rules  (27 tests)
        ├── ChecksTest.java        ← POST /api/checks/run + GET        (12 tests)
        └── ReportsTest.java       ← GET /api/reports + /trends        (19 tests)
```

---

## Setup in IntelliJ

1. `File → Open` → select this `datapulse-api-tests` folder
2. IntelliJ will detect the `pom.xml` — click **"Load Maven Project"** when prompted
3. Wait for Maven to download all dependencies (REST Assured, Allure, JUnit 5)
4. Make sure Docker is running the backend (see above)

---

## Running Tests

### Option A — IntelliJ (recommended)
- Right-click any test class → **Run**
- Right-click the `tests` package → **Run All Tests**

### Option B — Maven terminal
```bash
# Run all tests
mvn clean test

# Run a specific test class
mvn test -Dtest=AuthTest
mvn test -Dtest=RulesTest

# Run against a different server
API_URL=http://staging.myserver.com mvn test
```

---

## Generating the Allure Report

After running tests, Allure results are saved to `target/allure-results/`.

### Step 1 — Install Allure CLI (one-time setup)

**macOS:**
```bash
brew install allure
```

**Windows (Scoop):**
```bash
scoop install allure
```

**Linux:**
```bash
sudo apt-get install allure
# or via npm:
npm install -g allure-commandline
```

### Step 2 — Generate and open the report

```bash
# Generate + open in browser (live server)
allure serve target/allure-results

# OR generate static HTML to target/allure-report
allure generate target/allure-results --clean -o target/allure-report
# Then open:
open target/allure-report/index.html
```

### Alternative — via Maven (no Allure CLI needed)
```bash
mvn allure:serve       # generates + opens in browser
mvn allure:report      # generates static report only
```

---

## What the Allure Report Shows

| Section | What you see |
|---|---|
| **Overview** | Pass/fail pie chart, total tests, pass rate |
| **Suites** | Tests grouped by class |
| **Behaviors** | Tests grouped by Epic → Feature → Story |
| **Timeline** | Test execution order and duration |
| **Each test** | Full HTTP request + response body attached |

Every test is tagged with:
- **Epic**: DataPulse API
- **Feature**: Authentication / Dataset Upload / Validation Rules / Quality Checks / Reports
- **Story**: e.g. User Registration, RBAC / User Isolation, CSV Export
- **Severity**: BLOCKER / CRITICAL / NORMAL / MINOR

---

## Test Coverage

| Class | Tests | Covers |
|---|---|---|
| `AuthTest` | 14 | Register, login, weak passwords (parameterized), missing fields |
| `UploadTest` | 14 | CSV/JSON upload, file type/size/empty validation, auth, RBAC |
| `RulesTest` | 27 | All 5 rule types, invalid params, CRUD, RBAC |
| `ChecksTest` | 12 | Run, idempotency, score range, RBAC |
| `ReportsTest` | 19 | JSON + CSV format, field consistency, trend filters, RBAC |
| **Total** | **86** | Full API surface |
