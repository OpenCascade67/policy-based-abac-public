using Microsoft.Extensions.Options;
using PAP.Application.Interfaces;
using PAP.Domain.Entities;
using PAP.Infrastructure.Repositories.JsonFile;
using DomainEntity = PAP.Domain.Entities.Domain;

namespace PAP.Infrastructure.Repositories.JsonFile;

public class UserRepository : JsonFileRepository<User>, IUserRepository
{
    public UserRepository(IOptions<DataStoreOptions> o, ICurrentUserService cu) : base(o, "users.json", cu) { }
    protected override Guid GetId(User e) => e.Id;
    protected override void SetId(User e, Guid id) => e.Id = id;

    public async Task<User?> GetByUsernameAsync(string username)
        => (await ReadAllAsync()).FirstOrDefault(u => u.Username.Equals(username, StringComparison.OrdinalIgnoreCase));

    public async Task<User?> GetByEmailAsync(string email)
        => (await ReadAllAsync()).FirstOrDefault(u => u.Email != null && u.Email.Equals(email, StringComparison.OrdinalIgnoreCase));

    public async Task<bool> UsernameExistsAsync(string username, Guid? excludeId = null)
        => (await ReadAllAsync()).Any(u => u.Username.Equals(username, StringComparison.OrdinalIgnoreCase)
            && (excludeId == null || u.Id != excludeId));
}

public class RoleRepository : JsonFileRepository<Role>, IRoleRepository
{
    public RoleRepository(IOptions<DataStoreOptions> o, ICurrentUserService cu) : base(o, "roles.json", cu) { }
    protected override Guid GetId(Role e) => e.Id;
    protected override void SetId(Role e, Guid id) => e.Id = id;

    public async Task<bool> NameExistsAsync(string name, Guid? excludeId = null)
        => (await ReadAllAsync()).Any(r => r.Name.Equals(name, StringComparison.OrdinalIgnoreCase)
            && (excludeId == null || r.Id != excludeId));
}

public class DomainRepository : JsonFileRepository<DomainEntity>, IDomainRepository
{
    public DomainRepository(IOptions<DataStoreOptions> o, ICurrentUserService cu) : base(o, "domains.json", cu) { }
    protected override Guid GetId(DomainEntity e) => e.Id;
    protected override void SetId(DomainEntity e, Guid id) => e.Id = id;

    public async Task<bool> NameExistsAsync(string name, Guid? excludeId = null)
        => (await ReadAllAsync()).Any(d => d.Name.Equals(name, StringComparison.OrdinalIgnoreCase)
            && (excludeId == null || d.Id != excludeId));
}

public class PolicyRepository : JsonFileRepository<Policy>, IPolicyRepository
{
    public PolicyRepository(IOptions<DataStoreOptions> o, ICurrentUserService cu) : base(o, "policies.json", cu) { }
    protected override Guid GetId(Policy e) => e.Id;
    protected override void SetId(Policy e, Guid id) => e.Id = id;

    public async Task<List<Policy>> SearchAsync(string? search)
    {
        var all = await ReadAllAsync();
        if (string.IsNullOrWhiteSpace(search)) return all.OrderBy(p => p.Name).ToList();
        return all.Where(p => p.Name.Contains(search, StringComparison.OrdinalIgnoreCase)).OrderBy(p => p.Name).ToList();
    }
}

public class SharedRuleRepository : JsonFileRepository<SharedRule>, ISharedRuleRepository
{
    public SharedRuleRepository(IOptions<DataStoreOptions> o, ICurrentUserService cu) : base(o, "shared-rules.json", cu) { }
    protected override Guid GetId(SharedRule e) => e.Id;
    protected override void SetId(SharedRule e, Guid id) => e.Id = id;

    public async Task UpdateUsageAsync(Guid ruleId, Guid policyId, bool isUsing)
    {
        var all = await GetAllOriginalAsync();
        var rule = all.FirstOrDefault(r => r.Id == ruleId);
        if (rule == null) return;
        if (isUsing && !rule.UsedByPolicies.Contains(policyId))
            rule.UsedByPolicies.Add(policyId);
        else if (!isUsing)
            rule.UsedByPolicies.Remove(policyId);
        await WriteAllAsync(all);
    }
}

public class ScenarioRepository : JsonFileRepository<ABACScenario>, IScenarioRepository
{
    public ScenarioRepository(IOptions<DataStoreOptions> o, ICurrentUserService cu) : base(o, "scenarios.json", cu) { }
    protected override Guid GetId(ABACScenario e) => e.Id;
    protected override void SetId(ABACScenario e, Guid id) => e.Id = id;
}

public class TestCaseRepository : JsonFileRepository<TestCase>, ITestCaseRepository
{
    public TestCaseRepository(IOptions<DataStoreOptions> o, ICurrentUserService cu) : base(o, "test-cases.json", cu) { }
    protected override Guid GetId(TestCase e) => e.Id;
    protected override void SetId(TestCase e, Guid id) => e.Id = id;
}

public class SettingsRepository : ISettingsRepository
{
    private readonly JsonFileRepository<AppUserSettings> _inner;
    public SettingsRepository(IOptions<DataStoreOptions> o, ICurrentUserService cu) => _inner = new InnerSettingsRepo(o, cu);

    public async Task<AppUserSettings?> GetByUserIdAsync(Guid userId)
        => (await _inner.GetAllAsync()).FirstOrDefault(s => s.UserId == userId);

    public async Task<AppUserSettings> UpsertAsync(AppUserSettings settings)
    {
        var all = await _inner.GetAllOriginalAsync();
        var existing = all.FirstOrDefault(s => s.UserId == settings.UserId);
        if (existing == null) return await _inner.AddAsync(settings);
        existing.Theme = settings.Theme; existing.ColorScheme = settings.ColorScheme;
        return await _inner.UpdateAsync(existing);
    }

    private class InnerSettingsRepo : JsonFileRepository<AppUserSettings>
    {
        public InnerSettingsRepo(IOptions<DataStoreOptions> o, ICurrentUserService cu) : base(o, "settings.json", cu) { }
        protected override Guid GetId(AppUserSettings e) => e.UserId;
        protected override void SetId(AppUserSettings e, Guid id) => e.UserId = id;
    }
}

public class RefreshTokenRepository : IRefreshTokenRepository
{
    private readonly InnerRepo _inner;
    public RefreshTokenRepository(IOptions<DataStoreOptions> o, ICurrentUserService cu) => _inner = new InnerRepo(o, cu);

    public async Task<RefreshTokenData?> GetByTokenAsync(string token)
        => (await _inner.GetAllOriginalAsync()).FirstOrDefault(t => t.Token == token);

    public async Task AddAsync(RefreshTokenData token) => await _inner.AddAsync(token);

    public async Task RevokeAsync(string token)
    {
        var all = await _inner.GetAllOriginalAsync();
        var t = all.FirstOrDefault(x => x.Token == token);
        if (t == null) return;
        t.IsRevoked = true;
        await _inner.UpdateAsync(t);
    }

    public async Task RevokeAllForUserAsync(Guid userId)
    {
        var all = await _inner.GetAllOriginalAsync();
        foreach (var t in all.Where(t => t.UserId == userId))
        {
            t.IsRevoked = true;
            await _inner.UpdateAsync(t);
        }
    }

    private class InnerRepo : JsonFileRepository<RefreshTokenData>
    {
        public InnerRepo(IOptions<DataStoreOptions> o, ICurrentUserService cu) : base(o, "refresh-tokens.json", cu) { }
        protected override Guid GetId(RefreshTokenData e) => e.Id;
        protected override void SetId(RefreshTokenData e, Guid id) => e.Id = id;
    }
}
