using FluentValidation;
using MediatR;
using PAP.Application.Common.Exceptions;
using PAP.Application.Features.Auth.DTOs;
using PAP.Application.Interfaces;
using PAP.Domain.Entities;
using PAP.Domain.Enums;
using PAP.Domain.ValueObjects;

namespace PAP.Application.Features.Auth.Commands;

// ─── Login ───────────────────────────────────────────────────────────────────

public record LoginCommand(string Username, string Password) : IRequest<AuthResponse>;

public class LoginCommandValidator : AbstractValidator<LoginCommand>
{
    public LoginCommandValidator()
    {
        RuleFor(x => x.Username).NotEmpty();
        RuleFor(x => x.Password).NotEmpty();
    }
}

public class LoginCommandHandler : IRequestHandler<LoginCommand, AuthResponse>
{
    private readonly IUserRepository _users;
    private readonly ITokenService _tokens;
    private readonly IRefreshTokenRepository _refreshTokens;
    private readonly IPasswordHasher _hasher;

    public LoginCommandHandler(IUserRepository users, ITokenService tokens,
        IRefreshTokenRepository refreshTokens, IPasswordHasher hasher)
    {
        _users = users; _tokens = tokens; _refreshTokens = refreshTokens; _hasher = hasher;
    }

    public async Task<AuthResponse> Handle(LoginCommand request, CancellationToken ct)
    {
        var user = await _users.GetByUsernameAsync(request.Username)
            ?? throw new ForbiddenException("Invalid username or password.");

        if (user.UserType != UserType.Local || user.Status != UserStatus.Active)
            throw new ForbiddenException("Invalid username or password or account is inactive.");

        if (!_hasher.Verify(request.Password, user.PasswordHash))
            throw new ForbiddenException("Invalid username or password.");

        var domainId = user.DefaultDomainId ?? user.DomainRoles.FirstOrDefault()?.DomainId;
        var accessToken = _tokens.GenerateAccessToken(user, domainId);
        var refreshToken = _tokens.GenerateRefreshToken();

        await _refreshTokens.AddAsync(new RefreshTokenData
        {
            Token = refreshToken, UserId = user.Id, CurrentDomainId = domainId,
            ExpiresAt = DateTime.UtcNow.AddDays(7)
        });

        return BuildResponse(user, accessToken, refreshToken, domainId);
    }

    internal static AuthResponse BuildResponse(User user, string access, string refresh, Guid? domainId) => new()
    {
        AccessToken = access, RefreshToken = refresh,
        ExpiresAt = DateTime.UtcNow.AddHours(2),
        CurrentDomainId = domainId?.ToString(),
        User = new UserSummaryDto
        {
            Id = user.Id.ToString(), Username = user.Username, Email = user.Email,
            UserType = user.UserType.ToString().ToLower(),
            DefaultDomainId = user.DefaultDomainId?.ToString(),
            DomainRoles = user.DomainRoles.Select(dr => new DomainRoleDto
            {
                DomainId = dr.DomainId.ToString(),
                Roles = dr.Roles.Select(r => r.ToString()).ToList()
            }).ToList()
        }
    };
}

// ─── Send OTP ─────────────────────────────────────────────────────────────────

public record SendOtpCommand(string Email) : IRequest<string>;

public class SendOtpCommandValidator : AbstractValidator<SendOtpCommand>
{
    public SendOtpCommandValidator()
    {
        RuleFor(x => x.Email).NotEmpty().EmailAddress();
    }
}

public class SendOtpCommandHandler : IRequestHandler<SendOtpCommand, string>
{
    private readonly IOtpService _otp;
    public SendOtpCommandHandler(IOtpService otp) => _otp = otp;

    public Task<string> Handle(SendOtpCommand request, CancellationToken ct)
    {
        var code = _otp.GenerateOtp(request.Email);
        // In production: send email. For demo: return code in response.
        return Task.FromResult(code);
    }
}

// ─── Verify OTP ───────────────────────────────────────────────────────────────

public record VerifyOtpCommand(string Email, string Code) : IRequest<AuthResponse>;

public class VerifyOtpCommandValidator : AbstractValidator<VerifyOtpCommand>
{
    public VerifyOtpCommandValidator()
    {
        RuleFor(x => x.Email).NotEmpty().EmailAddress();
        RuleFor(x => x.Code).NotEmpty().Length(6);
    }
}

public class VerifyOtpCommandHandler : IRequestHandler<VerifyOtpCommand, AuthResponse>
{
    private readonly IUserRepository _users;
    private readonly IOtpService _otp;
    private readonly ITokenService _tokens;
    private readonly IRefreshTokenRepository _refreshTokens;

    public VerifyOtpCommandHandler(IUserRepository users, IOtpService otp,
        ITokenService tokens, IRefreshTokenRepository refreshTokens)
    {
        _users = users; _otp = otp; _tokens = tokens; _refreshTokens = refreshTokens;
    }

    public async Task<AuthResponse> Handle(VerifyOtpCommand request, CancellationToken ct)
    {
        if (!_otp.VerifyOtp(request.Email, request.Code))
            throw new ForbiddenException("Invalid OTP code.");

        _otp.ClearOtp(request.Email);

        var user = await _users.GetByEmailAsync(request.Email);
        if (user == null)
        {
            user = await _users.AddAsync(new User
            {
                Id = Guid.NewGuid(), Username = request.Email.Split('@')[0],
                Email = request.Email, UserType = UserType.Email, Status = UserStatus.Active,
                CreatedAt = DateTime.UtcNow, LastModifiedAt = DateTime.UtcNow
            });
        }

        if (user.Status != UserStatus.Active)
            throw new ForbiddenException("Account is inactive.");

        var domainId = user.DefaultDomainId ?? user.DomainRoles.FirstOrDefault()?.DomainId;
        var accessToken = _tokens.GenerateAccessToken(user, domainId);
        var refreshToken = _tokens.GenerateRefreshToken();

        await _refreshTokens.AddAsync(new RefreshTokenData
        {
            Token = refreshToken, UserId = user.Id, CurrentDomainId = domainId,
            ExpiresAt = DateTime.UtcNow.AddDays(7)
        });

        return LoginCommandHandler.BuildResponse(user, accessToken, refreshToken, domainId);
    }
}

// ─── Refresh Token ────────────────────────────────────────────────────────────

public record RefreshTokenCommand(string RefreshToken) : IRequest<AuthResponse>;

public class RefreshTokenCommandHandler : IRequestHandler<RefreshTokenCommand, AuthResponse>
{
    private readonly IRefreshTokenRepository _refreshTokens;
    private readonly IUserRepository _users;
    private readonly ITokenService _tokens;

    public RefreshTokenCommandHandler(IRefreshTokenRepository refreshTokens,
        IUserRepository users, ITokenService tokens)
    {
        _refreshTokens = refreshTokens; _users = users; _tokens = tokens;
    }

    public async Task<AuthResponse> Handle(RefreshTokenCommand request, CancellationToken ct)
    {
        var stored = await _refreshTokens.GetByTokenAsync(request.RefreshToken)
            ?? throw new ForbiddenException("Invalid refresh token.");

        if (stored.IsRevoked || stored.ExpiresAt < DateTime.UtcNow)
            throw new ForbiddenException("Refresh token has expired or been revoked.");

        var user = await _users.GetByIdAsync(stored.UserId)
            ?? throw new ForbiddenException("User not found.");

        await _refreshTokens.RevokeAsync(request.RefreshToken);

        var newAccess = _tokens.GenerateAccessToken(user, stored.CurrentDomainId);
        var newRefresh = _tokens.GenerateRefreshToken();

        await _refreshTokens.AddAsync(new RefreshTokenData
        {
            Token = newRefresh, UserId = user.Id, CurrentDomainId = stored.CurrentDomainId,
            ExpiresAt = DateTime.UtcNow.AddDays(7)
        });

        return LoginCommandHandler.BuildResponse(user, newAccess, newRefresh, stored.CurrentDomainId);
    }
}

// ─── Switch Domain ────────────────────────────────────────────────────────────

public record SwitchDomainCommand(string DomainId) : IRequest<AuthResponse>;

public class SwitchDomainCommandHandler : IRequestHandler<SwitchDomainCommand, AuthResponse>
{
    private readonly IUserRepository _users;
    private readonly ICurrentUserService _currentUser;
    private readonly ITokenService _tokens;
    private readonly IRefreshTokenRepository _refreshTokens;

    public SwitchDomainCommandHandler(IUserRepository users, ICurrentUserService currentUser,
        ITokenService tokens, IRefreshTokenRepository refreshTokens)
    {
        _users = users; _currentUser = currentUser; _tokens = tokens; _refreshTokens = refreshTokens;
    }

    public async Task<AuthResponse> Handle(SwitchDomainCommand request, CancellationToken ct)
    {
        var user = await _users.GetByIdAsync(_currentUser.UserId!.Value)
            ?? throw new NotFoundException(nameof(User), _currentUser.UserId);

        var domainId = Guid.Parse(request.DomainId);
        if (!user.DomainRoles.Any(dr => dr.DomainId == domainId))
            throw new ForbiddenException("User is not assigned to this domain.");

        var access = _tokens.GenerateAccessToken(user, domainId);
        var refresh = _tokens.GenerateRefreshToken();

        await _refreshTokens.RevokeAllForUserAsync(user.Id);
        await _refreshTokens.AddAsync(new RefreshTokenData
        {
            Token = refresh, UserId = user.Id, CurrentDomainId = domainId,
            ExpiresAt = DateTime.UtcNow.AddDays(7)
        });

        return LoginCommandHandler.BuildResponse(user, access, refresh, domainId);
    }
}

// ─── Logout ───────────────────────────────────────────────────────────────────

public record LogoutCommand(string RefreshToken) : IRequest;

public class LogoutCommandHandler : IRequestHandler<LogoutCommand>
{
    private readonly IRefreshTokenRepository _refreshTokens;
    public LogoutCommandHandler(IRefreshTokenRepository r) => _refreshTokens = r;

    public async Task Handle(LogoutCommand request, CancellationToken ct)
        => await _refreshTokens.RevokeAsync(request.RefreshToken);
}
