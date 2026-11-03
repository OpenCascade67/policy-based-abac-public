using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using PAP.Application.Interfaces;
using PAP.Infrastructure.Repositories.JsonFile;
using PAP.Infrastructure.Services;
using DomainEntity = PAP.Domain.Entities.Domain;

namespace PAP.Infrastructure;

public static class DependencyInjection
{
    public static IServiceCollection AddInfrastructureServices(
        this IServiceCollection services, IConfiguration configuration)
    {
        // Data store options
        services.Configure<DataStoreOptions>(opts =>
        {
            opts.DataFolder = configuration["DataStore:Folder"] ?? "data";
        });

        // JWT options
        services.Configure<JwtOptions>(opts =>
        {
            opts.Secret = configuration["Jwt:Secret"] ?? "DEFAULT_SECRET_CHANGE_IN_PRODUCTION_1234567890";
            opts.AccessTokenExpiryHours = int.Parse(configuration["Jwt:AccessTokenExpiryHours"] ?? "2");
        });

        // JSON File Repositories (swapped to Scoped to support ICurrentUserService injection)
        services.AddScoped<IUserRepository, UserRepository>();
        services.AddScoped<IRoleRepository, RoleRepository>();
        services.AddScoped<IDomainRepository, DomainRepository>();
        services.AddScoped<IPolicyRepository, PolicyRepository>();
        services.AddScoped<ISharedRuleRepository, SharedRuleRepository>();
        services.AddScoped<IScenarioRepository, ScenarioRepository>();
        services.AddScoped<ITestCaseRepository, TestCaseRepository>();
        services.AddScoped<ISettingsRepository, SettingsRepository>();
        services.AddScoped<IRefreshTokenRepository, RefreshTokenRepository>();

        // Services
        services.AddScoped<ITokenService, TokenService>();
        services.AddSingleton<IOtpService, OtpService>();
        services.AddScoped<IXacmlEvaluationService, XacmlEvaluationService>();
        services.AddScoped<ICurrentUserService, CurrentUserService>();
        services.AddScoped<IDataSeeder, DataSeeder>();
        services.AddSingleton<IPasswordHasher, BcryptPasswordHasher>();

        return services;
    }
}
