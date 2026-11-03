using PAP.Domain.Entities;
using PAP.Domain.Interfaces;
using DomainEntity = PAP.Domain.Entities.Domain;

namespace PAP.Application.Interfaces;

public interface IUserRepository : IRepository<User>
{
    Task<User?> GetByUsernameAsync(string username);
    Task<User?> GetByEmailAsync(string email);
    Task<bool> UsernameExistsAsync(string username, Guid? excludeId = null);
}

public interface IRoleRepository : IRepository<Role>
{
    Task<bool> NameExistsAsync(string name, Guid? excludeId = null);
}

public interface IDomainRepository : IRepository<DomainEntity>
{
    Task<bool> NameExistsAsync(string name, Guid? excludeId = null);
}

public interface IPolicyRepository : IRepository<Policy>
{
    Task<List<Policy>> SearchAsync(string? search);
}

public interface ISharedRuleRepository : IRepository<SharedRule>
{
    Task UpdateUsageAsync(Guid ruleId, Guid policyId, bool isUsing);
}

public interface IScenarioRepository : IRepository<ABACScenario> { }

public interface ITestCaseRepository : IRepository<TestCase> { }

public interface ISettingsRepository
{
    Task<AppUserSettings?> GetByUserIdAsync(Guid userId);
    Task<AppUserSettings> UpsertAsync(AppUserSettings settings);
}

public interface IRefreshTokenRepository
{
    Task<RefreshTokenData?> GetByTokenAsync(string token);
    Task AddAsync(RefreshTokenData token);
    Task RevokeAsync(string token);
    Task RevokeAllForUserAsync(Guid userId);
}
