package com.pap.domain.model;

/** XACML 3.0 rule-combining algorithms supported by the editor. */
public enum RuleCombiningAlgorithm {
    PERMIT_OVERRIDES("permit-overrides"),
    DENY_OVERRIDES("deny-overrides"),
    FIRST_APPLICABLE("first-applicable");

    private static final String URN_PREFIX = "urn:oasis:names:tc:xacml:3.0:rule-combining-algorithm:";

    private final String xacmlName;

    RuleCombiningAlgorithm(String xacmlName) {
        this.xacmlName = xacmlName;
    }

    /** Short XACML identifier, e.g. {@code "deny-overrides"}. */
    public String xacmlName() {
        return xacmlName;
    }

    public String urn() {
        return URN_PREFIX + xacmlName;
    }

    /** Parses a short XACML identifier; anything unrecognised means permit-overrides. */
    public static RuleCombiningAlgorithm fromXacmlName(String name) {
        for (RuleCombiningAlgorithm algorithm : values()) {
            if (algorithm.xacmlName.equals(name)) return algorithm;
        }
        return PERMIT_OVERRIDES;
    }
}
