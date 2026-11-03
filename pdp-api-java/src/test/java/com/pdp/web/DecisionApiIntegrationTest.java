package com.pdp.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** End-to-end decisions against PAP-format data files in a temporary folder. */
@SpringBootTest
@AutoConfigureMockMvc
class DecisionApiIntegrationTest {

    private static final String DEFAULT_DOMAIN = "00000000-0000-0000-0000-000000000001";
    private static final String INACTIVE_DOMAIN = "22222222-2222-2222-2222-222222222222";
    private static final String UNKNOWN_DOMAIN = "33333333-3333-3333-3333-333333333333";
    private static final String ADMIN_POLICY_ID = "aaaaaaaa-0000-0000-0000-000000000001";

    private static final String FULL_KEY = "full-access-key-0123456789";
    private static final String RESTRICTED_KEY = "restricted-key-0123456789";

    /** PAP file format: camelCase enum values; policies without a domainId belong to the default domain. */
    private static final String POLICIES = """
            [
              {"id": "%s", "name": "Admin Access", "description": "",
               "policyDefinition": {"version": "3.0", "policyId": "%s", "ruleCombiningAlgorithm": "denyOverrides",
                 "rules": [{"ruleId": "r1", "effect": "permit",
                            "target": {"subjects": [{"attributeId": "role", "matchFunction": "equals", "value": "admin"}]}}],
                 "obligations": [{"obligationId": "audit-log", "fulfillOn": "permit",
                                  "attributeAssignments": [{"attributeId": "level", "value": 1, "dataType": "integer"}]}]}},
              {"id": "aaaaaaaa-0000-0000-0000-000000000002", "domainId": "%s", "name": "Block Suspended",
               "policyDefinition": {"ruleCombiningAlgorithm": "firstApplicable",
                 "rules": [{"ruleId": "r2", "effect": "deny",
                            "condition": {"operator": "AND",
                              "conditions": [{"attributeId": "subject.suspended", "matchFunction": "equals", "value": true}]}}]}}
            ]""".formatted(ADMIN_POLICY_ID, ADMIN_POLICY_ID, DEFAULT_DOMAIN);

    @TempDir
    static Path dataFolder;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        write("domains.json", """
                [{"id": "%s", "name": "Default Domain", "status": "active"},
                 {"id": "%s", "name": "Retired", "status": "inactive"}]""".formatted(DEFAULT_DOMAIN, INACTIVE_DOMAIN));
        write("policies.json", POLICIES);

        registry.add("pdp.policy-store.folder", () -> dataFolder.toString());
        registry.add("pdp.policy-store.refresh-interval", () -> "0s");
        registry.add("pdp.max-batch-size", () -> "3");
        registry.add("pdp.clients[0].name", () -> "full");
        registry.add("pdp.clients[0].api-key", () -> FULL_KEY);
        registry.add("pdp.clients[1].name", () -> "restricted");
        registry.add("pdp.clients[1].api-key", () -> RESTRICTED_KEY);
        registry.add("pdp.clients[1].domains[0]", () -> INACTIVE_DOMAIN);
    }

    @Autowired
    private MockMvc mvc;

    @Test
    void requiresApiKey() throws Exception {
        mvc.perform(post(decisionsUrl(DEFAULT_DOMAIN)).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "ApiKey header=\"X-API-Key\""))
                .andExpect(jsonPath("$.status").value(401));
        mvc.perform(post(decisionsUrl(DEFAULT_DOMAIN)).header("X-API-Key", "wrong-key-0000000000")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void permitsWithObligations() throws Exception {
        decide(DEFAULT_DOMAIN, """
                {"subject": {"role": "admin"}, "action": {"id": "read"}}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("Permit"))
                .andExpect(jsonPath("$.matchedPolicies", contains("Admin Access")))
                .andExpect(jsonPath("$.obligations[0].obligationId").value("audit-log"))
                .andExpect(jsonPath("$.obligations[0].fulfillOn").value("Permit"));
    }

    @Test
    void denyOverridesAcrossPolicies() throws Exception {
        decide(DEFAULT_DOMAIN, """
                {"subject": {"role": "admin", "suspended": true}}""")
                .andExpect(jsonPath("$.decision").value("Deny"))
                .andExpect(jsonPath("$.matchedPolicies", contains("Block Suspended")))
                .andExpect(jsonPath("$.obligations", hasSize(0)));
    }

    @Test
    void canRestrictEvaluationToOnePolicy() throws Exception {
        mvc.perform(post(decisionsUrl(DEFAULT_DOMAIN) + "?policyId=" + ADMIN_POLICY_ID).header("X-API-Key", FULL_KEY)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"subject\": {\"role\": \"admin\", \"suspended\": true}}"))
                .andExpect(jsonPath("$.decision").value("Permit"));
        mvc.perform(post(decisionsUrl(DEFAULT_DOMAIN) + "?policyId=" + UNKNOWN_DOMAIN).header("X-API-Key", FULL_KEY)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void decidesBatchesWithinTheLimit() throws Exception {
        mvc.perform(post(decisionsUrl(DEFAULT_DOMAIN) + "/batch").header("X-API-Key", FULL_KEY).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requests\": [{\"subject\": {\"role\": \"admin\"}}, {\"subject\": {\"role\": \"guest\"}}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[*].decision", contains("Permit", "NotApplicable")));
        mvc.perform(post(decisionsUrl(DEFAULT_DOMAIN) + "/batch").header("X-API-Key", FULL_KEY).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requests\": [{}, {}, {}, {}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void rejectsBodiesThatAreNotRequests() throws Exception {
        decide(DEFAULT_DOMAIN, "[1, 2]").andExpect(status().isBadRequest());
        decide(DEFAULT_DOMAIN, "{not json").andExpect(status().isBadRequest());
    }

    @Test
    void enforcesDomainRules() throws Exception {
        decide(UNKNOWN_DOMAIN, "{}").andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Domain with id '" + UNKNOWN_DOMAIN + "' was not found."));
        decide(INACTIVE_DOMAIN, "{}").andExpect(status().isForbidden());
        mvc.perform(post(decisionsUrl(DEFAULT_DOMAIN)).header("X-API-Key", RESTRICTED_KEY)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/domains/not-a-guid/decisions").header("X-API-Key", FULL_KEY)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void answersXacmlJsonProfileRequests() throws Exception {
        xacml("""
                {"Request": {
                  "ReturnPolicyIdList": true,
                  "AccessSubject": {"Attribute": [
                    {"AttributeId": "urn:oasis:names:tc:xacml:1.0:subject:role", "Value": "admin", "IncludeInResult": true}]},
                  "Action": {"Attribute": [{"AttributeId": "urn:oasis:names:tc:xacml:1.0:action:action-id", "Value": "read"}]}}}
                """)
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/xacml+json"))
                .andExpect(jsonPath("$.Response", hasSize(1)))
                .andExpect(jsonPath("$.Response[0].Decision").value("Permit"))
                .andExpect(jsonPath("$.Response[0].Status.StatusCode.Value").value("urn:oasis:names:tc:xacml:1.0:status:ok"))
                .andExpect(jsonPath("$.Response[0].Obligations[0].Id").value("audit-log"))
                .andExpect(jsonPath("$.Response[0].Obligations[0].AttributeAssignment[0].DataType")
                        .value("http://www.w3.org/2001/XMLSchema#integer"))
                .andExpect(jsonPath("$.Response[0].PolicyIdentifierList.PolicyIdReference[0].Id").value(ADMIN_POLICY_ID))
                .andExpect(jsonPath("$.Response[0].Category[0].Attribute[0].Value").value("admin"));
    }

    @Test
    void answersXacmlMultipleDecisionRequests() throws Exception {
        xacml("""
                {"Request": {"Category": [
                  {"CategoryId": "urn:oasis:names:tc:xacml:1.0:subject-category:access-subject",
                   "Attribute": [{"AttributeId": "role", "Value": "admin"}]},
                  {"CategoryId": "urn:oasis:names:tc:xacml:1.0:subject-category:access-subject",
                   "Attribute": [{"AttributeId": "role", "Value": "guest"}]}]}}
                """)
                .andExpect(jsonPath("$.Response[*].Decision", contains("Permit", "NotApplicable")));
    }

    @Test
    void reportsXacmlSyntaxErrorsAsIndeterminate() throws Exception {
        xacml("{\"Request\": {\"Action\": {\"Attribute\": [{\"Value\": \"read\"}]}}}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.Response[0].Decision").value("Indeterminate"))
                .andExpect(jsonPath("$.Response[0].Status.StatusCode.Value").value("urn:oasis:names:tc:xacml:1.0:status:syntax-error"));
        xacml("{\"Request\": {\"CombinedDecision\": true}}")
                .andExpect(jsonPath("$.Response[0].Status.StatusCode.Value").value("urn:oasis:names:tc:xacml:1.0:status:processing-error"));
    }

    @Test
    void picksUpPolicyChangesWithoutRestart() throws Exception {
        String guestPolicy = """
                ,{"id": "aaaaaaaa-0000-0000-0000-000000000003", "name": "Guests Read",
                  "policyDefinition": {"ruleCombiningAlgorithm": "permitOverrides",
                    "rules": [{"ruleId": "g", "effect": "permit",
                               "target": {"subjects": [{"attributeId": "role", "matchFunction": "equals", "value": "guest"}]}}]}}
                ]""";
        try {
            write("policies.json", POLICIES.substring(0, POLICIES.lastIndexOf(']')) + guestPolicy);
            decide(DEFAULT_DOMAIN, "{\"subject\": {\"role\": \"guest\"}}")
                    .andExpect(jsonPath("$.decision").value("Permit"))
                    .andExpect(jsonPath("$.matchedPolicies", contains("Guests Read")));
        } finally {
            write("policies.json", POLICIES);
        }
    }

    @Test
    void exposesPublicHealth() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components.policyStore.details.domains").value(2));
    }

    private ResultActions decide(String domainId, String body) throws Exception {
        return mvc.perform(post(decisionsUrl(domainId)).header("X-API-Key", FULL_KEY)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions xacml(String body) throws Exception {
        return mvc.perform(post("/api/v1/domains/" + DEFAULT_DOMAIN + "/xacml/decisions").header("X-API-Key", FULL_KEY)
                .contentType("application/xacml+json").accept("application/xacml+json").content(body));
    }

    private static String decisionsUrl(String domainId) {
        return "/api/v1/domains/" + domainId + "/decisions";
    }

    private static void write(String fileName, String content) {
        try {
            Files.writeString(dataFolder.resolve(fileName), content);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
