# Verified Basic Auth + PostgreSQL edition

Verified 17 November 2026. This report applies to the Basic Auth/PostgreSQL build in this ZIP, not an earlier JWT/MySQL edition.

| Check | Result |
|---|---|
| Java 21 Maven clean verify and executable JAR | PASS |
| H2 PostgreSQL-mode regression suite | 24 passed; 0 failures/errors/skips |
| Real PostgreSQL 17.12 regression suite | 24 passed; 0 failures/errors/skips |
| Frontend unit tests | 14 passed |
| Vite production build | PASS |
| Browser integration against packaged JAR and real PostgreSQL | 6 scenarios passed |
| Flyway first migration and subsequent restart validation | PASS |
| Application data preserved on backend restart | PASS |
| Direct CORS Basic request and default same-origin proxy | PASS |

Runtime: Java 21.0.5, Spring Boot 3.5.7, Maven 3.9.11, PostgreSQL 17.12, Node 24.22.1. Browser checks used Playwright 1.65.1 with Chromium Headless Shell 131 at desktop 1440x1000 and mobile 390x844. Execution occurred on Linux. Windows launch instructions/scripts are provided, but Windows and physical devices were not executed here. These checks cover tested paths; they are not a zero-defect guarantee.

## What the browser checked

1. Public pages, live products, mobile navigation, responsive overflow and offline/retry.
2. Registration, profile edits surviving reload, role route restrictions and local session expiry.
3. Dashboard, eligibility results and stale-result clearing, direct cross-origin Basic API call, mobile workspace navigation.
4. Customer application submission, all five document uploads, authorized download, officer review, approval blocked until verification, approval, admin sanction/demo disbursement, persisted history and other-customer denial.
5. Admin product creation/deactivation, officer creation/disabling, customer updates and accessible dialog dismissal.
6. Reassignment, unassigned-officer denial, remarks, additional-document request, invalid-file error, document review/rejection, application rejection and no disbursement of a rejected application.

The backend tests cover database seed counts, normalized login, registration validation, roles, ownership, pagination, product bounds, concurrent idempotent submission, duplicate conflicts, document type/size/verification, latest replacement evidence, workflow transitions/history, admin operations, disabled-account denial, direct Basic access without a prior login, wrong-password rejection, obsolete Bearer rejection, CORS allowlist and EMI/DTI boundaries. Passwords remain BCrypt hashes. Flyway creates the schema and Hibernate validates it; ddl-auto is not used to create tables.

## Repeat backend tests

From home-loan-backend, use IntelliJ's Maven test/verify action with JDK 21, or `./mvnw clean verify` (Windows: `.\mvnw.cmd clean verify`) when your terminal Java is configured. Default tests use isolated H2 PostgreSQL mode and do not need an external database. The explicit Lombok annotation processor is in pom.xml.

For real PostgreSQL tests, create an empty disposable database, then use Maven arguments (replace the password locally):

```text
-Dspring.datasource.url=jdbc:postgresql://localhost:5432/home_loan_test
-Dspring.datasource.username=postgres
-Dspring.datasource.password=YOUR_LOCAL_TEST_DATABASE_PASSWORD
-Dspring.datasource.driver-class-name=org.postgresql.Driver
test
```

The suite seeds and changes data. Use a NEW empty database on every run; do not run it against your main demo database. Test-only credentials are defined in src/test/resources/application-test.yml, not used by ordinary startup.

## Repeat frontend tests

Run `npm.cmd ci`, `npm.cmd test`, and `npm.cmd run build` from home-loan-frontend. For browser tests, start the packaged backend against a fresh disposable PostgreSQL database with the demo seed enabled, then start the matching frontend on localhost:5173. Install the test browser with `npx.cmd playwright install chromium`. Supply DEMO_PASSWORD to the test runner with the app.demo.password from your local YAML, then run `npm.cmd run test:e2e`. This test-runner variable is not needed to start the application and no .env file is required. Backend port 8080 is expected for the direct CORS test. Basic credentials and traces are sensitive; keep test-results private.

## Local run

Follow README.md. Set your PostgreSQL username/password in src/main/resources/application.yml, create home_loan_basic_demo in pgAdmin, and start the prebuilt JAR via START-BACKEND.cmd. Start the frontend via node scripts/serve.mjs. Java 21, PostgreSQL and Node.js 22.12+ must be installed. No JWT key, OAuth setup, environment file, Maven build or npm install is required for the prebuilt demo.
