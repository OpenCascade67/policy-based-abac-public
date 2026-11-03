using System.Globalization;
using System.Text.Json;
using System.Text.Json.Serialization;
using System.Text.RegularExpressions;
using PAP.Application.Interfaces;
using PAP.Domain.Entities;
using PAP.Domain.Enums;
using PAP.Domain.ValueObjects;

namespace PAP.Infrastructure.Services;

/// <summary>
/// XACML 3.0-style evaluation of policies against a JSON request of the form
/// { "subject": {...}, "resource": {...}, "action": {...}, "environment": {...} }.
/// Attribute ids may be dot paths ("subject.department"); attributes not found in their
/// category are looked up at the root. Array values are treated as bags (any element may match).
/// </summary>
public static class PolicyEvaluator
{
    // Indeterminate{D|P|DP} records which effect an erroring rule/policy could have had (XACML 3.0 §7.11)
    private enum Decision { Permit, Deny, NotApplicable, IndeterminateD, IndeterminateP, IndeterminateDP }

    private sealed class EvaluationException(string message) : Exception(message);

    private static readonly string[] Categories = { "subject", "resource", "action", "environment" };
    private static readonly TimeSpan RegexTimeout = TimeSpan.FromSeconds(1);
    private static readonly JsonSerializerOptions JsonOptions = new()
    {
        PropertyNameCaseInsensitive = true,
        Converters = { new JsonStringEnumConverter() }
    };

    public static EvaluationResult Evaluate(IEnumerable<Policy> policies, IEnumerable<SharedRule> sharedRules, string inputData)
    {
        JsonElement input;
        try
        {
            using var doc = JsonDocument.Parse(inputData);
            input = doc.RootElement.Clone();
        }
        catch (JsonException) { return new EvaluationResult { Decision = "Indeterminate" }; }
        if (input.ValueKind != JsonValueKind.Object) return new EvaluationResult { Decision = "Indeterminate" };

        var sharedRuleList = sharedRules.ToList();
        var results = policies
            .Select(p => (Policy: p, Decision: EvaluatePolicy(p.PolicyDefinition, sharedRuleList, input)))
            .ToList();

        // Policies are combined with deny-overrides, the safe default when no policy set is defined
        var overall = DenyOverrides(results.Select(r => r.Decision));
        var decision = ToResultString(overall);
        var contributing = decision == "NotApplicable"
            ? new()
            : results.Where(r => ToResultString(r.Decision) == decision).ToList();

        return new EvaluationResult
        {
            Decision = decision,
            MatchedPolicies = contributing.Select(r => r.Policy.Name).ToList(),
            ApplicableObligations = overall is Decision.Permit or Decision.Deny
                ? contributing
                    .SelectMany(r => r.Policy.PolicyDefinition.Obligations ?? new())
                    .Where(o => o.FulfillOn == ObligationFulfillOn.Both ||
                                (o.FulfillOn == ObligationFulfillOn.Permit) == (overall == Decision.Permit))
                    .ToList()
                : new()
        };
    }

    private static Decision EvaluatePolicy(PolicyDefinition pd, List<SharedRule> sharedRules, JsonElement input)
    {
        var decisions = ResolveRules(pd, sharedRules).Select(r => EvaluateRule(r, input));
        return pd.RuleCombiningAlgorithm switch
        {
            RuleCombiningAlgorithm.PermitOverrides => PermitOverrides(decisions),
            RuleCombiningAlgorithm.DenyOverrides => DenyOverrides(decisions),
            RuleCombiningAlgorithm.FirstApplicable => FirstApplicable(decisions),
            _ => Decision.IndeterminateDP
        };
    }

    // A null entry is a shared rule reference that could not be resolved
    private static List<PolicyRule?> ResolveRules(PolicyDefinition pd, List<SharedRule> sharedRules)
    {
        if (pd.RuleReferences == null || pd.RuleReferences.Count == 0)
            return (pd.Rules ?? new()).ToList<PolicyRule?>();

        return pd.RuleReferences.Select(r =>
        {
            if (string.Equals(r.Type, "inline", StringComparison.OrdinalIgnoreCase))
                return r.InlineRule;
            if (string.Equals(r.Type, "shared", StringComparison.OrdinalIgnoreCase) &&
                sharedRules.FirstOrDefault(s => s.Id == r.SharedRuleId) is { } sr)
                return new PolicyRule { RuleId = sr.Id.ToString(), Effect = sr.Effect, Description = sr.Description, Target = sr.Target, Condition = sr.Condition };
            return null;
        }).ToList();
    }

    private static Decision EvaluateRule(PolicyRule? rule, JsonElement input)
    {
        if (rule == null) return Decision.IndeterminateDP;

        var effect = rule.Effect == RuleEffect.Permit ? Decision.Permit : Decision.Deny;
        var indeterminate = rule.Effect == RuleEffect.Permit ? Decision.IndeterminateP : Decision.IndeterminateD;

        var target = TargetMatches(rule.Target, input);
        if (target == false) return Decision.NotApplicable;
        if (target == null) return indeterminate;

        var condition = rule.Condition == null ? true : EvaluateCondition(rule.Condition, input);
        return condition switch { true => effect, false => Decision.NotApplicable, null => indeterminate };
    }

    #region Combining algorithms (XACML 3.0 Appendix C)

    private static Decision DenyOverrides(IEnumerable<Decision> decisions)
    {
        bool permit = false, indD = false, indP = false, indDP = false;
        foreach (var d in decisions)
        {
            switch (d)
            {
                case Decision.Deny: return Decision.Deny;
                case Decision.Permit: permit = true; break;
                case Decision.IndeterminateD: indD = true; break;
                case Decision.IndeterminateP: indP = true; break;
                case Decision.IndeterminateDP: indDP = true; break;
            }
        }
        if (indDP || (indD && (indP || permit))) return Decision.IndeterminateDP;
        if (indD) return Decision.IndeterminateD;
        if (permit) return Decision.Permit;
        if (indP) return Decision.IndeterminateP;
        return Decision.NotApplicable;
    }

    private static Decision PermitOverrides(IEnumerable<Decision> decisions)
    {
        bool deny = false, indD = false, indP = false, indDP = false;
        foreach (var d in decisions)
        {
            switch (d)
            {
                case Decision.Permit: return Decision.Permit;
                case Decision.Deny: deny = true; break;
                case Decision.IndeterminateD: indD = true; break;
                case Decision.IndeterminateP: indP = true; break;
                case Decision.IndeterminateDP: indDP = true; break;
            }
        }
        if (indDP || (indP && (indD || deny))) return Decision.IndeterminateDP;
        if (indP) return Decision.IndeterminateP;
        if (deny) return Decision.Deny;
        if (indD) return Decision.IndeterminateD;
        return Decision.NotApplicable;
    }

    private static Decision FirstApplicable(IEnumerable<Decision> decisions)
        => decisions.FirstOrDefault(d => d != Decision.NotApplicable, Decision.NotApplicable);

    private static string ToResultString(Decision d) => d switch
    {
        Decision.Permit => "Permit",
        Decision.Deny => "Deny",
        Decision.NotApplicable => "NotApplicable",
        _ => "Indeterminate"
    };

    #endregion

    #region Targets and conditions (three-valued: true, false, null = indeterminate)

    private static bool? TargetMatches(PolicyTarget? target, JsonElement input)
    {
        if (target == null) return true;
        return All(new[]
        {
            MatchCategory(target.Subjects, input, "subject"),
            MatchCategory(target.Resources, input, "resource"),
            MatchCategory(target.Actions, input, "action"),
            MatchCategory(target.Environments, input, "environment")
        });
    }

    private static bool? MatchCategory(List<AttributeMatch>? matches, JsonElement input, string category)
        => matches == null || matches.Count == 0 ? true : All(matches.Select(m => Match(m, input, category)));

    private static bool? EvaluateCondition(PolicyCondition condition, JsonElement input)
    {
        if (condition.Conditions == null || condition.Conditions.Count == 0) return true;
        var results = condition.Conditions.Select(c => EvaluateConditionItem(c, input));
        return (string.IsNullOrWhiteSpace(condition.Operator) ? "AND" : condition.Operator.Trim().ToUpperInvariant()) switch
        {
            "AND" => All(results),
            "OR" => AnyOf(results),
            _ => null
        };
    }

    // Condition items deserialize as JsonElement since the domain model types them as object
    private static bool? EvaluateConditionItem(object? item, JsonElement input)
    {
        try
        {
            return item switch
            {
                PolicyCondition nested => EvaluateCondition(nested, input),
                AttributeMatch match => Match(match, input, null),
                JsonElement el when TryGetProperty(el, "conditions", out _) => EvaluateCondition(el.Deserialize<PolicyCondition>(JsonOptions)!, input),
                JsonElement el when TryGetProperty(el, "attributeId", out _) => Match(el.Deserialize<AttributeMatch>(JsonOptions)!, input, null),
                _ => null
            };
        }
        catch (JsonException) { return null; }
    }

    private static bool? Match(AttributeMatch match, JsonElement input, string? category)
    {
        // A missing attribute is an empty bag, which never matches (MustBePresent = false)
        if (Resolve(input, category, match.AttributeId) is not { } actual) return false;
        var expected = match.Value is JsonElement je ? je : JsonSerializer.SerializeToElement(match.Value);
        return ApplyMatch(match.MatchFunction, actual, expected);
    }

    private static bool? ApplyMatch(MatchFunction fn, JsonElement actual, JsonElement expected)
    {
        if (actual.ValueKind == JsonValueKind.Array)
        {
            return fn == MatchFunction.Contains
                ? actual.EnumerateArray().Any(e => ValuesEqual(e, expected))
                : AnyOf(actual.EnumerateArray().Select(e => ApplyMatch(fn, e, expected)));
        }

        try
        {
            return fn switch
            {
                MatchFunction.Equals => ValuesEqual(actual, expected),
                MatchFunction.Contains => actual.ValueKind != JsonValueKind.Object
                    ? Text(actual).Contains(Text(expected), StringComparison.OrdinalIgnoreCase)
                    : throw new EvaluationException("Contains cannot be applied to an object"),
                MatchFunction.GreaterThan => CompareValues(actual, expected) > 0,
                MatchFunction.LessThan => CompareValues(actual, expected) < 0,
                MatchFunction.Regex => Regex.IsMatch(Text(actual), Text(expected), RegexOptions.None, RegexTimeout),
                _ => throw new EvaluationException($"Unsupported match function {fn}")
            };
        }
        catch (Exception ex) when (ex is EvaluationException or ArgumentException or RegexMatchTimeoutException)
        {
            return null;
        }
    }

    private static bool? All(IEnumerable<bool?> results)
    {
        var indeterminate = false;
        foreach (var r in results)
        {
            if (r == false) return false;
            if (r == null) indeterminate = true;
        }
        return indeterminate ? null : true;
    }

    private static bool? AnyOf(IEnumerable<bool?> results)
    {
        var indeterminate = false;
        foreach (var r in results)
        {
            if (r == true) return true;
            if (r == null) indeterminate = true;
        }
        return indeterminate ? null : false;
    }

    #endregion

    #region Attribute resolution and value comparison

    private static JsonElement? Resolve(JsonElement input, string? category, string attributeId)
    {
        if (string.IsNullOrWhiteSpace(attributeId)) return null;

        if (category != null && TryGetCategory(input, category, out var categoryEl) && TryGetPath(categoryEl, attributeId, out var value))
            return value;
        // Root lookup also covers fully qualified ids such as "subject.department"
        if (TryGetPath(input, attributeId, out value))
            return value;
        // Conditions don't declare a category, so search each one
        if (category == null)
            foreach (var c in Categories)
                if (TryGetCategory(input, c, out categoryEl) && TryGetPath(categoryEl, attributeId, out value))
                    return value;
        return null;
    }

    private static bool TryGetCategory(JsonElement input, string category, out JsonElement value)
        => (TryGetProperty(input, category, out value) || TryGetProperty(input, category + "s", out value))
           && value.ValueKind == JsonValueKind.Object;

    private static bool TryGetPath(JsonElement element, string path, out JsonElement value)
    {
        if (TryGetProperty(element, path, out value)) return true;
        var current = element;
        foreach (var segment in path.Split('.'))
        {
            if (!TryGetProperty(current, segment, out current)) { value = default; return false; }
        }
        value = current;
        return true;
    }

    private static bool TryGetProperty(JsonElement element, string name, out JsonElement value)
    {
        if (element.ValueKind == JsonValueKind.Object)
        {
            if (element.TryGetProperty(name, out value)) return true;
            foreach (var p in element.EnumerateObject())
            {
                if (string.Equals(p.Name, name, StringComparison.OrdinalIgnoreCase)) { value = p.Value; return true; }
            }
        }
        value = default;
        return false;
    }

    private static bool ValuesEqual(JsonElement a, JsonElement b)
    {
        if ((a.ValueKind == JsonValueKind.Number || b.ValueKind == JsonValueKind.Number) && TryNumber(a, out var x) && TryNumber(b, out var y))
            return x == y;
        if ((IsBool(a) || IsBool(b)) && TryBool(a, out var p) && TryBool(b, out var q))
            return p == q;
        return Text(a).Equals(Text(b), StringComparison.OrdinalIgnoreCase);
    }

    private static int CompareValues(JsonElement a, JsonElement b)
    {
        if (TryNumber(a, out var x) && TryNumber(b, out var y)) return x.CompareTo(y);
        if (TryDate(a, out var d1) && TryDate(b, out var d2)) return d1.CompareTo(d2);
        throw new EvaluationException($"Cannot compare '{Text(a)}' with '{Text(b)}'");
    }

    private static string Text(JsonElement e) => e.ValueKind switch
    {
        JsonValueKind.String => e.GetString() ?? "",
        JsonValueKind.True => "true",
        JsonValueKind.False => "false",
        JsonValueKind.Null or JsonValueKind.Undefined => "",
        _ => e.GetRawText()
    };

    private static bool IsBool(JsonElement e) => e.ValueKind is JsonValueKind.True or JsonValueKind.False;

    private static bool TryBool(JsonElement e, out bool value)
    {
        if (IsBool(e)) { value = e.GetBoolean(); return true; }
        return bool.TryParse(Text(e), out value);
    }

    private static bool TryNumber(JsonElement e, out double value)
    {
        if (e.ValueKind == JsonValueKind.Number) return e.TryGetDouble(out value);
        value = 0;
        return e.ValueKind == JsonValueKind.String &&
               double.TryParse(e.GetString(), NumberStyles.Float, CultureInfo.InvariantCulture, out value);
    }

    private static bool TryDate(JsonElement e, out DateTimeOffset value)
    {
        value = default;
        return e.ValueKind == JsonValueKind.String &&
               DateTimeOffset.TryParse(e.GetString(), CultureInfo.InvariantCulture, DateTimeStyles.AssumeUniversal, out value);
    }

    #endregion
}
