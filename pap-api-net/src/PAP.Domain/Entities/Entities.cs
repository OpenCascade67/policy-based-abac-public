using PAP.Domain.Enums;
using PAP.Domain.Interfaces;
using PAP.Domain.ValueObjects;

namespace PAP.Domain.Entities;

public class User : IAuditableEntity
{
    public Guid Id { get; set; }
    public string Username { get; set; } = string.Empty;
    public string? PasswordHash { get; set; }
    public string? Email { get; set; }
    public UserType UserType { get; set; }
    public UserStatus Status { get; set; }
    public List<DomainRole> DomainRoles { get; set; } = new();
    public Guid? DefaultDomainId { get; set; }
    public DateTime CreatedAt { get; set; }
    public Guid? CreatedById { get; set; }
    public DateTime LastModifiedAt { get; set; }
    public Guid? LastModifiedById { get; set; }
}

public class Role : IAuditableEntity
{
    public Guid Id { get; set; }
    public string Name { get; set; } = string.Empty;
    public string Description { get; set; } = string.Empty;
    public bool IsSystemRole { get; set; }
    public List<string> Permissions { get; set; } = new();
    public DateTime CreatedAt { get; set; }
    public Guid? CreatedById { get; set; }
    public DateTime LastModifiedAt { get; set; }
    public Guid? LastModifiedById { get; set; }
}

public class Domain : IAuditableEntity
{
    public Guid Id { get; set; }
    public string Name { get; set; } = string.Empty;
    public string Description { get; set; } = string.Empty;
    public DomainStatus Status { get; set; }
    public DateTime CreatedAt { get; set; }
    public Guid? CreatedById { get; set; }
    public DateTime LastModifiedAt { get; set; }
    public Guid? LastModifiedById { get; set; }
}

public class Policy : IAuditableEntity, IDomainEntity
{
    public Guid Id { get; set; }
    public Guid DomainId { get; set; }
    public string Name { get; set; } = string.Empty;
    public string Description { get; set; } = string.Empty;
    public PolicyDefinition PolicyDefinition { get; set; } = new();
    public DateTime CreatedAt { get; set; }
    public Guid? CreatedById { get; set; }
    public DateTime LastModifiedAt { get; set; }
    public Guid? LastModifiedById { get; set; }
}

public class SharedRule : IAuditableEntity, IDomainEntity
{
    public Guid Id { get; set; }
    public Guid DomainId { get; set; }
    public string Name { get; set; } = string.Empty;
    public string Description { get; set; } = string.Empty;
    public string Category { get; set; } = string.Empty;
    public RuleEffect Effect { get; set; }
    public PolicyTarget? Target { get; set; }
    public PolicyCondition? Condition { get; set; }
    public List<Guid> UsedByPolicies { get; set; } = new();
    public DateTime CreatedAt { get; set; }
    public Guid? CreatedById { get; set; }
    public DateTime LastModifiedAt { get; set; }
    public Guid? LastModifiedById { get; set; }
}

public class ABACScenario : IAuditableEntity, IDomainEntity
{
    public Guid Id { get; set; }
    public Guid DomainId { get; set; }
    public string Name { get; set; } = string.Empty;
    public string Description { get; set; } = string.Empty;
    public string Value { get; set; } = "{}"; // JSON string
    public List<Guid> TestCases { get; set; } = new();
    public DateTime CreatedAt { get; set; }
    public Guid? CreatedById { get; set; }
    public DateTime LastModifiedAt { get; set; }
    public Guid? LastModifiedById { get; set; }
}

public class TestCase : IAuditableEntity, IDomainEntity
{
    public Guid Id { get; set; }
    public Guid DomainId { get; set; }
    public string Name { get; set; } = string.Empty;
    public string Description { get; set; } = string.Empty;
    public Guid? PolicyId { get; set; }
    public Guid? ScenarioId { get; set; }
    public string InputData { get; set; } = "{}"; // JSON string
    public TestResult? Result { get; set; }
    public DateTime CreatedAt { get; set; }
    public Guid? CreatedById { get; set; }
    public DateTime LastModifiedAt { get; set; }
    public Guid? LastModifiedById { get; set; }
}

public class AppUserSettings
{
    public Guid UserId { get; set; }
    public string Theme { get; set; } = "light";
    public string ColorScheme { get; set; } = "blue";
}

public class RefreshTokenData
{
    public Guid Id { get; set; } = Guid.NewGuid();
    public string Token { get; set; } = string.Empty;
    public Guid UserId { get; set; }
    public Guid? CurrentDomainId { get; set; }
    public DateTime ExpiresAt { get; set; }
    public bool IsRevoked { get; set; }
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
}
