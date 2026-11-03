package com.pap.web.dto.policy;

import com.pap.domain.model.Obligation;
import com.pap.domain.model.PolicyDefinition;
import com.pap.domain.model.PolicyRule;
import com.pap.domain.model.PolicyRuleReference;
import com.pap.domain.model.RuleCombiningAlgorithm;

import java.util.List;

/** Policy body as exchanged with the UI; the combining algorithm uses its XACML short name. */
public record PolicyDefinitionDto(
        String version,
        String policyId,
        String ruleCombiningAlgorithm,
        List<PolicyRule> rules,
        List<PolicyRuleReference> ruleReferences,
        List<Obligation> obligations) {

    public PolicyDefinitionDto {
        version = version == null ? PolicyDefinition.XACML_VERSION : version;
        policyId = policyId == null ? "" : policyId;
        ruleCombiningAlgorithm = ruleCombiningAlgorithm == null ? "" : ruleCombiningAlgorithm;
        rules = rules == null ? List.of() : rules;
    }

    public static PolicyDefinitionDto empty() {
        return new PolicyDefinitionDto(null, null, null, null, null, null);
    }

    public static PolicyDefinitionDto from(PolicyDefinition definition) {
        return new PolicyDefinitionDto(
                definition.version(),
                definition.policyId(),
                definition.ruleCombiningAlgorithm().xacmlName(),
                definition.rules(),
                definition.ruleReferences(),
                definition.obligations());
    }

    /** The stored definition always carries version 3.0 and the owning policy's id. */
    public PolicyDefinition toDefinition(String owningPolicyId) {
        return new PolicyDefinition(
                PolicyDefinition.XACML_VERSION,
                owningPolicyId,
                RuleCombiningAlgorithm.fromXacmlName(ruleCombiningAlgorithm),
                rules,
                ruleReferences,
                obligations);
    }
}
