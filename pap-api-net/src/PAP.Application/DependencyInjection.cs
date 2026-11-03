using FluentValidation;
using MediatR;
using Microsoft.Extensions.DependencyInjection;
using PAP.Application.Common.Behaviours;

namespace PAP.Application;

public static class DependencyInjection
{
    public static IServiceCollection AddApplicationServices(this IServiceCollection services)
    {
        services.AddMediatR(cfg => cfg.RegisterServicesFromAssembly(typeof(DependencyInjection).Assembly));
        services.AddValidatorsFromAssembly(typeof(DependencyInjection).Assembly);
        services.AddTransient(typeof(IPipelineBehavior<,>), typeof(ValidationBehaviour<,>));
        services.AddAutoMapper(typeof(DependencyInjection).Assembly);
        return services;
    }
}
