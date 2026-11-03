using MediatR;
using PAP.Application.Common.Exceptions;
using PAP.Application.Features.Settings.DTOs;
using PAP.Application.Interfaces;
using PAP.Domain.Entities;

namespace PAP.Application.Features.Settings;

public record GetSettingsQuery : IRequest<AppSettingsDto>;
public class GetSettingsQueryHandler : IRequestHandler<GetSettingsQuery, AppSettingsDto>
{
    private readonly ISettingsRepository _settings; private readonly ICurrentUserService _cu;
    public GetSettingsQueryHandler(ISettingsRepository s, ICurrentUserService cu) { _settings = s; _cu = cu; }
    public async Task<AppSettingsDto> Handle(GetSettingsQuery request, CancellationToken ct)
    {
        if (!_cu.IsAuthenticated) throw new ForbiddenException();
        var s = await _settings.GetByUserIdAsync(_cu.UserId!.Value)
            ?? new AppUserSettings { UserId = _cu.UserId!.Value };
        return new AppSettingsDto { Theme = s.Theme, ColorScheme = s.ColorScheme };
    }
}

public record UpdateSettingsCommand(UpdateSettingsRequest Request) : IRequest<AppSettingsDto>;
public class UpdateSettingsCommandHandler : IRequestHandler<UpdateSettingsCommand, AppSettingsDto>
{
    private readonly ISettingsRepository _settings; private readonly ICurrentUserService _cu;
    public UpdateSettingsCommandHandler(ISettingsRepository s, ICurrentUserService cu) { _settings = s; _cu = cu; }
    public async Task<AppSettingsDto> Handle(UpdateSettingsCommand request, CancellationToken ct)
    {
        if (!_cu.IsAuthenticated) throw new ForbiddenException();
        var updated = await _settings.UpsertAsync(new AppUserSettings
        { UserId = _cu.UserId!.Value, Theme = request.Request.Theme, ColorScheme = request.Request.ColorScheme });
        return new AppSettingsDto { Theme = updated.Theme, ColorScheme = updated.ColorScheme };
    }
}
