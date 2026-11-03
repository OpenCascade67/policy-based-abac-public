using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Security.Cryptography;
using System.Text;
using Microsoft.Extensions.Options;
using Microsoft.IdentityModel.Tokens;
using PAP.Application.Interfaces;
using PAP.Domain.Entities;

namespace PAP.Infrastructure.Services;

public class JwtOptions
{
    public string Secret { get; set; } = string.Empty;
    public int AccessTokenExpiryHours { get; set; } = 2;
}

public class TokenService : ITokenService
{
    private readonly JwtOptions _options;
    public TokenService(IOptions<JwtOptions> options) => _options = options.Value;

    public string GenerateAccessToken(User user, Guid? currentDomainId)
    {
        var key = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(_options.Secret));
        var creds = new SigningCredentials(key, SecurityAlgorithms.HmacSha256);
        var claims = new List<Claim>
        {
            new(JwtRegisteredClaimNames.Sub, user.Id.ToString()),
            new(JwtRegisteredClaimNames.UniqueName, user.Username),
            new("userType", user.UserType.ToString().ToLower()),
        };
        if (currentDomainId.HasValue)
            claims.Add(new("domain", currentDomainId.Value.ToString()));

        // Embed permissions for current domain
        var domainRole = user.DomainRoles.FirstOrDefault(dr => dr.DomainId == currentDomainId);
        // Permissions will be resolved server-side from roles; embed domain for claim-based lookup

        var token = new JwtSecurityToken(
            claims: claims,
            expires: DateTime.UtcNow.AddHours(_options.AccessTokenExpiryHours),
            signingCredentials: creds);

        return new JwtSecurityTokenHandler().WriteToken(token);
    }

    public string GenerateRefreshToken()
    {
        var bytes = new byte[64];
        using var rng = RandomNumberGenerator.Create();
        rng.GetBytes(bytes);
        return Convert.ToBase64String(bytes);
    }

    public (Guid userId, Guid? domainId)? ValidateRefreshTokenClaims(string accessToken)
    {
        var handler = new JwtSecurityTokenHandler();
        try
        {
            var jwt = handler.ReadJwtToken(accessToken);
            var sub = jwt.Claims.FirstOrDefault(c => c.Type == JwtRegisteredClaimNames.Sub)?.Value;
            var domain = jwt.Claims.FirstOrDefault(c => c.Type == "domain")?.Value;
            if (sub == null) return null;
            return (Guid.Parse(sub), domain != null ? Guid.Parse(domain) : null);
        }
        catch { return null; }
    }
}
