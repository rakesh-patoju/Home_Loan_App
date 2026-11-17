package com.demo.homeloan;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BackendIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  private static String customer, other, officer1, officer2, admin;
  private static long applicationId;
  private static String assignedOfficer, unassignedOfficer;

  private String credentials(String email, String password) {
    return Base64.getEncoder().encodeToString((email + ":" + password).getBytes(java.nio.charset.StandardCharsets.UTF_8));
  }

  private String login(String email) throws Exception {
    send("POST", "/api/auth/login", null, Map.of("email", email, "password", "TestOnlyPassword123!"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.authType").value("Basic"))
        .andExpect(jsonPath("$.accessToken").doesNotExist());
    return credentials(email, "TestOnlyPassword123!");
  }

  private JsonNode node(MvcResult r) throws Exception {
    return json.readTree(r.getResponse().getContentAsString());
  }

  private ResultActions send(String method, String path, String token, Object body)
      throws Exception {
    var req =
        switch (method) {
          case "POST" -> post(path);
          case "PUT" -> put(path);
          default -> get(path);
        };
    if (token != null) req.header("Authorization", "Basic " + token);
    if (body != null)
      req.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
    return mvc.perform(req);
  }

  private Map<String, Object> application(long amount) {
    return Map.of(
        "loanProductId",
        1,
        "requestedLoanAmount",
        amount,
        "tenureMonths",
        240,
        "monthlyIncome",
        100000,
        "employmentType",
        "SALARIED",
        "existingEmi",
        5000);
  }

  private ResultActions submit(String token, String key, long amount) throws Exception {
    return mvc.perform(
        post("/api/loan-applications")
            .header("Authorization", "Basic " + token)
            .header("Idempotency-Key", key)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(application(amount))));
  }

  private ResultActions change(String token, String value) throws Exception {
    return send(
        "PUT",
        "/api/loan-applications/" + applicationId + "/status",
        token,
        Map.of("status", value, "remarks", "Automated integration review"));
  }

  @Test
  @Order(1)
  void seedAndLogin() throws Exception {
    customer = login("customer1@homeloan.demo");
    other = login("customer2@homeloan.demo");
    officer1 = login("officer1@homeloan.demo");
    officer2 = login("officer2@homeloan.demo");
    admin = login("admin@homeloan.demo");
    send("GET", "/api/dashboard/admin", admin, null)
        .andExpect(jsonPath("$.totalCustomers").value(5))
        .andExpect(jsonPath("$.totalApplications").value(10));
    send("GET", "/api/loan-products", null, null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(3));
  }

  @Test
  @Order(2)
  void authenticationAndRoleRestrictions() throws Exception {
    send("GET", "/api/customers/profile", null, null).andExpect(status().isUnauthorized());
    send("GET", "/api/dashboard/admin", customer, null).andExpect(status().isForbidden());
    send("GET", "/api/admin/customers", officer1, null).andExpect(status().isForbidden());
    send(
            "POST",
            "/api/auth/login",
            null,
            Map.of("email", "customer1@homeloan.demo", "password", "badpassword"))
        .andExpect(status().isUnauthorized());
    send("GET", "/api/customers/profile", "malformed-basic-credentials", null)
        .andExpect(status().isUnauthorized());
    send("GET", "/api/loan-applications", customer, null).andExpect(status().isForbidden());
  }

  @Test
  @Order(3)
  void registerAndUpdateProfile() throws Exception {
    var body =
        new LinkedHashMap<String, Object>(
            Map.of(
                "fullName",
                "Test Customer",
                "email",
                "test-customer@example.test",
                "password",
                "NewTestPassword123!",
                "mobile",
                "9999999999",
                "dateOfBirth",
                "1995-01-01",
                "address",
                "10 Fictional Street",
                "employmentType",
                "SELF_EMPLOYED",
                "monthlyIncome",
                90000));
    send("POST", "/api/auth/register", null, body).andExpect(status().isCreated());
    String token = credentials("test-customer@example.test", "NewTestPassword123!");
    send("POST", "/api/auth/register", null, body).andExpect(status().isConflict());
    body.remove("email");
    body.remove("password");
    body.put("fullName", "Updated Test Customer");
    send("PUT", "/api/customers/profile", token, body)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.fullName").value("Updated Test Customer"));
    body.put("dateOfBirth", "2020-01-01");
    send("PUT", "/api/customers/profile", token, body).andExpect(status().isBadRequest());
  }

  @Test
  @Order(4)
  void submitAndPreventDuplicates() throws Exception {
    JsonNode a =
        node(
            submit(customer, "integration-key-0001", 3123456)
                .andExpect(status().isCreated())
                .andReturn());
    applicationId = a.get("id").asLong();
    long officerId = a.get("assignedOfficerId").asLong();
    long firstId =
        node(send("GET", "/api/admin/loan-officers", admin, null).andReturn())
            .get("content")
            .get(0)
            .get("id")
            .asLong();
    assignedOfficer = officerId == firstId ? officer1 : officer2;
    unassignedOfficer = officerId == firstId ? officer2 : officer1;
    submit(customer, "integration-key-0001", 3123456)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(applicationId));
    submit(customer, "integration-key-0001", 3123457).andExpect(status().isConflict());
    submit(customer, "integration-key-0002", 3123456).andExpect(status().isConflict());
    submit(customer, "integration-bad-range", 10).andExpect(status().isBadRequest());
    var invalid = application(3123456);
    var bad = new HashMap<>(invalid);
    bad.put("tenureMonths", 6);
    mvc.perform(
            post("/api/loan-applications")
                .header("Authorization", "Basic " + customer)
                .header("Idempotency-Key", "invalid-tenure-01")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(bad)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @Order(5)
  void ownershipAndAssignmentEnforced() throws Exception {
    String p = "/api/loan-applications/" + applicationId;
    for (String token : List.of(other, unassignedOfficer)) {
      send("GET", p, token, null).andExpect(status().isForbidden());
      send("GET", p + "/documents", token, null).andExpect(status().isForbidden());
      send("GET", p + "/history", token, null).andExpect(status().isForbidden());
    }
    send("GET", p + "/customer", assignedOfficer, null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("customer1@homeloan.demo"));
    change(customer, "APPROVED").andExpect(status().isForbidden());
    change(unassignedOfficer, "UNDER_REVIEW").andExpect(status().isForbidden());
    send("GET", "/api/loan-applications/my-applications?sort=passwordHash,asc", customer, null)
        .andExpect(status().isBadRequest());
    send("GET", "/api/loan-applications/my-applications?size=101", customer, null)
        .andExpect(status().isBadRequest());
  }

  @Test
  @Order(6)
  void uploadVerificationAndWorkflow() throws Exception {
    change(assignedOfficer, "UNDER_REVIEW").andExpect(status().isOk());
    change(assignedOfficer, "APPROVED").andExpect(status().isConflict());
    var fake = new MockMultipartFile("file", "fake.pdf", "application/pdf", "not a PDF".getBytes());
    mvc.perform(
            multipart("/api/loan-applications/" + applicationId + "/documents")
                .file(fake)
                .param("documentType", "IDENTITY_PROOF")
                .header("Authorization", "Basic " + customer))
        .andExpect(status().isBadRequest());
    long lastDocument = 0;
    for (String type :
        List.of(
            "IDENTITY_PROOF",
            "ADDRESS_PROOF",
            "INCOME_PROOF",
            "BANK_STATEMENT",
            "PROPERTY_DOCUMENT")) {
      java.awt.image.BufferedImage img =
          new java.awt.image.BufferedImage(2, 2, java.awt.image.BufferedImage.TYPE_INT_RGB);
      java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
      javax.imageio.ImageIO.write(img, "png", out);
      var file = new MockMultipartFile("file", "demo.png", "image/png", out.toByteArray());
      JsonNode d =
          node(
              mvc.perform(
                      multipart("/api/loan-applications/" + applicationId + "/documents")
                          .file(file)
                          .param("documentType", type)
                          .header("Authorization", "Basic " + customer))
                  .andExpect(status().isCreated())
                  .andReturn());
      lastDocument = d.get("id").asLong();
      send(
              "PUT",
              "/api/documents/" + lastDocument + "/verify",
              customer,
              Map.of("status", "VERIFIED", "remarks", "Invalid self verification"))
          .andExpect(status().isForbidden());
      send(
              "PUT",
              "/api/documents/" + lastDocument + "/verify",
              assignedOfficer,
              Map.of("status", "VERIFIED", "remarks", "Demo document reviewed"))
          .andExpect(status().isOk());
    }
    send("GET", "/api/documents/" + lastDocument + "/download", other, null)
        .andExpect(status().isForbidden());
    send("GET", "/api/documents/" + lastDocument + "/download", customer, null)
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Type", "image/png"));
    change(assignedOfficer, "APPROVED").andExpect(status().isOk());
    change(assignedOfficer, "SANCTIONED").andExpect(status().isForbidden());
    change(admin, "SANCTIONED").andExpect(status().isOk());
    change(admin, "DISBURSED").andExpect(status().isOk());
    change(admin, "REJECTED").andExpect(status().isConflict());
    send(
            "PUT",
            "/api/documents/" + lastDocument + "/verify",
            assignedOfficer,
            Map.of("status", "REJECTED", "remarks", "Too late"))
        .andExpect(status().isConflict());
    send("GET", "/api/loan-applications/" + applicationId + "/history", customer, null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(5));
  }

  @Test
  @Order(7)
  void rejectedCannotBeDisbursed() throws Exception {
    long id =
        node(submit(customer, "integration-reject-01", 3234567)
                .andExpect(status().isCreated())
                .andReturn())
            .get("id")
            .asLong();
    send(
            "PUT",
            "/api/loan-applications/" + id + "/status",
            admin,
            Map.of("status", "REJECTED", "remarks", "Incomplete application"))
        .andExpect(status().isOk());
    send(
            "PUT",
            "/api/loan-applications/" + id + "/status",
            admin,
            Map.of("status", "DISBURSED", "remarks", "Invalid transition"))
        .andExpect(status().isConflict());
  }

  @Test
  @Order(8)
  void productAdministrationAndEligibility() throws Exception {
    Map<String, Object> p =
        Map.of(
            "productName",
            "Test Product",
            "minimumLoanAmount",
            100000,
            "maximumLoanAmount",
            5000000,
            "interestRate",
            8.75,
            "minimumTenureMonths",
            12,
            "maximumTenureMonths",
            240,
            "description",
            "Fictional test product",
            "active",
            true);
    send("POST", "/api/loan-products", customer, p).andExpect(status().isForbidden());
    long id =
        node(send("POST", "/api/loan-products", admin, p)
                .andExpect(status().isCreated())
                .andReturn())
            .get("id")
            .asLong();
    var updated = new HashMap<>(p);
    updated.put("active", false);
    send("PUT", "/api/loan-products/" + id, admin, updated).andExpect(status().isOk());
    send("GET", "/api/loan-products/" + id, null, null).andExpect(status().isNotFound());
    updated.put("minimumLoanAmount", 6000000);
    send("PUT", "/api/loan-products/" + id, admin, updated).andExpect(status().isBadRequest());
    send(
            "POST",
            "/api/eligibility/check",
            customer,
            Map.of(
                "monthlyIncome",
                100000,
                "existingEmi",
                5000,
                "requestedLoanAmount",
                3000000,
                "tenureMonths",
                240,
                "interestRate",
                8.5))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.eligible").value(true));
  }

  @Test
  @Order(9)
  void adminManagementAndDisabledToken() throws Exception {
    long id =
        node(send(
                    "POST",
                    "/api/admin/loan-officers",
                    admin,
                    Map.of(
                        "fullName",
                        "New Officer",
                        "email",
                        "officer-new@example.test",
                        "password",
                        "TestOnlyPassword123!"))
                .andExpect(status().isCreated())
                .andReturn())
            .get("id")
            .asLong();
    String token = login("officer-new@example.test");
    send(
            "PUT",
            "/api/admin/loan-officers/" + id,
            admin,
            Map.of("fullName", "New Officer", "enabled", false))
        .andExpect(status().isOk());
    send("GET", "/api/dashboard/officer", token, null).andExpect(status().isUnauthorized());
    send("GET", "/api/dashboard/customer", customer, null).andExpect(status().isOk());
    send("GET", "/api/dashboard/officer", officer1, null).andExpect(status().isOk());
    send("GET", "/api/admin/customers?page=0&size=2", admin, null)
        .andExpect(jsonPath("$.content.length()").value(2));
  }

  @Test
  @Order(10)
  void swaggerWorksAndNoEntityLeaks() throws Exception {
    send("GET", "/v3/api-docs", null, null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths['/api/auth/login']").exists());
    String response =
        send("GET", "/api/loan-applications/" + applicationId, admin, null)
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(response)
        .doesNotContain(
            "passwordHash", "idempotencyKey", "requestFingerprint", "hibernateLazyInitializer");
  }

  @Test
  @Order(11)
  void concurrentRetriesCreateOneApplication() throws Exception {
    try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      var start = new java.util.concurrent.CountDownLatch(1);
      java.util.concurrent.Callable<Long> task =
          () -> {
            start.await();
            return node(submit(customer, "parallel-idempotency-01", 3344556)
                    .andExpect(status().isCreated())
                    .andReturn())
                .get("id")
                .asLong();
          };
      var a = pool.submit(task);
      var b = pool.submit(task);
      start.countDown();
      assertThat(a.get(30, java.util.concurrent.TimeUnit.SECONDS))
          .isEqualTo(b.get(30, java.util.concurrent.TimeUnit.SECONDS));
    }
  }

  private long uploadImage(long id, String type) throws Exception {
    var image = new java.awt.image.BufferedImage(2, 2, java.awt.image.BufferedImage.TYPE_INT_RGB);
    var out = new java.io.ByteArrayOutputStream();
    javax.imageio.ImageIO.write(image, "png", out);
    return node(mvc.perform(
                multipart("/api/loan-applications/" + id + "/documents")
                    .file(
                        new MockMultipartFile("file", "proof.png", "image/png", out.toByteArray()))
                    .param("documentType", type)
                    .header("Authorization", "Basic " + customer))
            .andExpect(status().isCreated())
            .andReturn())
        .get("id")
        .asLong();
  }

  @Test
  @Order(12)
  void latestReplacementMustBeVerified() throws Exception {
    long id =
        node(submit(customer, "replacement-proof-01", 3456789)
                .andExpect(status().isCreated())
                .andReturn())
            .get("id")
            .asLong();
    long oldIdentity = 0;
    for (String type :
        List.of(
            "IDENTITY_PROOF",
            "ADDRESS_PROOF",
            "INCOME_PROOF",
            "BANK_STATEMENT",
            "PROPERTY_DOCUMENT")) {
      long doc = uploadImage(id, type);
      if (type.equals("IDENTITY_PROOF")) oldIdentity = doc;
      send(
              "PUT",
              "/api/documents/" + doc + "/verify",
              admin,
              Map.of("status", "VERIFIED", "remarks", "Reviewed"))
          .andExpect(status().isOk());
    }
    long latest = uploadImage(id, "IDENTITY_PROOF");
    String path = "/api/loan-applications/" + id + "/status";
    send("PUT", path, admin, Map.of("status", "UNDER_REVIEW", "remarks", "Reviewing"))
        .andExpect(status().isOk());
    send(
            "PUT",
            path,
            admin,
            Map.of("status", "APPROVED", "remarks", "Attempt with pending replacement"))
        .andExpect(status().isConflict());
    send(
            "PUT",
            "/api/documents/" + oldIdentity + "/verify",
            admin,
            Map.of("status", "VERIFIED", "remarks", "Old file"))
        .andExpect(status().isConflict());
    send(
            "PUT",
            "/api/documents/" + latest + "/verify",
            admin,
            Map.of("status", "REJECTED", "remarks", "Needs clearer proof"))
        .andExpect(status().isOk());
    send("PUT", path, admin, Map.of("status", "APPROVED", "remarks", "Attempt with rejected proof"))
        .andExpect(status().isConflict());
    send(
            "PUT",
            "/api/documents/" + latest + "/verify",
            admin,
            Map.of("status", "VERIFIED", "remarks", "Review corrected"))
        .andExpect(status().isOk());
    send("PUT", path, admin, Map.of("status", "APPROVED", "remarks", "All latest files reviewed"))
        .andExpect(status().isOk());
  }

  @Test
  @Order(13)
  void malformedRequestsAndImmutableApplications() throws Exception {
    send("PUT", "/api/loan-applications/" + applicationId, customer, application(3000000))
        .andExpect(status().isMethodNotAllowed());
    mvc.perform(
            post("/api/loan-applications")
                .header("Authorization", "Basic " + customer)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(application(3000000))))
        .andExpect(status().isBadRequest());
    var registration = new java.util.HashMap<String, Object>();
    registration.put("email", "x@example.test");
    registration.put("role", "ADMIN");
    send("POST", "/api/auth/register", null, registration).andExpect(status().isBadRequest());
    send("GET", "/api/no-such-endpoint", admin, null).andExpect(status().isNotFound());
  }

  @Test
  @Order(14)
  void reassignmentRevokesOldOfficerAccess() throws Exception {
    long id =
        node(submit(customer, "reassign-proof-01", 3567890)
                .andExpect(status().isCreated())
                .andReturn())
            .get("id")
            .asLong();
    var a = node(send("GET", "/api/loan-applications/" + id, admin, null).andReturn());
    long original = a.get("assignedOfficerId").asLong();
    var officers =
        node(send("GET", "/api/admin/loan-officers", admin, null).andReturn()).get("content");
    long first = officers.get(0).get("id").asLong(), second = officers.get(1).get("id").asLong();
    long replacement = original == first ? second : first;
    String oldToken = original == first ? officer1 : officer2,
        newToken = original == first ? officer2 : officer1;
    send(
            "PUT",
            "/api/loan-applications/" + id + "/assignment",
            admin,
            Map.of("officerId", replacement))
        .andExpect(status().isOk());
    send("GET", "/api/loan-applications/" + id, oldToken, null).andExpect(status().isForbidden());
    send("GET", "/api/loan-applications/" + id, newToken, null).andExpect(status().isOk());
    send(
            "PUT",
            "/api/admin/loan-officers/" + replacement,
            admin,
            Map.of("fullName", "Demo Officer", "enabled", false))
        .andExpect(status().isConflict());
  }

  @Test
  void allowedBrowserPreflightIncludesCorsHeaders() throws Exception {
    mvc.perform(options("/api/eligibility/check")
        .header("Origin", "http://localhost:5173")
        .header("Access-Control-Request-Method", "POST")
        .header("Access-Control-Request-Headers", "authorization,content-type,idempotency-key"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
  }

  @Test
  void untrustedBrowserOriginIsRejected() throws Exception {
    mvc.perform(options("/api/eligibility/check")
        .header("Origin", "https://untrusted.example")
        .header("Access-Control-Request-Method", "POST"))
        .andExpect(status().isForbidden())
        .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
  }

  @Test
  void wrongBasicPasswordAndOldBearerSchemeAreRejected() throws Exception {
    send("GET", "/api/customers/profile", credentials("customer1@homeloan.demo", "IncorrectPassword123!"), null)
        .andExpect(status().isUnauthorized());
    mvc.perform(get("/api/customers/profile").header("Authorization", "Bearer obsolete-token"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void basicCredentialsWorkWithoutLoginOrServerSession() throws Exception {
    send("GET", "/api/customers/profile", credentials("CUSTOMER1@HOMELOAN.DEMO", "TestOnlyPassword123!"), null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("customer1@homeloan.demo"))
        .andExpect(header().doesNotExist("Set-Cookie"));
  }
}
