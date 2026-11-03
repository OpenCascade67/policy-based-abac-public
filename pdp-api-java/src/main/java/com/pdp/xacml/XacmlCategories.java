package com.pdp.xacml;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * XACML 3.0 attribute categories, their JSON Profile shorthand names, and the keys used for them in the
 * evaluator's native request ({@code subject}, {@code resource}, {@code action}, {@code environment}).
 */
final class XacmlCategories {

    static final String ACCESS_SUBJECT = "urn:oasis:names:tc:xacml:1.0:subject-category:access-subject";
    static final String RECIPIENT_SUBJECT = "urn:oasis:names:tc:xacml:1.0:subject-category:recipient-subject";
    static final String INTERMEDIARY_SUBJECT = "urn:oasis:names:tc:xacml:1.0:subject-category:intermediary-subject";
    static final String CODEBASE = "urn:oasis:names:tc:xacml:1.0:subject-category:codebase";
    static final String REQUESTING_MACHINE = "urn:oasis:names:tc:xacml:1.0:subject-category:requesting-machine";
    static final String RESOURCE = "urn:oasis:names:tc:xacml:3.0:attribute-category:resource";
    static final String ACTION = "urn:oasis:names:tc:xacml:3.0:attribute-category:action";
    static final String ENVIRONMENT = "urn:oasis:names:tc:xacml:3.0:attribute-category:environment";

    /** JSON Profile shorthand member name -> category id. */
    static final Map<String, String> SHORTHANDS = shorthands();

    private static final Map<String, String> NATIVE_KEYS = Map.of(
            ACCESS_SUBJECT, "subject",
            RECIPIENT_SUBJECT, "recipientSubject",
            INTERMEDIARY_SUBJECT, "intermediarySubject",
            CODEBASE, "codebase",
            REQUESTING_MACHINE, "requestingMachine",
            RESOURCE, "resource",
            ACTION, "action",
            ENVIRONMENT, "environment");

    private XacmlCategories() {
    }

    /** The native request key for a category; custom categories keep their id. */
    static String nativeKey(String categoryId) {
        return NATIVE_KEYS.getOrDefault(categoryId, categoryId);
    }

    private static Map<String, String> shorthands() {
        Map<String, String> shorthands = new LinkedHashMap<>();
        shorthands.put("AccessSubject", ACCESS_SUBJECT);
        shorthands.put("Action", ACTION);
        shorthands.put("Resource", RESOURCE);
        shorthands.put("Environment", ENVIRONMENT);
        shorthands.put("RecipientSubject", RECIPIENT_SUBJECT);
        shorthands.put("IntermediarySubject", INTERMEDIARY_SUBJECT);
        shorthands.put("Codebase", CODEBASE);
        shorthands.put("RequestingMachine", REQUESTING_MACHINE);
        return Map.copyOf(shorthands);
    }
}
