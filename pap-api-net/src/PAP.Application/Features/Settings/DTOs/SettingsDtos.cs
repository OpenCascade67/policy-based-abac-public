namespace PAP.Application.Features.Settings.DTOs;

public class AppSettingsDto
{
    public string Theme { get; set; } = "light";
    public string ColorScheme { get; set; } = "blue";
}

public class UpdateSettingsRequest
{
    public string Theme { get; set; } = "light";
    public string ColorScheme { get; set; } = "blue";
}
