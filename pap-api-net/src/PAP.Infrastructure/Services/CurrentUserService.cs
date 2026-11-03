using System.Security.Claims;
using Microsoft.AspNetCore.Http;
using PAP.Application.Interfaces;

namespace PAP.Infrastructure.Services;

public class CurrentUserService : ICurrentUserService
{
    private readonly IHttpContextAccessor _httpContextAccessor;

    public CurrentUserService(IHttpContextAccessor httpContextAccessor)
        => _httpContextAccessor = httpContextAccessor;

    private ClaimsPrincipal? User => _httpContextAccessor.HttpContext?.User;

    public bool IsAuthenticated => User?.Identity?.IsAuthenticated ?? false;

    public Guid? UserId
    {
        get
        {
            var sub = User?.FindFirst(ClaimTypes.NameIdentifier)?.Value
                ?? User?.FindFirst("sub")?.Value;
            return sub != null && Guid.TryParse(sub, out var id) ? id : null;
        }
    }

    public string? Username =>
        User?.FindFirst(ClaimTypes.Name)?.Value
        ?? User?.FindFirst("unique_name")?.Value;

    public Guid? CurrentDomainId
    {
        get
        {
            var domain = User?.FindFirst("domain")?.Value;
            return domain != null && Guid.TryParse(domain, out var id) ? id : null;
        }
    }
}
