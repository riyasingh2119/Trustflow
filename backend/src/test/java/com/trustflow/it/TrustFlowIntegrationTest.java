package com.trustflow.it;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration + API tests against a real PostgreSQL (Testcontainers) with the Flyway migrations applied.
 * Skipped automatically when Docker is not available.
 * Seed data: resource 1 = Dev Orders API (LOW), resource 2 = Prod Customer DB (HIGH).
 */
@SpringBootTest(properties = {
        "trustflow.jwt.secret=integration-test-secret-integration-test-secret-01",
        "trustflow.demo-mode=true"
})
@AutoConfigureMockMvc
@AutoConfigureObservability
@Testcontainers(disabledWithoutDocker = true)
class TrustFlowIntegrationTest {

    @Container
    static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", pg::getJdbcUrl);
        r.add("spring.datasource.username", pg::getUsername);
        r.add("spring.datasource.password", pg::getPassword);
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private String login(String user, String password) throws Exception {
        MvcResult r = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + user + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk()).andReturn();
        return json.readTree(r.getResponse().getContentAsString()).get("token").asText();
    }

    private MvcResult access(String token, long resource, String action, String device, int hour, String mfa) throws Exception {
        var req = post("/api/gateway/access").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"resourceId\":" + resource + ",\"action\":\"" + action + "\",\"simulatedHour\":" + hour + "}");
        if (device != null) req = req.header("X-Device-Id", device);
        if (mfa != null) req = req.header("X-MFA-Token", mfa);
        return mvc.perform(req).andReturn();
    }

    private JsonNode body(MvcResult r) throws Exception {
        return json.readTree(r.getResponse().getContentAsString());
    }

    @Test
    void gatewayWithoutTokenIs401() throws Exception {
        mvc.perform(post("/api/gateway/access").contentType(MediaType.APPLICATION_JSON)
                .content("{\"resourceId\":1,\"action\":\"READ\"}")).andExpect(status().isUnauthorized());
    }

    @Test
    void wrongPasswordIs401() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"auditor\",\"password\":\"nope-nope\"}")).andExpect(status().isUnauthorized());
    }

    @Test
    void managedDeviceReadingLowResourceIsAllowedWithScore5() throws Exception {
        String t = login("dev", "Passw0rd!");
        MvcResult r = access(t, 1, "READ", "laptop-managed-01", 11, null);
        assertEquals(200, r.getResponse().getStatus());
        assertEquals("ALLOW", body(r).get("decision").asText());
        assertEquals(5, body(r).get("score").asInt());
    }

    @Test
    void unknownDeviceOnHighResourceNeedsMfaThenSucceedsOnceOnly() throws Exception {
        String t = login("dev", "Passw0rd!");
        MvcResult first = access(t, 2, "READ", "phone-byod-02", 14, null);
        assertEquals("MFA_REQUIRED", body(first).get("decision").asText());
        assertEquals(45, body(first).get("score").asInt());
        String challenge = body(first).get("challengeId").asText();

        String code = json.readTree(mvc.perform(get("/api/auth/mfa/demo-code").header("Authorization", "Bearer " + t))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("code").asText();
        String mfaToken = json.readTree(mvc.perform(post("/api/auth/mfa/verify").header("Authorization", "Bearer " + t)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"challengeId\":\"" + challenge + "\",\"code\":\"" + code + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("mfaToken").asText();

        MvcResult retry = access(t, 2, "READ", "phone-byod-02", 14, mfaToken);
        assertEquals("ALLOW", body(retry).get("decision").asText());
        assertTrue(body(retry).get("mfaSatisfied").asBoolean());

        MvcResult replay = access(t, 2, "READ", "phone-byod-02", 14, mfaToken);   // single use
        assertEquals("MFA_REQUIRED", body(replay).get("decision").asText());
    }

    @Test
    void wrongMfaCodeIs401() throws Exception {
        String t = login("dev", "Passw0rd!");
        String challenge = body(access(t, 2, "READ", "phone-byod-02", 14, null)).get("challengeId").asText();
        mvc.perform(post("/api/auth/mfa/verify").header("Authorization", "Bearer " + t)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"challengeId\":\"" + challenge + "\",\"code\":\"000000\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void failedLoginsRaiseTheRiskAndHighRiskRequestsAreDenied() throws Exception {
        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"analyst\",\"password\":\"wrong-wrong\"}")).andExpect(status().isUnauthorized());
        }
        String t = login("analyst", "Passw0rd!");
        // unknown device 15 + HIGH 30 + 19:00 7 + three failed logins 15 = 67
        MvcResult r = access(t, 2, "READ", "unregistered-device", 19, null);
        assertEquals(403, r.getResponse().getStatus());
        assertEquals("DENY", body(r).get("decision").asText());
        assertEquals(67, body(r).get("score").asInt());
    }

    @Test
    void missingPolicyIsDeniedWithoutAScore() throws Exception {
        String t = login("auditor", "Passw0rd!");
        MvcResult r = access(t, 1, "WRITE", "laptop-managed-01", 11, null);
        assertEquals(403, r.getResponse().getStatus());
        assertTrue(body(r).get("reason").asText().startsWith("NO_POLICY"));
        assertEquals(0, body(r).get("score").asInt());
    }

    @Test
    void auditEndpointIsRestrictedToAdminAndAuditor() throws Exception {
        mvc.perform(get("/api/audit").header("Authorization", "Bearer " + login("dev", "Passw0rd!")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/audit").header("Authorization", "Bearer " + login("auditor", "Passw0rd!")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void adminEndpointsRequireAdminRole() throws Exception {
        mvc.perform(get("/api/admin/risk-config").header("Authorization", "Bearer " + login("dev", "Passw0rd!")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/risk-config").header("Authorization", "Bearer " + login("admin", "Passw0rd!")))
                .andExpect(status().isOk()).andExpect(jsonPath("$['threshold.mfa_max']").value(60));
    }

    @Test
    void prometheusEndpointExposesTrustFlowMetrics() throws Exception {
        String text = mvc.perform(get("/actuator/prometheus")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(text.contains("trustflow_decisions_total"));
        assertTrue(text.contains("trustflow_failed_logins_total"));
    }
}
