package com.pdp.domain.model;

import java.util.List;

/** XACML 3.0-aligned policy body. */
public record PolicyDefinition(
        String version,
        String policyId,
        RuleCombiningAlgorithm ruleCombiningAlgorithm,
        List<PolicyRule> rules,
        List<PolicyRuleReference> ruleReferences,
        List<Obligation> obligations) {

    public static final String XACML_VERSION = "3.0";

    public PolicyDefinition {
        version = version == null ? XACML_VERSION : version;
        policyId = policyId == null ? "" : policyId;
        ruleCombiningAlgorithm = ruleCombiningAlgorithm == null ? RuleCombiningAlgorithm.PERMIT_OVERRIDES : ruleCombiningAlgorithm;
        rules = rules == null ? List.of() : rules;
    }
}
