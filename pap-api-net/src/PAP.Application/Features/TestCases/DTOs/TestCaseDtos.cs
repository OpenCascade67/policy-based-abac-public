using PAP.Domain.ValueObjects;

namespace PAP.Application.Features.TestCases.DTOs;

public class TestCaseDto
{
    public string Id { get; set; } = string.Empty;
    public string Name { get; set; } = string.Empty;
    public string Description { get; set; } = string.Empty;
    public string? PolicyId { get; set; }
    public string? ScenarioId { get; set; }
    public string InputData { get; set; } = "{}";
    public TestResultDto? Result { get; set; }
    public string? CreatedById { get; set; }
    public string? LastModifiedById { get; set; }
    public DateTime CreatedAt { get; set; }
    public DateTime LastModifiedAt { get; set; }
}

public class TestResultDto
{
    public string Decision { get; set; } = string.Empty;
    public List<string> MatchedPolicies { get; set; } = new();
    public DateTime ExecutedAt { get; set; }
}

public class CreateTestCaseRequest
{
    public string Name { get; set; } = string.Empty;
    public string Description { get; set; } = string.Empty;
    public string? PolicyId { get; set; }
    public string? ScenarioId { get; set; }
    public string InputData { get; set; } = "{}";
}

public class UpdateTestCaseRequest
{
    public string Name { get; set; } = string.Empty;
    public string Description { get; set; } = string.Empty;
    public string? PolicyId { get; set; }
    public string? ScenarioId { get; set; }
    public string InputData { get; set; } = "{}";
    public TestResultDto? Result { get; set; }
}

public class RunTestCaseResponse
{
    public string Decision { get; set; } = string.Empty;
    public List<string> MatchedPolicies { get; set; } = new();
    public DateTime ExecutedAt { get; set; }
}

public class RunTestCaseRequest
{
    public string? InputData { get; set; }
}
