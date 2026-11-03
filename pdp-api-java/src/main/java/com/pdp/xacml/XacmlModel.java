package com.pdp.xacml;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.pdp.evaluation.Decision;

import java.util.List;

/** XACML 3.0 JSON Profile (v1.1) response structures, plus the parsed form of a request. */
public final class XacmlModel {

    private XacmlModel() {
    }

    /** A parsed request: one entry per individual decision (several for multiple-decision requests). */
    record ParsedRequest(boolean returnPolicyIdList, boolean combinedDecision, List<IndividualRequest> individualRequests) {
    }

    /**
     * @param input            the request in the evaluator's native form
     * @param includedInResult attributes the client asked to have echoed in the result
     */
    record IndividualRequest(JsonNode input, List<Category> includedInResult) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record Response(@JsonProperty("Response") List<Result> results) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record Result(
            @JsonProperty("Decision") Decision decision,
            @JsonProperty("Status") Status status,
            @JsonProperty("Obligations") List<Obligation> obligations,
            @JsonProperty("Category") List<Category> categories,
            @JsonProperty("PolicyIdentifierList") PolicyIdentifierList policyIdentifierList) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record Status(@JsonProperty("StatusCode") StatusCode statusCode, @JsonProperty("StatusMessage") String statusMessage) {

        static final String OK = "urn:oasis:names:tc:xacml:1.0:status:ok";
        static final String SYNTAX_ERROR = "urn:oasis:names:tc:xacml:1.0:status:syntax-error";
        static final String PROCESSING_ERROR = "urn:oasis:names:tc:xacml:1.0:status:processing-error";

        static Status of(String code, String message) {
            return new Status(new StatusCode(code), message);
        }
    }

    public record StatusCode(@JsonProperty("Value") String value) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record Obligation(@JsonProperty("Id") String id,
                             @JsonProperty("AttributeAssignment") List<AttributeAssignment> attributeAssignments) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AttributeAssignment(@JsonProperty("AttributeId") String attributeId,
                                      @JsonProperty("Value") JsonNode value,
                                      @JsonProperty("DataType") String dataType) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record Category(@JsonProperty("CategoryId") String categoryId, @JsonProperty("Attribute") List<Attribute> attributes) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Attribute(@JsonProperty("AttributeId") String attributeId,
                            @JsonProperty("Value") JsonNode value,
                            @JsonProperty("DataType") String dataType,
                            @JsonProperty("Issuer") String issuer,
                            @JsonProperty("IncludeInResult") Boolean includeInResult) {
    }

    public record PolicyIdentifierList(@JsonProperty("PolicyIdReference") List<IdReference> policyIdReferences) {
    }

    public record IdReference(@JsonProperty("Id") String id, @JsonProperty("Version") String version) {
    }
}
