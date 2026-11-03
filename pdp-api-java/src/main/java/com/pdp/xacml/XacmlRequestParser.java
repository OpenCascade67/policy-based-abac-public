package com.pdp.xacml;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pdp.common.exception.BadRequestException;
import com.pdp.xacml.XacmlModel.Attribute;
import com.pdp.xacml.XacmlModel.Category;
import com.pdp.xacml.XacmlModel.IndividualRequest;
import com.pdp.xacml.XacmlModel.ParsedRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Parses XACML 3.0 JSON Profile requests (shorthand categories and the {@code Category} array) and turns each
 * individual decision into the evaluator's native request:
 * <pre>{@code {"subject": {"<AttributeId>": <Value>, ...}, "resource": {...}, "action": {...}, "environment": {...}}}</pre>
 * Attribute ids are kept verbatim; a URN id is also available under its last segment
 * ({@code urn:...:subject:subject-id} as {@code subject-id}) unless the request sets that name explicitly.
 * Repeated attributes and array values form bags. Multiple decisions are supported through repeated category
 * instances (every combination is decided) and through {@code MultiRequests} references.
 */
@Component
public class XacmlRequestParser {

    /** One occurrence of a category in the request. */
    private record CategoryInstance(String categoryId, String id, List<Attribute> attributes) {
    }

    private static final JsonNodeFactory NODES = JsonNodeFactory.instance;

    /**
     * @param maxIndividualRequests upper bound on the decisions one request may expand to
     * @throws XacmlSyntaxException if the body is not a valid XACML JSON request
     * @throws BadRequestException  if the request expands to more than {@code maxIndividualRequests} decisions
     */
    public ParsedRequest parse(JsonNode body, int maxIndividualRequests) {
        JsonNode request = member(body, "Request")
                .filter(JsonNode::isObject)
                .orElseThrow(() -> new XacmlSyntaxException("The body must be an object with a 'Request' member."));

        List<CategoryInstance> instances = new ArrayList<>();
        member(request, "Category").ifPresent(c -> elements(c).forEach(node ->
                instances.add(category(node, member(node, "CategoryId").map(JsonNode::asText).orElse(null)))));
        XacmlCategories.SHORTHANDS.forEach((shorthand, categoryId) ->
                member(request, shorthand).ifPresent(c -> elements(c).forEach(node -> instances.add(category(node, categoryId)))));

        List<List<CategoryInstance>> combinations = member(request, "MultiRequests")
                .map(multi -> referencedCombinations(multi, instances))
                .orElseGet(() -> repeatedCombinations(instances, maxIndividualRequests));
        if (combinations.size() > maxIndividualRequests) {
            throw new BadRequestException("A request may contain at most %d decisions; %d were requested."
                    .formatted(maxIndividualRequests, combinations.size()));
        }

        return new ParsedRequest(
                bool(request, "ReturnPolicyIdList"),
                bool(request, "CombinedDecision"),
                combinations.stream().map(XacmlRequestParser::toIndividualRequest).toList());
    }

    private static CategoryInstance category(JsonNode node, String categoryId) {
        if (!node.isObject()) throw new XacmlSyntaxException("Each category must be a JSON object.");
        if (categoryId == null || categoryId.isBlank()) throw new XacmlSyntaxException("Each Category must have a CategoryId.");
        List<Attribute> attributes = new ArrayList<>();
        member(node, "Attribute").ifPresent(a -> elements(a).forEach(attribute -> attributes.add(attribute(attribute))));
        return new CategoryInstance(categoryId, member(node, "Id").map(JsonNode::asText).orElse(null), attributes);
    }

    private static Attribute attribute(JsonNode node) {
        if (!node.isObject()) throw new XacmlSyntaxException("Each Attribute must be a JSON object.");
        String attributeId = member(node, "AttributeId").filter(JsonNode::isTextual).map(JsonNode::asText)
                .filter(id -> !id.isBlank())
                .orElseThrow(() -> new XacmlSyntaxException("Each Attribute must have an AttributeId."));
        JsonNode value = member(node, "Value")
                .orElseThrow(() -> new XacmlSyntaxException("Attribute '%s' must have a Value.".formatted(attributeId)));
        return new Attribute(attributeId, value,
                member(node, "DataType").map(JsonNode::asText).orElse(null),
                member(node, "Issuer").map(JsonNode::asText).orElse(null),
                bool(node, "IncludeInResult"));
    }

    /** Multiple Decision Profile §2.3: one decision per combination of repeated category instances. */
    private static List<List<CategoryInstance>> repeatedCombinations(List<CategoryInstance> instances, int limit) {
        Map<String, List<CategoryInstance>> byCategory = new LinkedHashMap<>();
        instances.forEach(i -> byCategory.computeIfAbsent(i.categoryId(), k -> new ArrayList<>()).add(i));

        long total = 1;
        for (List<CategoryInstance> group : byCategory.values()) {
            total *= group.size();
            if (total > limit) {
                throw new BadRequestException("A request may contain at most %d decisions; more were requested.".formatted(limit));
            }
        }

        List<List<CategoryInstance>> combinations = new ArrayList<>();
        combinations.add(List.of());
        for (List<CategoryInstance> group : byCategory.values()) {
            List<List<CategoryInstance>> next = new ArrayList<>();
            for (List<CategoryInstance> partial : combinations) {
                for (CategoryInstance instance : group) {
                    List<CategoryInstance> extended = new ArrayList<>(partial);
                    extended.add(instance);
                    next.add(extended);
                }
            }
            combinations = next;
        }
        return combinations;
    }

    /** Multiple Decision Profile §2.4: one decision per RequestReference, built from the referenced instances. */
    private static List<List<CategoryInstance>> referencedCombinations(JsonNode multiRequests, List<CategoryInstance> instances) {
        Map<String, CategoryInstance> byId = new LinkedHashMap<>();
        instances.stream().filter(i -> i.id() != null).forEach(i -> byId.put(i.id(), i));

        JsonNode references = member(multiRequests, "RequestReference")
                .orElseThrow(() -> new XacmlSyntaxException("MultiRequests must contain RequestReference entries."));
        List<List<CategoryInstance>> combinations = new ArrayList<>();
        for (JsonNode reference : elements(references)) {
            List<CategoryInstance> combination = new ArrayList<>();
            JsonNode ids = member(reference, "ReferenceId")
                    .orElseThrow(() -> new XacmlSyntaxException("Each RequestReference must contain ReferenceId values."));
            for (JsonNode id : elements(ids)) {
                CategoryInstance instance = byId.get(id.asText());
                if (instance == null) throw new XacmlSyntaxException("ReferenceId '%s' does not match any category Id.".formatted(id.asText()));
                combination.add(instance);
            }
            combinations.add(combination);
        }
        return combinations;
    }

    private static IndividualRequest toIndividualRequest(List<CategoryInstance> instances) {
        ObjectNode input = NODES.objectNode();
        Map<String, List<Attribute>> included = new LinkedHashMap<>();
        for (CategoryInstance instance : instances) {
            ObjectNode category = input.withObjectProperty(XacmlCategories.nativeKey(instance.categoryId()));
            instance.attributes().forEach(a -> addValue(category, a.attributeId(), a.value()));
            instance.attributes().stream()
                    .filter(a -> Boolean.TRUE.equals(a.includeInResult()))
                    .forEach(a -> included.computeIfAbsent(instance.categoryId(), k -> new ArrayList<>()).add(a));
        }
        // Aliases go in last so an explicitly sent attribute always wins over a derived short name
        for (CategoryInstance instance : instances) {
            ObjectNode category = (ObjectNode) input.get(XacmlCategories.nativeKey(instance.categoryId()));
            instance.attributes().forEach(a -> addShortNameAlias(category, a));
        }
        List<Category> echoed = included.entrySet().stream().map(e -> new Category(e.getKey(), e.getValue())).toList();
        return new IndividualRequest(input, echoed);
    }

    /** Repeated attribute ids accumulate into a bag (JSON array). */
    private static void addValue(ObjectNode category, String attributeId, JsonNode value) {
        JsonNode existing = category.get(attributeId);
        if (existing == null) {
            category.set(attributeId, value.deepCopy());
            return;
        }
        ArrayNode bag = existing.isArray() ? (ArrayNode) existing : NODES.arrayNode().add(existing);
        if (value.isArray()) bag.addAll((ArrayNode) value.deepCopy());
        else bag.add(value.deepCopy());
        category.set(attributeId, bag);
    }

    private static void addShortNameAlias(ObjectNode category, Attribute attribute) {
        String id = attribute.attributeId();
        int separator = id.lastIndexOf(':');
        if (separator < 0 || separator == id.length() - 1) return;
        String shortName = id.substring(separator + 1);
        if (!category.has(shortName)) category.set(shortName, category.get(id).deepCopy());
    }

    private static List<JsonNode> elements(JsonNode node) {
        if (!node.isArray()) return List.of(node);
        List<JsonNode> items = new ArrayList<>();
        node.forEach(items::add);
        return items;
    }

    private static boolean bool(JsonNode node, String name) {
        return member(node, name).map(JsonNode::asBoolean).orElse(false);
    }

    /** JSON Profile member names are PascalCase; lookups also accept other casings. */
    private static Optional<JsonNode> member(JsonNode node, String name) {
        if (node == null || !node.isObject()) return Optional.empty();
        JsonNode exact = node.get(name);
        if (exact != null && !exact.isNull()) return Optional.of(exact);
        return node.properties().stream()
                .filter(p -> p.getKey().equalsIgnoreCase(name) && !p.getValue().isNull())
                .map(Map.Entry::getValue)
                .findFirst();
    }
}
