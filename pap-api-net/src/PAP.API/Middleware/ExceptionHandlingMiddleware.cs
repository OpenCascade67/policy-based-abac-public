using PAP.Application.Common.Exceptions;
using System.Net;
using System.Text.Json;

namespace PAP.API.Middleware;

public class ExceptionHandlingMiddleware
{
    private readonly RequestDelegate _next;
    private readonly ILogger<ExceptionHandlingMiddleware> _logger;

    public ExceptionHandlingMiddleware(RequestDelegate next, ILogger<ExceptionHandlingMiddleware> logger)
    { _next = next; _logger = logger; }

    public async Task InvokeAsync(HttpContext context)
    {
        try { await _next(context); }
        catch (Exception ex) { await HandleExceptionAsync(context, ex); }
    }

    private async Task HandleExceptionAsync(HttpContext context, Exception exception)
    {
        _logger.LogError(exception, "Unhandled exception: {Message}", exception.Message);

        var (status, title, errors) = exception switch
        {
            ValidationException ve => (HttpStatusCode.BadRequest, "Validation failed", ve.Errors),
            NotFoundException => (HttpStatusCode.NotFound, exception.Message, (IDictionary<string, string[]>?)null),
            ForbiddenException => (HttpStatusCode.Forbidden, exception.Message, (IDictionary<string, string[]>?)null),
            ConflictException => (HttpStatusCode.Conflict, exception.Message, (IDictionary<string, string[]>?)null),
            _ => (HttpStatusCode.InternalServerError, "An unexpected error occurred.", (IDictionary<string, string[]>?)null)
        };

        context.Response.ContentType = "application/problem+json";
        context.Response.StatusCode = (int)status;

        var problem = new Dictionary<string, object?>
        {
            ["type"] = $"https://httpstatuses.com/{(int)status}",
            ["title"] = title,
            ["status"] = (int)status
        };
        if (errors != null) problem["errors"] = errors;

        var json = JsonSerializer.Serialize(problem, new JsonSerializerOptions { PropertyNamingPolicy = JsonNamingPolicy.CamelCase });
        await context.Response.WriteAsync(json);
    }
}
