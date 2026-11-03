using PAP.Domain.Enums;
using System.Text.Json.Serialization;

namespace PAP.Domain.ValueObjects;

public class DomainRole
{
    public Guid DomainId { get; set; }
    public List<Guid> Roles { get; set; } = new();
}

public class PolicyDefinition
{
    public string Version { get; set; } = "3.0";
    public string PolicyId { get; set; } = string.Empty;
    public RuleCombiningAlgorithm RuleCombiningAlgorithm { get; set; }
    public List<PolicyRule> Rules { get; set; } = new();
    public List<PolicyRuleReference>? RuleReferences { get; set; }
    public List<Obligation>? Obligations { get; set; }
}

public class PolicyRule
{
    public string RuleId { get; set; } = string.Empty;
    public RuleEffect Effect { get; set; }
    public string? Description { get; set; }
    public PolicyTarget? Target { get; set; }
    public PolicyCondition? Condition { get; set; }
}

public class PolicyTarget
{
    public List<AttributeMatch>? Subjects { get; set; }
    public List<AttributeMatch>? Resources { get; set; }
    public List<AttributeMatch>? Actions { get; set; }
    public List<AttributeMatch>? Environments { get; set; }
}

public class AttributeMatch
{
    public string AttributeId { get; set; } = string.Empty;
    public MatchFunction MatchFunction { get; set; }
    [JsonPropertyName("value")]
    public object? Value { get; set; }
}

public class PolicyCondition
{
    public string Operator { get; set; } = "AND"; // AND | OR
    // Each item can be a nested PolicyCondition or an AttributeMatch
    public List<object> Conditions { get; set; } = new();
}

public class PolicyRuleReference
{
    public string Type { get; set; } = "inline"; // inline | shared
    public Guid? SharedRuleId { get; set; }
    public PolicyRule? InlineRule { get; set; }
}

public class Obligation
{
    public string ObligationId { get; set; } = string.Empty;
    public ObligationFulfillOn FulfillOn { get; set; }
    public string? Description { get; set; }
    public List<ObligationAttributeAssignment> AttributeAssignments { get; set; } = new();
}

public class ObligationAttributeAssignment
{
    public string AttributeId { get; set; } = string.Empty;
    public object? Value { get; set; }
    public AttributeDataType? DataType { get; set; }
}

public class TestResult
{
    public TestDecision Decision { get; set; }
    public List<string> MatchedPolicies { get; set; } = new();
    public DateTime ExecutedAt { get; set; }
}
