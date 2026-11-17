# Architecture and database relationships

## Runtime request path

HTTP request → Spring Security Basic authentication validation → Controller validation/role rule → Service implementation/transaction → Repository → PostgreSQL. A mapper constructs response DTOs while the transaction is open. JPA entities are never API responses; `open-in-view=false` prevents hidden database queries during serialization.

The application is one Spring Boot process and one database. Files live under a private directory, never under a frontend public directory. No API gateway, microservices, Redis or messaging infrastructure is needed for this demonstration.

## Packages and important classes

| Package / class | Responsibility |
|---|---|
| HomeLoanApplication | Spring Boot entry point |
| configuration/SecurityConfig | Stateless security, BCrypt, database-backed UserDetailsService and DaoAuthenticationProvider, live account checks, CORS |
| configuration/OpenApiConfig | Swagger information and Basic authorization |
| configuration/DemoDataInitializer | Atomic first-boot fictional data seed; passwords supplied locally |
| controller/*Controller | HTTP routing, request validation and endpoint role rules |
| service/*Service | Service contracts |
| service/impl/AuthServiceImpl | Customer registration, normalized email, password verification, account metadata responses |
| service/impl/CustomerServiceImpl | Customer profile view/update and adult-age check |
| service/impl/ProductServiceImpl | Product bounds and product creation/update |
| service/impl/EligibilityServiceImpl | Reducing-balance EMI, zero-interest branch, unrounded DTI decision |
| service/impl/ApplicationServiceImpl | Product limits, idempotency, duplicate checks, assignment, state machine, audit events |
| service/impl/ApplicationAccess | Shared customer-owner / assigned-officer / admin authorization and row locking |
| service/impl/DocumentServiceImpl | File type/size validation, private file storage, latest-version verification, secure downloads |
| service/impl/DashboardServiceImpl | Role-scoped counts and five most recent applications |
| service/impl/AdminServiceImpl | Customer/officer maintenance, account activation, inactive product listing |
| security/CurrentUser | Resolves the authenticated email to the current enabled database user |
| dto/ApiDtos | Validated request records and explicit response records |
| mapper/ApiMapper | Entity-to-response mapping without passwords, file paths or internal keys |
| exception/GlobalExceptionHandler | Consistent errors and validation field details |
| repository/*Repository | JPA operations, scoped queries, pessimistic locks |
| db/migration/V1__initial_schema.sql | Versioned schema, foreign keys, unique keys, checks and indexes |

## Entity relationships

| From | Relationship | To | Foreign key |
|---|---|---|---|
| Customer | One-to-one | User | customers.user_id (unique) |
| Customer | One-to-many | LoanApplication | loan_applications.customer_id |
| LoanApplication | Many-to-one | LoanProduct | loan_product_id |
| LoanApplication | Many-to-one, optional | User (officer) | assigned_officer_id |
| LoanApplication | One-to-many | LoanDocument | loan_documents.application_id |
| LoanApplication | One-to-many | ApplicationStatusHistory | application_status_history.application_id |
| LoanDocument | Many-to-one, optional | User (verifier) | verified_by_id |
| ApplicationStatusHistory | Many-to-one | User (actor) | changed_by_id |

`User` stores identity, name, login hash and role. `Customer` stores only customer-specific details; it does not duplicate name/email. Officers and admins need no customer row. The `User` class name maps to `app_users` to avoid SQL reserved-word conflicts. Associations are lazy. Explicit inverse collections illustrate one-to-many relationships without exposing them over HTTP. No cascading deletes remove audit history.

Application income, employment and existing EMI are intentional **submission snapshots**, so later profile changes do not rewrite an application. The annual product interest rate is copied at submission for the same reason. Product names are read from the current product row; financial snapshots remain unchanged. Monetary values use BigDecimal/DECIMAL, timestamps use UTC Instant, birth dates use LocalDate.

## Correctness and concurrency

- The connection pool uses READ_COMMITTED isolation so reads after waiting for a row lock see the preceding transaction’s committed application/document changes.
- Customer row lock serializes submissions per customer. A database unique key on `(customer_id, idempotency_key)` adds protection. The normalized request fingerprint blocks identical active requests with different keys; rejected/disbursed applications do not block a new application.
- Application row locks serialize status, upload, verification and assignment operations. JPA `@Version` also detects conflicting application changes.
- The latest uploaded document for every required category must be verified before approval. Uploads/verifications are frozen after approval or rejection. Customers cannot edit submitted loan fields.
- The state transition and its history event share one transaction. Remarks and reassignment are recorded as history events with identical from/to statuses; they do not change status.
- Basic authentication signature, issuer and expiry are validated. Authorities are read from the current enabled database account, so deactivation is effective on the next request. There is no client-controlled registration role.
- Bean validation checks boundaries before service business rules. Database constraints are a final defense, not the only validation.
- Officer selection is a simple least-open-application policy, sufficient for a small demo. Concurrent submissions by different customers can choose the same officer; strict workload balancing is intentionally not promised.

## Workflow

| Current | Allowed next |
|---|---|
| SUBMITTED | UNDER_REVIEW, DOCUMENT_PENDING, REJECTED |
| DOCUMENT_PENDING | UNDER_REVIEW, REJECTED |
| UNDER_REVIEW | DOCUMENT_PENDING, APPROVED, REJECTED |
| APPROVED | SANCTIONED (admin only) |
| SANCTIONED | DISBURSED (admin only) |
| REJECTED | None |
| DISBURSED | None |

Approval requires verified documents. The preliminary calculator does not automatically approve or reject applications; a human officer reviews them. Pending dashboard counts combine SUBMITTED, DOCUMENT_PENDING and UNDER_REVIEW. Approved counts combine APPROVED, SANCTIONED and DISBURSED. Pending verification counts pending applications with a missing or non-verified latest required document.
