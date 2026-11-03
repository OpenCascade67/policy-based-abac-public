package com.pdp.domain.model;

/** XACML attribute data types used for obligation attribute assignments. */
public enum AttributeDataType {
    STRING("string"),
    INTEGER("integer"),
    BOOLEAN("boolean"),
    DOUBLE("double"),
    DATE("date"),
    TIME("time"),
    DATE_TIME("dateTime"),
    ANY_URI("anyURI");

    private static final String XML_SCHEMA = "http://www.w3.org/2001/XMLSchema#";

    private final String xmlSchemaName;

    AttributeDataType(String xmlSchemaName) {
        this.xmlSchemaName = xmlSchemaName;
    }

    /** The XACML data type identifier, e.g. {@code http://www.w3.org/2001/XMLSchema#integer}. */
    public String uri() {
        return XML_SCHEMA + xmlSchemaName;
    }
}
