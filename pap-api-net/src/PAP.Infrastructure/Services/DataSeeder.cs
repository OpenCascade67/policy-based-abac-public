using Microsoft.Extensions.Options;
using PAP.Application.Interfaces;
using PAP.Domain.Entities;
using PAP.Domain.Enums;
using PAP.Infrastructure.Repositories.JsonFile;
using DomainEntity = PAP.Domain.Entities.Domain;

namespace PAP.Infrastructure.Services;

public class DataSeeder : IDataSeeder
{
    private readonly IUserRepository _users;
    private readonly IRoleRepository _roles;
    private readonly IDomainRepository _domains;
    private readonly IPasswordHasher _hasher;
    private readonly DataStoreOptions _options;

    public DataSeeder(IUserRepository users, IRoleRepository roles, IDomainRepository domains,
        IPasswordHasher hasher, IOptions<DataStoreOptions> options)
    {
        _users = users; _roles = roles; _domains = domains; _hasher = hasher; _options = options.Value;
    }

    public async Task SeedAsync()
    {
        await SeedRolesAsync();
        await SeedDomainsAsync();
        await SeedAdminUserAsync();
    }

    private async Task SeedRolesAsync()
    {
        var existing = await _roles.GetAllAsync();
        if (existing.Any()) return;

        var systemRoles = new[]
        {
            ("System Admin",    new[] { "manage_users","manage_domains","manage_roles","manage_policies","test_policies","manage_scenarios" }),
            ("Policy Author",   new[] { "manage_policies","test_policies","manage_scenarios" }),
            ("Policy Viewer",   new[] { "test_policies", "manage_scenarios" }),
            ("Domain Admin",    new[] { "manage_users","manage_domains","manage_policies","test_policies","manage_scenarios" }),
            ("Auditor",         new[] { "test_policies", "manage_scenarios" }),
        };

        foreach (var (name, perms) in systemRoles)
        {
            await _roles.AddAsync(new Role
            {
                Id = Guid.NewGuid(), Name = name, Description = $"{name} role",
                IsSystemRole = true, Permissions = perms.ToList(),
                CreatedAt = DateTime.UtcNow, LastModifiedAt = DateTime.UtcNow
            });
        }
    }

    private async Task SeedDomainsAsync()
    {
        var existing = await _domains.GetAllAsync();
        if (existing.Any()) return;

        await _domains.AddAsync(new DomainEntity
        {
            Id = Guid.Parse("00000000-0000-0000-0000-000000000001"),
            Name = "Default Domain", Description = "Default administration domain",
            Status = DomainStatus.Active,
            CreatedAt = DateTime.UtcNow, LastModifiedAt = DateTime.UtcNow
        });
    }

    private async Task SeedAdminUserAsync()
    {
        var existing = await _users.GetAllAsync();
        if (existing.Any()) return;

        var roles = await _roles.GetAllAsync();
        var adminRole = roles.FirstOrDefault(r => r.Name == "System Admin");

        await _users.AddAsync(new User
        {
            Id = Guid.Parse("00000000-0000-0000-0000-000000000002"),
            Username = "admin",
            PasswordHash = _hasher.Hash("admin123"),
            UserType = UserType.Local,
            Status = UserStatus.Active,
            DefaultDomainId = Guid.Parse("00000000-0000-0000-0000-000000000001"),
            DomainRoles = adminRole != null
                ? new List<Domain.ValueObjects.DomainRole>
                {
                    new() { DomainId = Guid.Parse("00000000-0000-0000-0000-000000000001"), Roles = new List<Guid> { adminRole.Id } }
                }
                : new(),
            CreatedAt = DateTime.UtcNow, LastModifiedAt = DateTime.UtcNow
        });
    }
}
