package com.pdp.xacml;

/** The body is JSON but not a valid XACML JSON Profile request; reported as Indeterminate with syntax-error. */
class XacmlSyntaxException extends RuntimeException {

    XacmlSyntaxException(String message) {
        super(message);
    }
}
