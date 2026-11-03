package com.pdp.xacml;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pdp.common.exception.BadRequestException;
import com.pdp.xacml.XacmlModel.ParsedRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class XacmlRequestParserTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final XacmlRequestParser parser = new XacmlRequestParser();

    @Test
    void mapsShorthandCategoriesToNativeRequest() throws Exception {
        ParsedRequest request = parse("""
                {"Request": {
                  "ReturnPolicyIdList": true,
                  "AccessSubject": {"Attribute": [
                    {"AttributeId": "urn:oasis:names:tc:xacml:1.0:subject:subject-id", "Value": "alice", "IncludeInResult": true},
                    {"AttributeId": "role", "Value": ["admin", "user"]}]},
                  "Action": {"Attribute": [{"AttributeId": "id", "Value": "read"}]},
                  "Environment": {"Attribute": [{"AttributeId": "hour", "Value": 9}]}}}
                """);

        assertThat(request.returnPolicyIdList()).isTrue();
        assertThat(request.individualRequests()).hasSize(1);
        assertThat(request.individualRequests().getFirst().input()).isEqualTo(json("""
                {"subject": {"urn:oasis:names:tc:xacml:1.0:subject:subject-id": "alice", "role": ["admin", "user"],
                             "subject-id": "alice"},
                 "action": {"id": "read"},
                 "environment": {"hour": 9}}
                """));
        assertThat(request.individualRequests().getFirst().includedInResult())
                .singleElement()
                .satisfies(c -> assertThat(c.categoryId()).isEqualTo(XacmlCategories.ACCESS_SUBJECT));
    }

    @Test
    void mergesRepeatedAttributesIntoBags() throws Exception {
        ParsedRequest request = parse("""
                {"Request": {"Category": [{"CategoryId": "urn:oasis:names:tc:xacml:3.0:attribute-category:resource",
                  "Attribute": [{"AttributeId": "tag", "Value": "a"}, {"AttributeId": "tag", "Value": ["b", "c"]}]}]}}
                """);
        assertThat(request.individualRequests().getFirst().input().at("/resource/tag")).isEqualTo(json("[\"a\", \"b\", \"c\"]"));
    }

    @Test
    void explicitAttributeWinsOverDerivedShortName() throws Exception {
        ParsedRequest request = parse("""
                {"Request": {"AccessSubject": {"Attribute": [
                  {"AttributeId": "urn:example:role", "Value": "from-urn"},
                  {"AttributeId": "role", "Value": "explicit"}]}}}
                """);
        assertThat(request.individualRequests().getFirst().input().at("/subject/role").asText()).isEqualTo("explicit");
    }

    @Test
    void expandsRepeatedCategoriesIntoEveryCombination() throws Exception {
        ParsedRequest request = parse("""
                {"Request": {
                  "AccessSubject": {"Attribute": [{"AttributeId": "id", "Value": "alice"}]},
                  "Resource": [{"Attribute": [{"AttributeId": "id", "Value": "doc-1"}]},
                               {"Attribute": [{"AttributeId": "id", "Value": "doc-2"}]}],
                  "Action": [{"Attribute": [{"AttributeId": "id", "Value": "read"}]},
                             {"Attribute": [{"AttributeId": "id", "Value": "write"}]}]}}
                """);
        assertThat(request.individualRequests()).hasSize(4)
                .extracting(r -> r.input().at("/resource/id").asText() + "/" + r.input().at("/action/id").asText())
                .containsExactlyInAnyOrder("doc-1/read", "doc-1/write", "doc-2/read", "doc-2/write");
    }

    @Test
    void buildsRequestsFromMultiRequestReferences() throws Exception {
        ParsedRequest request = parse("""
                {"Request": {
                  "AccessSubject": {"Id": "s1", "Attribute": [{"AttributeId": "id", "Value": "alice"}]},
                  "Resource": [{"Id": "r1", "Attribute": [{"AttributeId": "id", "Value": "doc-1"}]},
                               {"Id": "r2", "Attribute": [{"AttributeId": "id", "Value": "doc-2"}]}],
                  "MultiRequests": {"RequestReference": [{"ReferenceId": ["s1", "r2"]}]}}}
                """);
        assertThat(request.individualRequests()).singleElement()
                .satisfies(r -> assertThat(r.input().at("/resource/id").asText()).isEqualTo("doc-2"));
    }

    @Test
    void rejectsMalformedRequests() {
        assertThatThrownBy(() -> parse("{\"NotARequest\": {}}")).isInstanceOf(XacmlSyntaxException.class);
        assertThatThrownBy(() -> parse("{\"Request\": {\"Category\": [{\"Attribute\": []}]}}"))
                .isInstanceOf(XacmlSyntaxException.class).hasMessageContaining("CategoryId");
        assertThatThrownBy(() -> parse("{\"Request\": {\"Action\": {\"Attribute\": [{\"AttributeId\": \"id\"}]}}}"))
                .isInstanceOf(XacmlSyntaxException.class).hasMessageContaining("Value");
        assertThatThrownBy(() -> parse("""
                {"Request": {"Resource": {"Id": "r1", "Attribute": []},
                 "MultiRequests": {"RequestReference": [{"ReferenceId": ["missing"]}]}}}"""))
                .isInstanceOf(XacmlSyntaxException.class).hasMessageContaining("missing");
    }

    @Test
    void limitsTheNumberOfIndividualDecisions() {
        String request = """
                {"Request": {"Resource": [{"Attribute": []}, {"Attribute": []}],
                             "Action": [{"Attribute": []}, {"Attribute": []}]}}""";
        assertThatThrownBy(() -> parser.parse(json(request), 3)).isInstanceOf(BadRequestException.class);
    }

    private ParsedRequest parse(String body) throws Exception {
        return parser.parse(json(body), 100);
    }

    private JsonNode json(String text) {
        try {
            return mapper.readTree(text);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
}
