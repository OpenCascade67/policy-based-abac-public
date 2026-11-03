using PAP.Domain.Entities;
using PAP.Domain.ValueObjects;

namespace PAP.Application.Interfaces;

public interface ICurrentUserService
{
    Guid? UserId { get; }
    string? Username { get; }
    Guid? CurrentDomainId { get; }
    bool IsAuthenticated { get; }
}

public interface IOtpService
{
    string GenerateOtp(string email);
    bool VerifyOtp(string email, string code);
    void ClearOtp(string email);
}

public interface ITokenService
{
    string GenerateAccessToken(User user, Guid? currentDomainId);
    string GenerateRefreshToken();
    (Guid userId, Guid? domainId)? ValidateRefreshTokenClaims(string accessToken);
}

public class EvaluationRequest
{
    public Guid? PolicyId { get; set; }
    public string InputData { get; set; } = "{}";
}

public class EvaluationResult
{
    public string Decision { get; set; } = "NotApplicable";
    public List<string> MatchedPolicies { get; set; } = new();
    public List<Obligation> ApplicableObligations { get; set; } = new();
}

public interface IXacmlEvaluationService
{
    Task<EvaluationResult> EvaluateAsync(EvaluationRequest request);
}

public interface IDataSeeder
{
    Task SeedAsync();
}
