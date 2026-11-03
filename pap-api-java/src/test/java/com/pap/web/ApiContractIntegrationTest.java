package com.pap.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Path;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Verifies the HTTP contract the React UI relies on (shared with the .NET implementation). */
@SpringBootTest
@AutoConfigureMockMvc
class ApiContractIntegrationTest {

    @TempDir
    static Path dataFolder;

    @DynamicPropertySource
    static void dataStore(DynamicPropertyRegistry registry) {
        registry.add("pap.data-store.folder", () -> dataFolder.toString());
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    private String token;

    @BeforeEach
    void signIn() throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.userType").value("local"))
                .andExpect(jsonPath("$.currentDomainId").value("00000000-0000-0000-0000-000000000001"))
                .andReturn().getResponse().getContentAsString();
        token = mapper.readTree(body).get("accessToken").asText();
    }

    @Test
    void rejectsMissingTokenWithBearerChallenge() throws Exception {
        mvc.perform(get("/api/domains"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(content().string(""));
        mvc.perform(get("/api/domains").header("Authorization", "Bearer not.a.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer error=\"invalid_token\""));
    }

    @Test
    void rejectsWrongPasswordAsForbiddenProblem() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"wrong\"}"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://httpstatuses.com/403"))
                .andExpect(jsonPath("$.title").value("Invalid username or password."));
    }

    @Test
    void reportsMissingRequiredFieldsLikeModelValidation() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"admin\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("One or more validation errors occurred."))
                .andExpect(jsonPath("$.errors.Password[0]").value("The Password field is required."))
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    void createsDomainWithLocationAndValidatesInput() throws Exception {
        mvc.perform(post("/api/domains").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Contract Test\",\"description\":\"d\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("http://localhost/api/domains/")))
                .andExpect(jsonPath("$.status").value("active"));

        mvc.perform(post("/api/domains").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\",\"status\":\"weird\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors['Request.Name'][0]").value("'Request Name' must not be empty."))
                .andExpect(jsonPath("$.errors['Request.Status'][0]").value("The specified condition was not met for 'Request Status'."));
    }

    @Test
    void returnsNotFoundForNonGuidIds() throws Exception {
        mvc.perform(get("/api/domains/not-a-guid").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));
    }

    @Test
    void storesPolicyAndRunsTestCase() throws Exception {
        String policy = """
                {"name":"Admins","description":"desc <&>","policyDefinition":{"ruleCombiningAlgorithm":"deny-overrides",
                 "rules":[{"ruleId":"r1","effect":"Permit","target":{"subjects":[{"attributeId":"role","matchFunction":"Equals","value":"admin"}]}}]}}
                """;
        JsonNode created = json(mvc.perform(post("/api/policies").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(policy))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.policyDefinition.ruleCombiningAlgorithm").value("deny-overrides"))
                .andExpect(jsonPath("$.policyDefinition.rules[0].effect").value("Permit"))
                .andExpect(jsonPath("$.policyDefinition.rules[0].target.subjects[0].matchFunction").value("Equals"))
                .andReturn().getResponse().getContentAsString());
        String policyId = created.get("id").asText();

        mvc.perform(get("/api/policies/" + policyId + "/export/json").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=Admins.json; filename*=UTF-8''Admins.json"))
                .andExpect(content().string(containsString("\"Description\": \"desc \\u003C\\u0026\\u003E\"")));

        JsonNode testCase = json(mvc.perform(post("/api/test-cases").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"T\",\"description\":\"\",\"policyId\":\"" + policyId + "\",\"inputData\":\"{}\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        mvc.perform(post("/api/test-cases/" + testCase.get("id").asText() + "/run").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"inputData\":\"{\\\"subject\\\":{\\\"role\\\":\\\"admin\\\"}}\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("Permit"))
                .andExpect(jsonPath("$.matchedPolicies[0]").value("Admins"));
    }

    @Test
    void keepsSettingsPerUser() throws Exception {
        mvc.perform(get("/api/settings").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.theme").exists());
        mvc.perform(put("/api/settings").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"theme\":\"dark\",\"colorScheme\":\"green\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.theme").value("dark"));
    }

    private JsonNode json(String body) throws Exception {
        return mapper.readTree(body);
    }
}
