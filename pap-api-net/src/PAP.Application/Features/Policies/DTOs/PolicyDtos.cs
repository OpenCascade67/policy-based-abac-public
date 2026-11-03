using PAP.Domain.ValueObjects;

namespace PAP.Application.Features.Policies.DTOs;

public class PolicyDto
{
    public string Id { get; set; } = string.Empty;
    public string Name { get; set; } = string.Empty;
    public string Description { get; set; } = string.Empty;
    public PolicyDefinitionDto PolicyDefinition { get; set; } = new();
    public string? CreatedById { get; set; }
    public string? LastModifiedById { get; set; }
    public DateTime CreatedAt { get; set; }
    public DateTime LastModifiedAt { get; set; }
}

public class PolicyDefinitionDto
{
    public string Version { get; set; } = "3.0";
    public string PolicyId { get; set; } = string.Empty;
    public string RuleCombiningAlgorithm { get; set; } = string.Empty;
    public List<PolicyRule> Rules { get; set; } = new();
    public List<PolicyRuleReference>? RuleReferences { get; set; }
    public List<Obligation>? Obligations { get; set; }
}

public class CreatePolicyRequest
{
    public string Name { get; set; } = string.Empty;
    public string Description { get; set; } = string.Empty;
    public PolicyDefinitionDto PolicyDefinition { get; set; } = new();
}

public class UpdatePolicyRequest
{
    public string Name { get; set; } = string.Empty;
    public string Description { get; set; } = string.Empty;
    public PolicyDefinitionDto PolicyDefinition { get; set; } = new();
}
