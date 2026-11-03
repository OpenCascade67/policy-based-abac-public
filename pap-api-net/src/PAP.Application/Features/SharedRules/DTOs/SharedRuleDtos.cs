using PAP.Domain.ValueObjects;

namespace PAP.Application.Features.SharedRules.DTOs;

public class SharedRuleDto
{
    public string Id { get; set; } = string.Empty;
    public string Name { get; set; } = string.Empty;
    public string Description { get; set; } = string.Empty;
    public string Category { get; set; } = string.Empty;
    public string Effect { get; set; } = string.Empty;
    public PolicyTarget? Target { get; set; }
    public PolicyCondition? Condition { get; set; }
    public List<string> UsedByPolicies { get; set; } = new();
    public string? CreatedById { get; set; }
    public string? LastModifiedById { get; set; }
    public DateTime CreatedAt { get; set; }
    public DateTime LastModifiedAt { get; set; }
}

public class CreateSharedRuleRequest
{
    public string Name { get; set; } = string.Empty;
    public string Description { get; set; } = string.Empty;
    public string Category { get; set; } = string.Empty;
    public string Effect { get; set; } = "Permit";
    public PolicyTarget? Target { get; set; }
    public PolicyCondition? Condition { get; set; }
}

public class UpdateSharedRuleRequest
{
    public string Name { get; set; } = string.Empty;
    public string Description { get; set; } = string.Empty;
    public string Category { get; set; } = string.Empty;
    public string Effect { get; set; } = "Permit";
    public PolicyTarget? Target { get; set; }
    public PolicyCondition? Condition { get; set; }
}
