// =============================================================================
// SAMPLE SQL REPOSITORY PATTERN — For future EF Core migration
// =============================================================================
// To swap from JSON to SQL:
//   1. Install Microsoft.EntityFrameworkCore and your DB driver NuGet packages
//   2. Create PapDbContext (see below) and configure your connection string
//   3. Register SQL repos instead of JSON repos in InfrastructureDI.cs
//   4. Run EF migrations: dotnet ef migrations add InitialCreate
// =============================================================================

/*
using Microsoft.EntityFrameworkCore;
using PAP.Application.Interfaces;
using PAP.Domain.Entities;

namespace PAP.Infrastructure.Repositories.Sql;

// ─── DbContext ────────────────────────────────────────────────────────────────

public class PapDbContext : DbContext
{
    public PapDbContext(DbContextOptions<PapDbContext> options) : base(options) { }

    public DbSet<User> Users => Set<User>();
    public DbSet<Role> Roles => Set<Role>();
    public DbSet<Domain> Domains => Set<Domain>();
    public DbSet<Policy> Policies => Set<Policy>();
    public DbSet<SharedRule> SharedRules => Set<SharedRule>();
    public DbSet<ABACScenario> Scenarios => Set<ABACScenario>();
    public DbSet<TestCase> TestCases => Set<TestCase>();
    public DbSet<AppUserSettings> Settings => Set<AppUserSettings>();
    public DbSet<RefreshTokenData> RefreshTokens => Set<RefreshTokenData>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        // Configure owned types (JSON columns in SQL Server 2022 / EF8+)
        modelBuilder.Entity<Policy>().OwnsOne(p => p.PolicyDefinition, pd =>
        {
            pd.ToJson(); // EF Core 8 JSON column support
        });
        modelBuilder.Entity<SharedRule>().OwnsOne(r => r.Target, t => t.ToJson());
        modelBuilder.Entity<User>().OwnsMany(u => u.DomainRoles, dr => dr.ToJson());

        // Alternatively, store complex objects as JSON strings manually:
        // modelBuilder.Entity<Policy>()
        //     .Property(p => p.PolicyDefinition)
        //     .HasConversion(
        //         v => JsonSerializer.Serialize(v, null),
        //         v => JsonSerializer.Deserialize<PolicyDefinition>(v, null)!);
    }
}

// ─── Generic SQL Repository Base ──────────────────────────────────────────────

public abstract class SqlRepository<T> where T : class
{
    protected readonly PapDbContext _db;
    protected SqlRepository(PapDbContext db) => _db = db;

    protected abstract IQueryable<T> Query();
    protected abstract Task<T?> FindByIdAsync(Guid id);

    public async Task<List<T>> GetAllAsync() => await Query().ToListAsync();
    public async Task<T?> GetByIdAsync(Guid id) => await FindByIdAsync(id);

    public async Task<T> AddAsync(T entity)
    {
        _db.Set<T>().Add(entity);
        await _db.SaveChangesAsync();
        return entity;
    }

    public async Task<T> UpdateAsync(T entity)
    {
        _db.Set<T>().Update(entity);
        await _db.SaveChangesAsync();
        return entity;
    }

    public async Task DeleteAsync(Guid id)
    {
        var entity = await FindByIdAsync(id) ?? throw new KeyNotFoundException();
        _db.Set<T>().Remove(entity);
        await _db.SaveChangesAsync();
    }

    public async Task<bool> ExistsAsync(Guid id) => await FindByIdAsync(id) != null;
}

// ─── Sample: SQL User Repository ─────────────────────────────────────────────

public class SqlUserRepository : SqlRepository<User>, IUserRepository
{
    public SqlUserRepository(PapDbContext db) : base(db) { }

    protected override IQueryable<User> Query() => _db.Users;
    protected override Task<User?> FindByIdAsync(Guid id) =>
        _db.Users.FirstOrDefaultAsync(u => u.Id == id);

    public Task<User?> GetByUsernameAsync(string username) =>
        _db.Users.FirstOrDefaultAsync(u => u.Username == username);

    public Task<User?> GetByEmailAsync(string email) =>
        _db.Users.FirstOrDefaultAsync(u => u.Email == email);

    public Task<bool> UsernameExistsAsync(string username, Guid? excludeId = null) =>
        _db.Users.AnyAsync(u => u.Username == username && (excludeId == null || u.Id != excludeId));
}

// ─── Sample: SQL Policy Repository ───────────────────────────────────────────

public class SqlPolicyRepository : SqlRepository<Policy>, IPolicyRepository
{
    public SqlPolicyRepository(PapDbContext db) : base(db) { }

    protected override IQueryable<Policy> Query() => _db.Policies;
    protected override Task<Policy?> FindByIdAsync(Guid id) =>
        _db.Policies.FirstOrDefaultAsync(p => p.Id == id);

    public Task<List<Policy>> SearchAsync(string? search) =>
        string.IsNullOrWhiteSpace(search)
            ? _db.Policies.OrderBy(p => p.Name).ToListAsync()
            : _db.Policies.Where(p => EF.Functions.Like(p.Name, $"%{search}%"))
                .OrderBy(p => p.Name).ToListAsync();
}

// ─── DI Registration for SQL ──────────────────────────────────────────────────
// In InfrastructureDI.cs, replace JSON repo registrations with:
//
//   services.AddDbContext<PapDbContext>(options =>
//       options.UseSqlServer(configuration.GetConnectionString("DefaultConnection")));
//   services.AddScoped<IUserRepository, SqlUserRepository>();
//   services.AddScoped<IPolicyRepository, SqlPolicyRepository>();
//   // ... and so on for other repositories

*/
