package com.pdp.evaluation;

/** An authorization decision; serialized as "Permit", "Deny", "NotApplicable" or "Indeterminate". */
public enum Decision {
    PERMIT, DENY, NOT_APPLICABLE, INDETERMINATE
}
