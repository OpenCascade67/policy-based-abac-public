namespace PAP.Application.Features.Scenarios.DTOs;

public class ScenarioDto
{
    public string Id { get; set; } = string.Empty;
    public string Name { get; set; } = string.Empty;
    public string Description { get; set; } = string.Empty;
    public string Value { get; set; } = "{}";
    public List<string> TestCases { get; set; } = new();
    public string? CreatedById { get; set; }
    public string? LastModifiedById { get; set; }
    public DateTime CreatedAt { get; set; }
    public DateTime LastModifiedAt { get; set; }
}

public class CreateScenarioRequest
{
    public string Name { get; set; } = string.Empty;
    public string Description { get; set; } = string.Empty;
    public string Value { get; set; } = "{}";
}

public class UpdateScenarioRequest
{
    public string Name { get; set; } = string.Empty;
    public string Description { get; set; } = string.Empty;
    public string Value { get; set; } = "{}";
}
