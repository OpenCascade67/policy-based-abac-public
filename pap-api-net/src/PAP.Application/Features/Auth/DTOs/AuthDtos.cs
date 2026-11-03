using PAP.Domain.ValueObjects;

namespace PAP.Application.Features.Auth.DTOs;

public record LoginRequest(string Username, string Password);
public record SendOtpRequest(string Email);
public record VerifyOtpRequest(string Email, string Code);
public record RefreshTokenRequest(string RefreshToken);
public record SwitchDomainRequest(string DomainId);

public class AuthResponse
{
    public string AccessToken { get; set; } = string.Empty;
    public string RefreshToken { get; set; } = string.Empty;
    public DateTime ExpiresAt { get; set; }
    public UserSummaryDto User { get; set; } = new();
    public string? CurrentDomainId { get; set; }
}

public class UserSummaryDto
{
    public string Id { get; set; } = string.Empty;
    public string Username { get; set; } = string.Empty;
    public string? Email { get; set; }
    public string UserType { get; set; } = string.Empty;
    public string? DefaultDomainId { get; set; }
    public List<DomainRoleDto> DomainRoles { get; set; } = new();
}

public class DomainRoleDto
{
    public string DomainId { get; set; } = string.Empty;
    public List<string> Roles { get; set; } = new();
}
