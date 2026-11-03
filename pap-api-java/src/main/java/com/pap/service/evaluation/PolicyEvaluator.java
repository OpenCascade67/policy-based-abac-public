package com.pap.service.evaluation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.pap.domain.model.AttributeMatch;
import com.pap.domain.model.MatchFunction;
import com.pap.domain.model.Obligation;
import com.pap.domain.model.ObligationFulfillOn;
import com.pap.domain.model.Policy;
import com.pap.domain.model.PolicyCondition;
import com.pap.domain.model.PolicyDefinition;
import com.pap.domain.model.PolicyRule;
import com.pap.domain.model.PolicyRuleReference;
import com.pap.domain.model.PolicyTarget;
import com.pap.domain.model.RuleEffect;
import com.pap.domain.model.SharedRule;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * XACML 3.0-style evaluation of policies against a JSON request of the form
 * {@code {"subject": {...}, "resource": {...}, "action": {...}, "environment": {...}}}.
 * <p>
 * Attribute ids may be dot paths ({@code "subject.department"}); attributes not found in their category are
 * looked up at the root. Array values are bags: any element may match, and {@code Contains} on an array means
 * membership. Processing errors (invalid regex, values that cannot be compared, unresolvable shared rules)
 * make the affected rule Indeterminate rather than silently not matching.
 */
@Component
public class PolicyEvaluator {

    /** Indeterminate{D|P|DP} records which effect an erroring rule or policy could have had (XACML 3.0 §7.11). */
    private enum Decision { PERMIT, DENY, NOT_APPLICABLE, INDETERMINATE_D, INDETERMINATE_P, INDETERMINATE_DP }

    private static final List<String> CATEGORIES = List.of("subject", "resource", "action", "environment");
    private static final Duration REGEX_TIMEOUT = Duration.ofSeconds(1);

    private final ObjectMapper mapper;

    public PolicyEvaluator(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public EvaluationResult evaluate(List<Policy> policies, List<SharedRule> sharedRules, String inputData) {
        JsonNode input;
        try {
            input = mapper.readTree(inputData == null ? "" : inputData);
        } catch (JsonProcessingException e) {
            return EvaluationResult.indeterminate();
        }
        if (input == null || !input.isObject()) return EvaluationResult.indeterminate();

        Map<UUID, SharedRule> sharedById = sharedRules.stream()
                .collect(Collectors.toMap(SharedRule::getId, Function.identity(), (a, b) -> a));
        record Outcome(Policy policy, Decision decision) {
        }
        List<Outcome> outcomes = policies.stream()
                .map(p -> new Outcome(p, evaluatePolicy(p.getPolicyDefinition(), sharedById, input)))
                .toList();

        // Policies are combined with deny-overrides, the safe default when no policy set is defined
        Decision overall = denyOverrides(outcomes.stream().map(Outcome::decision));
        String decision = toResultString(overall);
        List<Outcome> contributing = decision.equals("NotApplicable")
                ? List.of()
                : outcomes.stream().filter(o -> toResultString(o.decision()).equals(decision)).toList();

        List<Obligation> obligations = overall == Decision.PERMIT || overall == Decision.DENY
                ? contributing.stream()
                    .flatMap(o -> Optional.ofNullable(o.policy().getPolicyDefinition().obligations()).orElse(List.of()).stream())
                    .filter(ob -> appliesTo(ob, overall))
                    .toList()
                : List.of();

        return new EvaluationResult(decision, contributing.stream().map(o -> o.policy().getName()).toList(), obligations);
    }

    private static boolean appliesTo(Obligation obligation, Decision decision) {
        return obligation.fulfillOn() == ObligationFulfillOn.BOTH
                || (obligation.fulfillOn() == ObligationFulfillOn.PERMIT) == (decision == Decision.PERMIT);
    }

    private Decision evaluatePolicy(PolicyDefinition definition, Map<UUID, SharedRule> sharedRules, JsonNode input) {
        Stream<Decision> decisions = resolveRules(definition, sharedRules).stream().map(r -> evaluateRule(r, input));
        return switch (definition.ruleCombiningAlgorithm()) {
            case PERMIT_OVERRIDES -> permitOverrides(decisions);
            case DENY_OVERRIDES -> denyOverrides(decisions);
            case FIRST_APPLICABLE -> firstApplicable(decisions);
        };
    }

    /** Rule references take precedence over inline rules; an empty entry is an unresolvable shared rule. */
    private static List<Optional<PolicyRule>> resolveRules(PolicyDefinition definition, Map<UUID, SharedRule> sharedRules) {
        List<PolicyRuleReference> references = definition.ruleReferences();
        if (references == null || references.isEmpty()) {
            return definition.rules().stream().map(Optional::ofNullable).toList();
        }
        List<Optional<PolicyRule>> rules = new ArrayList<>(references.size());
        for (PolicyRuleReference reference : references) {
            if (PolicyRuleReference.INLINE.equalsIgnoreCase(reference.type())) {
                rules.add(Optional.ofNullable(reference.inlineRule()));
            } else if (PolicyRuleReference.SHARED.equalsIgnoreCase(reference.type()) && reference.sharedRuleId() != null) {
                rules.add(Optional.ofNullable(sharedRules.get(reference.sharedRuleId())).map(SharedRule::toPolicyRule));
            } else {
                rules.add(Optional.empty());
            }
        }
        return rules;
    }

    private Decision evaluateRule(Optional<PolicyRule> resolved, JsonNode input) {
        if (resolved.isEmpty()) return Decision.INDETERMINATE_DP;
        PolicyRule rule = resolved.get();

        boolean permit = rule.effect() == RuleEffect.PERMIT;
        Decision effect = permit ? Decision.PERMIT : Decision.DENY;
        Decision indeterminate = permit ? Decision.INDETERMINATE_P : Decision.INDETERMINATE_D;

        Truth target = targetMatches(rule.target(), input);
        if (target == Truth.FALSE) return Decision.NOT_APPLICABLE;
        if (target == Truth.UNKNOWN) return indeterminate;

        Truth condition = rule.condition() == null ? Truth.TRUE : evaluateCondition(rule.condition(), input);
        return switch (condition) {
            case TRUE -> effect;
            case FALSE -> Decision.NOT_APPLICABLE;
            case UNKNOWN -> indeterminate;
        };
    }

    // ── Combining algorithms (XACML 3.0 Appendix C) ────────────────────────────────────────────────────

    private static Decision denyOverrides(Stream<Decision> decisions) {
        boolean permit = false, indD = false, indP = false, indDP = false;
        for (Iterator<Decision> it = decisions.iterator(); it.hasNext(); ) {
            switch (it.next()) {
                case DENY -> { return Decision.DENY; }
                case PERMIT -> permit = true;
                case INDETERMINATE_D -> indD = true;
                case INDETERMINATE_P -> indP = true;
                case INDETERMINATE_DP -> indDP = true;
                case NOT_APPLICABLE -> { }
            }
        }
        if (indDP || (indD && (indP || permit))) return Decision.INDETERMINATE_DP;
        if (indD) return Decision.INDETERMINATE_D;
        if (permit) return Decision.PERMIT;
        if (indP) return Decision.INDETERMINATE_P;
        return Decision.NOT_APPLICABLE;
    }

    private static Decision permitOverrides(Stream<Decision> decisions) {
        boolean deny = false, indD = false, indP = false, indDP = false;
        for (Iterator<Decision> it = decisions.iterator(); it.hasNext(); ) {
            switch (it.next()) {
                case PERMIT -> { return Decision.PERMIT; }
                case DENY -> deny = true;
                case INDETERMINATE_D -> indD = true;
                case INDETERMINATE_P -> indP = true;
                case INDETERMINATE_DP -> indDP = true;
                case NOT_APPLICABLE -> { }
            }
        }
        if (indDP || (indP && (indD || deny))) return Decision.INDETERMINATE_DP;
        if (indP) return Decision.INDETERMINATE_P;
        if (deny) return Decision.DENY;
        if (indD) return Decision.INDETERMINATE_D;
        return Decision.NOT_APPLICABLE;
    }

    private static Decision firstApplicable(Stream<Decision> decisions) {
        return decisions.filter(d -> d != Decision.NOT_APPLICABLE).findFirst().orElse(Decision.NOT_APPLICABLE);
    }

    private static String toResultString(Decision decision) {
        return switch (decision) {
            case PERMIT -> "Permit";
            case DENY -> "Deny";
            case NOT_APPLICABLE -> "NotApplicable";
            default -> "Indeterminate";
        };
    }

    // ── Targets and conditions ─────────────────────────────────────────────────────────────────────────

    private Truth targetMatches(PolicyTarget target, JsonNode input) {
        if (target == null) return Truth.TRUE;
        return Truth.all(Stream.<Supplier<Truth>>of(
                        () -> matchCategory(target.subjects(), input, "subject"),
                        () -> matchCategory(target.resources(), input, "resource"),
                        () -> matchCategory(target.actions(), input, "action"),
                        () -> matchCategory(target.environments(), input, "environment"))
                .map(Supplier::get));
    }

    private Truth matchCategory(List<AttributeMatch> matches, JsonNode input, String category) {
        if (matches == null || matches.isEmpty()) return Truth.TRUE;
        return Truth.all(matches.stream().map(m -> match(m, input, category)));
    }

    private Truth evaluateCondition(PolicyCondition condition, JsonNode input) {
        if (condition.conditions().isEmpty()) return Truth.TRUE;
        Stream<Truth> results = condition.conditions().stream().map(item -> evaluateConditionItem(item, input));
        String operator = condition.operator().isBlank() ? "AND" : condition.operator().trim().toUpperCase(Locale.ROOT);
        return switch (operator) {
            case "AND" -> Truth.all(results);
            case "OR" -> Truth.any(results);
            default -> Truth.UNKNOWN;
        };
    }

    /** Condition items are raw JSON: a nested condition (has "conditions") or an attribute match (has "attributeId"). */
    private Truth evaluateConditionItem(JsonNode item, JsonNode input) {
        try {
            if (property(item, "conditions").isPresent()) {
                return evaluateCondition(mapper.treeToValue(item, PolicyCondition.class), input);
            }
            if (property(item, "attributeId").isPresent()) {
                return match(mapper.treeToValue(item, AttributeMatch.class), input, null);
            }
            return Truth.UNKNOWN;
        } catch (JsonProcessingException | IllegalArgumentException e) {
            return Truth.UNKNOWN;
        }
    }

    private Truth match(AttributeMatch match, JsonNode input, String category) {
        // A missing attribute is an empty bag, which never matches (MustBePresent = false)
        Optional<JsonNode> actual = resolve(input, category, match.attributeId());
        if (actual.isEmpty()) return Truth.FALSE;
        JsonNode expected = match.value() == null ? NullNode.getInstance() : match.value();
        return applyMatch(match.matchFunction(), actual.get(), expected);
    }

    private Truth applyMatch(MatchFunction function, JsonNode actual, JsonNode expected) {
        if (actual.isArray()) {
            Stream<JsonNode> elements = StreamSupport.stream(actual.spliterator(), false);
            return function == MatchFunction.CONTAINS
                    ? Truth.of(elements.anyMatch(e -> valuesEqual(e, expected)))
                    : Truth.any(elements.map(e -> applyMatch(function, e, expected)));
        }
        return switch (function) {
            case EQUALS -> Truth.of(valuesEqual(actual, expected));
            case CONTAINS -> actual.isObject()
                    ? Truth.UNKNOWN
                    : Truth.of(text(actual).toUpperCase(Locale.ROOT).contains(text(expected).toUpperCase(Locale.ROOT)));
            case GREATER_THAN -> compare(actual, expected).map(c -> Truth.of(c > 0)).orElse(Truth.UNKNOWN);
            case LESS_THAN -> compare(actual, expected).map(c -> Truth.of(c < 0)).orElse(Truth.UNKNOWN);
            case REGEX -> regexMatches(text(actual), text(expected));
        };
    }

    private static Truth regexMatches(String value, String pattern) {
        try {
            long deadline = System.nanoTime() + REGEX_TIMEOUT.toNanos();
            return Truth.of(Pattern.compile(pattern).matcher(new DeadlineCharSequence(value, deadline)).find());
        } catch (PatternSyntaxException | DeadlineCharSequence.TimeoutException e) {
            return Truth.UNKNOWN;
        }
    }

    // ── Attribute resolution and value comparison ───────────────────────────────────────────────────────

    private static Optional<JsonNode> resolve(JsonNode input, String category, String attributeId) {
        if (attributeId == null || attributeId.isBlank()) return Optional.empty();

        if (category != null) {
            Optional<JsonNode> value = category(input, category).flatMap(c -> path(c, attributeId));
            if (value.isPresent()) return value;
        }
        // Root lookup also covers fully qualified ids such as "subject.department"
        Optional<JsonNode> value = path(input, attributeId);
        if (value.isPresent() || category != null) return value;

        // Conditions don't declare a category, so search each one
        return CATEGORIES.stream()
                .map(c -> category(input, c).flatMap(node -> path(node, attributeId)))
                .flatMap(Optional::stream)
                .findFirst();
    }

    /** The category object, accepting singular or plural keys ("subject" / "subjects"). */
    private static Optional<JsonNode> category(JsonNode input, String category) {
        return property(input, category).or(() -> property(input, category + "s")).filter(JsonNode::isObject);
    }

    private static Optional<JsonNode> path(JsonNode node, String path) {
        Optional<JsonNode> literal = property(node, path);
        if (literal.isPresent()) return literal;
        JsonNode current = node;
        for (String segment : path.split("\\.", -1)) {
            Optional<JsonNode> next = property(current, segment);
            if (next.isEmpty()) return Optional.empty();
            current = next.get();
        }
        return Optional.of(current);
    }

    /** Case-insensitive property lookup (exact match preferred). */
    private static Optional<JsonNode> property(JsonNode node, String name) {
        if (node == null || !node.isObject()) return Optional.empty();
        JsonNode exact = node.get(name);
        if (exact != null) return Optional.of(exact);
        return node.properties().stream()
                .filter(field -> field.getKey().equalsIgnoreCase(name))
                .map(Map.Entry::getValue)
                .findFirst();
    }

    private static boolean valuesEqual(JsonNode a, JsonNode b) {
        if (a.isNumber() || b.isNumber()) {
            OptionalDouble x = number(a), y = number(b);
            if (x.isPresent() && y.isPresent()) return x.getAsDouble() == y.getAsDouble();
        }
        if (a.isBoolean() || b.isBoolean()) {
            Optional<Boolean> p = bool(a), q = bool(b);
            if (p.isPresent() && q.isPresent()) return Objects.equals(p.get(), q.get());
        }
        return text(a).equalsIgnoreCase(text(b));
    }

    /** Numeric comparison, else date/time comparison; empty when the values are not comparable. */
    private static Optional<Integer> compare(JsonNode a, JsonNode b) {
        OptionalDouble x = number(a), y = number(b);
        if (x.isPresent() && y.isPresent()) return Optional.of(Double.compare(x.getAsDouble(), y.getAsDouble()));
        if (a.isTextual() && b.isTextual()) {
            Optional<Instant> d1 = ValueParsing.parseInstant(a.asText()), d2 = ValueParsing.parseInstant(b.asText());
            if (d1.isPresent() && d2.isPresent()) return Optional.of(d1.get().compareTo(d2.get()));
        }
        return Optional.empty();
    }

    private static String text(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) return "";
        if (node.isTextual()) return node.asText();
        if (node.isBoolean()) return node.booleanValue() ? "true" : "false";
        return node.toString();
    }

    private static OptionalDouble number(JsonNode node) {
        if (node.isNumber()) return OptionalDouble.of(node.doubleValue());
        return node.isTextual() ? ValueParsing.parseNumber(node.asText()) : OptionalDouble.empty();
    }

    private static Optional<Boolean> bool(JsonNode node) {
        if (node.isBoolean()) return Optional.of(node.booleanValue());
        return ValueParsing.parseBoolean(text(node));
    }
}
