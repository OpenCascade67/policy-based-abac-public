package com.pdp.evaluation;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.pdp.domain.model.AttributeMatch;
import com.pdp.domain.model.MatchFunction;
import com.pdp.domain.model.Policy;
import com.pdp.domain.model.PolicyTarget;
import com.pdp.domain.model.RuleEffect;
import com.pdp.domain.model.SharedRule;
import com.pdp.json.EnumNamingModule;
import com.pdp.json.JsonSupport;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** The same scenarios the PAP's evaluator is tested with, so both evaluators stay behaviourally identical. */
class PolicyEvaluatorTest {

    private static final ObjectMapper API_MAPPER = JsonMapper.builder()
            .addModule(new EnumNamingModule(EnumNamingModule.Style.PASCAL))
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .build();
    private static final ObjectMapper STORAGE_MAPPER = JsonSupport.storageMapper();

    private final PolicyEvaluator evaluator = new PolicyEvaluator(API_MAPPER);

    private static final Policy FINANCE = policy("Finance Read", """
            { "policyDefinition": { "ruleCombiningAlgorithm": "denyOverrides", "rules": [
              { "ruleId": "r1", "effect": "permit",
                "target": { "subjects": [ { "attributeId": "department", "matchFunction": "equals", "value": "finance" } ],
                            "actions":  [ { "attributeId": "id", "matchFunction": "equals", "value": "read" } ] },
                "condition": { "operator": "AND", "conditions": [
                    { "attributeId": "subject.clearance", "matchFunction": "greaterThan", "value": 2 },
                    { "operator": "OR", "conditions": [
                        { "attributeId": "roles", "matchFunction": "contains", "value": "analyst" },
                        { "attributeId": "subject.isManager", "matchFunction": "equals", "value": true } ] } ] } },
              { "ruleId": "r2", "effect": "deny",
                "condition": { "operator": "AND", "conditions": [
                    { "attributeId": "environment.time", "matchFunction": "greaterThan", "value": "2026-01-01T18:00:00Z" } ] } } ],
              "obligations": [ { "obligationId": "log", "fulfillOn": "permit" }, { "obligationId": "alert", "fulfillOn": "deny" } ] } }
            """);

    private static final Policy ALLOW_DOCS = policy("Docs Permit", """
            { "policyDefinition": { "ruleCombiningAlgorithm": "firstApplicable", "rules": [ { "ruleId": "a", "effect": "permit",
              "target": { "resources": [ { "attributeId": "type", "matchFunction": "equals", "value": "doc" } ] } } ] } }
            """);

    private static final Policy DENY_CONTRACTORS = policy("Contractor Deny", """
            { "policyDefinition": { "ruleCombiningAlgorithm": "firstApplicable", "rules": [ { "ruleId": "b", "effect": "deny",
              "target": { "subjects": [ { "attributeId": "employment.type", "matchFunction": "equals", "value": "contractor" } ] } } ] } }
            """);

    @Test
    void permitsWhenTargetAndConditionMatch() {
        var result = evaluate(List.of(FINANCE), financeRequest("""
                { "department": "Finance", "clearance": 3, "roles": ["analyst", "user"] }"""));
        assertThat(result.decision()).isEqualTo(Decision.PERMIT);
        assertThat(result.matchedPolicies()).extracting(Policy::name).containsExactly("Finance Read");
        assertThat(result.obligations()).extracting(o -> o.obligationId()).containsExactly("log");
    }

    @Test
    void notApplicableWhenConditionIsFalse() {
        var result = evaluate(List.of(FINANCE), financeRequest("""
                { "department": "finance", "clearance": 2, "roles": ["analyst"] }"""));
        assertThat(result.decision()).isEqualTo(Decision.NOT_APPLICABLE);
        assertThat(result.matchedPolicies()).isEmpty();
    }

    @Test
    void orBranchMatchesBooleanAndNumericString() {
        var result = evaluate(List.of(FINANCE), financeRequest("""
                { "department": "finance", "clearance": "5", "roles": [], "isManager": true }"""));
        assertThat(result.decision()).isEqualTo(Decision.PERMIT);
    }

    @Test
    void containsOnArrayMeansMembership() {
        var result = evaluate(List.of(FINANCE), financeRequest("""
                { "department": "finance", "clearance": 3, "roles": ["senior-analyst"] }"""));
        assertThat(result.decision()).isEqualTo(Decision.NOT_APPLICABLE);
    }

    @Test
    void denyOverridesAfterHours() {
        var result = evaluate(List.of(FINANCE), financeRequest("""
                { "department": "finance", "clearance": 3, "roles": ["analyst"] }""", "2026-01-01T19:00:00Z"));
        assertThat(result.decision()).isEqualTo(Decision.DENY);
        assertThat(result.obligations()).extracting(o -> o.obligationId()).containsExactly("alert");
    }

    @Test
    void uncomparableValuesAreIndeterminate() {
        var result = evaluate(List.of(FINANCE), financeRequest("""
                { "department": "finance", "clearance": "high", "roles": ["analyst"] }"""));
        assertThat(result.decision()).isEqualTo(Decision.INDETERMINATE);
    }

    @Test
    void nonObjectInputIsIndeterminate() {
        assertThat(evaluator.evaluate(List.of(FINANCE), List.of(), json("[1, 2]")).decision()).isEqualTo(Decision.INDETERMINATE);
    }

    @Test
    void invalidRegexIsIndeterminate() {
        Policy badRegex = policy("Bad Regex", """
                { "policyDefinition": { "ruleCombiningAlgorithm": "permitOverrides", "rules": [ { "ruleId": "x", "effect": "permit",
                  "target": { "resources": [ { "attributeId": "path", "matchFunction": "regex", "value": "([" } ] } } ] } }
                """);
        assertThat(evaluate(List.of(badRegex), """
                { "resource": { "path": "/a" } }""").decision()).isEqualTo(Decision.INDETERMINATE);
    }

    @Test
    void policiesCombineWithDenyOverridesRegardlessOfOrder() {
        String contractor = """
                { "subject": { "employment": { "type": "contractor" } }, "resource": { "type": "doc" } }""";
        assertThat(evaluate(List.of(ALLOW_DOCS, DENY_CONTRACTORS), contractor).decision()).isEqualTo(Decision.DENY);
        assertThat(evaluate(List.of(DENY_CONTRACTORS, ALLOW_DOCS), contractor).matchedPolicies())
                .extracting(Policy::name).containsExactly("Contractor Deny");
    }

    @Test
    void resolvesSharedRuleReferences() {
        UUID sharedId = UUID.randomUUID();
        SharedRule shared = new SharedRule(sharedId, null, "Writers", null, RuleEffect.PERMIT,
                new PolicyTarget(null, null,
                        List.of(new AttributeMatch("id", MatchFunction.EQUALS, API_MAPPER.getNodeFactory().textNode("write"))), null),
                null);
        Policy withRefs = policy("Refs", """
                { "policyDefinition": { "ruleCombiningAlgorithm": "permitOverrides",
                  "ruleReferences": [ { "type": "shared", "sharedRuleId": "%s" } ] } }
                """.formatted(sharedId));
        JsonNode request = json("""
                { "action": { "id": "write" } }""");

        assertThat(evaluator.evaluate(List.of(withRefs), List.of(shared), request).decision()).isEqualTo(Decision.PERMIT);
        assertThat(evaluator.evaluate(List.of(withRefs), List.of(), request).decision()).isEqualTo(Decision.INDETERMINATE);
    }

    private EvaluationResult evaluate(List<Policy> policies, String input) {
        return evaluator.evaluate(policies, List.of(), json(input));
    }

    private static String financeRequest(String subject) {
        return financeRequest(subject, "2026-01-01T09:00:00Z");
    }

    private static String financeRequest(String subject, String time) {
        return """
                { "subject": %s, "action": { "id": "read" }, "environment": { "time": "%s" } }
                """.formatted(subject, time);
    }

    private static JsonNode json(String text) {
        try {
            return API_MAPPER.readTree(text);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static Policy policy(String name, String json) {
        try {
            Policy parsed = STORAGE_MAPPER.readValue(json, Policy.class);
            return new Policy(UUID.randomUUID(), null, name, "", parsed.policyDefinition());
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
}
