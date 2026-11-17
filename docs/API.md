# API guide

Base URL: `http://localhost:8080`. All request/response bodies are JSON except multipart uploads and file downloads. Secure calls use `Authorization: Basic BASE64(email:password)`. Swagger is generated from the actual controllers and DTOs.

## Endpoint authorization

| Method | Path | Access |
|---|---|---|
| POST | /api/auth/register | Public; creates CUSTOMER only |
| POST | /api/auth/login | Public |
| GET | /api/customers/profile | Customer |
| PUT | /api/customers/profile | Customer |
| GET | /api/loan-products | Public; active products |
| GET | /api/loan-products/{id} | Public; active product |
| POST | /api/loan-products | Admin |
| PUT | /api/loan-products/{id} | Admin |
| POST | /api/eligibility/check | Customer |
| POST | /api/loan-applications | Customer; Idempotency-Key required |
| GET | /api/loan-applications/my-applications | Customer; only owned applications |
| GET | /api/loan-applications | Officer: assigned only; admin: all |
| GET | /api/loan-applications/{id} | Owner / assigned officer / admin |
| GET | /api/loan-applications/{id}/customer | Assigned officer / admin |
| GET | /api/loan-applications/{id}/history | Owner / assigned officer / admin |
| PUT | /api/loan-applications/{id}/status | Assigned officer / admin; state rules apply |
| PUT | /api/loan-applications/{id}/assignment | Admin |
| POST | /api/loan-applications/{id}/remarks | Assigned officer / admin |
| POST | /api/loan-applications/{id}/documents | Owner; editable workflow stage |
| GET | /api/loan-applications/{id}/documents | Owner / assigned officer / admin |
| GET | /api/documents/{id}/download | Owner / assigned officer / admin |
| PUT | /api/documents/{id}/verify | Assigned officer / admin |
| GET | /api/dashboard/customer | Customer |
| GET | /api/dashboard/officer | Officer |
| GET | /api/dashboard/admin | Admin |
| GET | /api/admin/customers | Admin; paginated |
| PUT | /api/admin/customers/{id} | Admin; profile fields |
| PUT | /api/admin/customers/{id}/enabled | Admin |
| GET | /api/admin/loan-officers | Admin; paginated |
| POST | /api/admin/loan-officers | Admin |
| PUT | /api/admin/loan-officers/{id} | Admin; name and enabled |
| GET | /api/admin/loan-products | Admin; includes inactive products |

No delete endpoint destroys loan history. Disable accounts/products instead. Deactivate an officer only after reassigning their open applications. User IDs and customer IDs are distinct: assignment takes an **officer user ID**.

## Examples

Register:
```json
{"fullName":"Demo Customer","email":"new@example.test","password":"choose-your-own-long-password","mobile":"9000000099","dateOfBirth":"1994-03-15","address":"10 Fictional Street","employmentType":"SALARIED","monthlyIncome":100000}
```

Login (replace password with the generated local demo password):
```json
{"email":"customer1@homeloan.demo","password":"YOUR_GENERATED_DEMO_PASSWORD"}
```

Response fields: `authType` ("Basic"), `userId`, `fullName`, `email`, `role`. No tokens are issued. Login failures use one generic message. Passwords are limited to 72 UTF-8 bytes by BCrypt (registration additionally uses 10–64 characters).

Update customer profile: same fields as registration except `email` and `password`. Email is immutable in this basic demo. The customer must be at least 18 years old.

Create/update product:
```json
{"productName":"Demo Home Loan","minimumLoanAmount":100000,"maximumLoanAmount":10000000,"interestRate":8.5,"minimumTenureMonths":12,"maximumTenureMonths":360,"description":"Fictional portfolio product","active":true}
```

Eligibility:
```json
{"monthlyIncome":100000,"existingEmi":5000,"requestedLoanAmount":3000000,"tenureMonths":240,"interestRate":8.5}
```

EMI is `P × r × (1+r)^n / ((1+r)^n − 1)`, where `r = annualPercent / 1200`, `n = tenureMonths`. For zero interest, EMI is `P / n`. DTI is `(existingEmi + newEmi) / monthlyIncome × 100`. Eligibility compares the unrounded ratio to the configured maximum and income to the minimum. Display values are rounded to two decimals. Defaults: minimum income 25000 and maximum DTI 50%. Currency amounts are INR for the demo; there is no currency conversion.

Submit application:
```json
{"loanProductId":1,"requestedLoanAmount":3000000,"tenureMonths":240,"monthlyIncome":100000,"employmentType":"SALARIED","existingEmi":5000}
```

Supply `Idempotency-Key: <a unique UUID>`. Tenure is always **months**, not years. Rate comes from the selected product; clients cannot set an application rate or status. The response contains the reference, assignment and snapshotted financial values. A repeated successful submission with the same key/body returns the existing response (201); a changed payload under the same key returns 409. A new key with an identical active application also returns 409.

Pagination:
`GET /api/loan-applications?page=0&size=20&sort=applicationDate,desc`

Page is zero-based; size is 1–100. Supported sort fields: `id`, `applicationDate`, `requestedLoanAmount`, `status`; direction `asc` or `desc`. Response: `content`, `page`, `size`, `totalElements`, `totalPages`.

Upload uses `multipart/form-data`:
- `documentType`: IDENTITY_PROOF, ADDRESS_PROOF, INCOME_PROOF, BANK_STATEMENT or PROPERTY_DOCUMENT.
- `file`: nonempty PDF/JPG/JPEG/PNG, at most 5 MiB. Content must match extension. Images are decoded and limited to 20 million pixels; PDFs need the PDF header/end marker. MIME supplied by the browser is not trusted.

Each upload creates a new document record. New uploads are allowed in SUBMITTED, DOCUMENT_PENDING and UNDER_REVIEW. Verification operates only on the latest upload per category:
```json
{"status":"VERIFIED","remarks":"Fictional file reviewed for the demo"}
```
Use `REJECTED` with a reason to reject a document. No client may set verification back to PENDING. New replacement uploads start PENDING automatically.

Change application status:
```json
{"status":"DOCUMENT_PENDING","remarks":"Please upload a clearer income proof"}
```

Remarks:
```json
{"remarks":"Customer details reviewed; awaiting property document"}
```

Assignment:
```json
{"officerId":3}
```

Create officer:
```json
{"fullName":"Demo Officer","email":"new-officer@example.test","password":"choose-your-own-long-password"}
```
Update officer: `{"fullName":"Updated Officer","enabled":true}`. Enable/disable customer: `{"enabled":false}`.

## Error contract

Created resources: 201. Successful reads/updates: 200. Invalid payload/bounds: 400. Missing/invalid Basic credentials or failed login: 401. Wrong role/ownership/assignment: 403. Missing resource: 404. Duplicate/concurrent conflict or invalid workflow transition: 409. Multipart size exceeded: 413. Unexpected internal error: 500, without internal exception details.

Validation errors include `timestamp`, `status`, `message`, `path`, and `fieldErrors`. Security-filter errors include `status` and `message`. The client should show `message`, and use `fieldErrors` when present. Unknown JSON properties are rejected, so unsupported changes cannot silently appear to succeed.

Remarks are visible to the application customer. Do not put internal-only or sensitive notes in them. API logs record references/status changes, not request bodies, password hashes or document contents.
