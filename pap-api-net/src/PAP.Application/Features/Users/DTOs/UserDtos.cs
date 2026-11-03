namespace PAP.Application.Features.Users.DTOs;

public class UserDto
{
    public string Id { get; set; } = string.Empty;
    public string Username { get; set; } = string.Empty;
    public string? Email { get; set; }
    public string UserType { get; set; } = string.Empty;
    public string Status { get; set; } = string.Empty;
    public List<DomainRoleItemDto> DomainRoles { get; set; } = new();
    public string? DefaultDomainId { get; set; }
    public string? CreatedById { get; set; }
    public string? LastModifiedById { get; set; }
    public DateTime CreatedAt { get; set; }
    public DateTime LastModifiedAt { get; set; }
}

public class DomainRoleItemDto
{
    public string DomainId { get; set; } = string.Empty;
    public List<string> Roles { get; set; } = new();
}

public class CreateUserRequest
{
    public string Username { get; set; } = string.Empty;
    public string Password { get; set; } = string.Empty;
    public string Status { get; set; } = "active";
    public List<DomainRoleItemDto> DomainRoles { get; set; } = new();
}

public class UpdateUserRequest
{
    public string? Password { get; set; }
    public string Status { get; set; } = "active";
    public List<DomainRoleItemDto> DomainRoles { get; set; } = new();
}

public class SetDefaultDomainRequest
{
    public string DomainId { get; set; } = string.Empty;
}
