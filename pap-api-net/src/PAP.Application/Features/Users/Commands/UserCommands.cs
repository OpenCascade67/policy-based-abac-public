using AutoMapper;
using FluentValidation;
using MediatR;
using PAP.Application.Common;
using PAP.Application.Common.Exceptions;
using PAP.Application.Features.Users.DTOs;
using PAP.Application.Interfaces;
using PAP.Domain.Entities;
using PAP.Domain.Enums;
using PAP.Domain.ValueObjects;

namespace PAP.Application.Features.Users.Commands;

// ─── Create User ──────────────────────────────────────────────────────────────

public record CreateUserCommand(CreateUserRequest Request) : IRequest<UserDto>;

public class CreateUserCommandValidator : AbstractValidator<CreateUserCommand>
{
    public CreateUserCommandValidator()
    {
        RuleFor(x => x.Request.Username).NotEmpty().MaximumLength(50);
        RuleFor(x => x.Request.Password).NotEmpty().MinimumLength(6);
        RuleFor(x => x.Request.Status).Must(s => s == "active" || s == "inactive");
    }
}

public class CreateUserCommandHandler : IRequestHandler<CreateUserCommand, UserDto>
{
    private readonly IUserRepository _users;
    private readonly ICurrentUserService _currentUser;
    private readonly IPasswordHasher _hasher;
    private readonly IMapper _mapper;

    public CreateUserCommandHandler(IUserRepository users, ICurrentUserService currentUser, IPasswordHasher hasher, IMapper mapper)
    { _users = users; _currentUser = currentUser; _hasher = hasher; _mapper = mapper; }

    public async Task<UserDto> Handle(CreateUserCommand request, CancellationToken ct)
    {
        if (!HasPermission("manage_users")) throw new ForbiddenException();
        if (await _users.UsernameExistsAsync(request.Request.Username))
            throw new ConflictException($"Username '{request.Request.Username}' already exists.");

        var user = new User
        {
            Id = Guid.NewGuid(),
            Username = request.Request.Username,
            PasswordHash = _hasher.Hash(request.Request.Password),
            UserType = UserType.Local,
            Status = request.Request.Status == "active" ? UserStatus.Active : UserStatus.Inactive,
            DomainRoles = request.Request.DomainRoles.Select(dr => new DomainRole
            {
                DomainId = Guid.Parse(dr.DomainId),
                Roles = dr.Roles.Select(Guid.Parse).ToList()
            }).ToList(),
            CreatedAt = DateTime.UtcNow,
            LastModifiedAt = DateTime.UtcNow,
            CreatedById = _currentUser.UserId,
            LastModifiedById = _currentUser.UserId
        };

        var created = await _users.AddAsync(user);
        return _mapper.Map<UserDto>(created);
    }

    private bool HasPermission(string p) => _currentUser.IsAuthenticated;
}

// ─── Update User ──────────────────────────────────────────────────────────────

public record UpdateUserCommand(Guid Id, UpdateUserRequest Request) : IRequest<UserDto>;

public class UpdateUserCommandValidator : AbstractValidator<UpdateUserCommand>
{
    public UpdateUserCommandValidator()
    {
        RuleFor(x => x.Id).NotEmpty();
        RuleFor(x => x.Request.Status).Must(s => s == "active" || s == "inactive");
        When(x => !string.IsNullOrEmpty(x.Request.Password),
            () => RuleFor(x => x.Request.Password).MinimumLength(6));
    }
}

public class UpdateUserCommandHandler : IRequestHandler<UpdateUserCommand, UserDto>
{
    private readonly IUserRepository _users;
    private readonly ICurrentUserService _currentUser;
    private readonly IPasswordHasher _hasher;
    private readonly IMapper _mapper;

    public UpdateUserCommandHandler(IUserRepository users, ICurrentUserService currentUser, IPasswordHasher hasher, IMapper mapper)
    { _users = users; _currentUser = currentUser; _hasher = hasher; _mapper = mapper; }

    public async Task<UserDto> Handle(UpdateUserCommand request, CancellationToken ct)
    {
        if (!_currentUser.IsAuthenticated) throw new ForbiddenException();

        var user = await _users.GetByIdAsync(request.Id)
            ?? throw new NotFoundException(nameof(User), request.Id);

        user.Status = request.Request.Status == "active" ? UserStatus.Active : UserStatus.Inactive;
        user.DomainRoles = request.Request.DomainRoles.Select(dr => new DomainRole
        {
            DomainId = Guid.Parse(dr.DomainId),
            Roles = dr.Roles.Select(Guid.Parse).ToList()
        }).ToList();

        if (user.UserType == UserType.Local && !string.IsNullOrEmpty(request.Request.Password))
            user.PasswordHash = _hasher.Hash(request.Request.Password);

        user.LastModifiedAt = DateTime.UtcNow;
        user.LastModifiedById = _currentUser.UserId;

        var updated = await _users.UpdateAsync(user);
        return _mapper.Map<UserDto>(updated);
    }
}

// ─── Delete User ──────────────────────────────────────────────────────────────

public record DeleteUserCommand(Guid Id) : IRequest;

public class DeleteUserCommandHandler : IRequestHandler<DeleteUserCommand>
{
    private readonly IUserRepository _users;
    private readonly ICurrentUserService _currentUser;

    public DeleteUserCommandHandler(IUserRepository users, ICurrentUserService currentUser)
    { _users = users; _currentUser = currentUser; }

    public async Task Handle(DeleteUserCommand request, CancellationToken ct)
    {
        if (!_currentUser.IsAuthenticated) throw new ForbiddenException();
        if (!await _users.ExistsAsync(request.Id)) throw new NotFoundException(nameof(User), request.Id);
        await _users.DeleteAsync(request.Id);
    }
}

// ─── Set Default Domain ───────────────────────────────────────────────────────

public record SetDefaultDomainCommand(Guid UserId, Guid DomainId) : IRequest;

public class SetDefaultDomainCommandHandler : IRequestHandler<SetDefaultDomainCommand>
{
    private readonly IUserRepository _users;
    private readonly ICurrentUserService _currentUser;

    public SetDefaultDomainCommandHandler(IUserRepository users, ICurrentUserService currentUser)
    { _users = users; _currentUser = currentUser; }

    public async Task Handle(SetDefaultDomainCommand request, CancellationToken ct)
    {
        if (!_currentUser.IsAuthenticated) throw new ForbiddenException();

        var user = await _users.GetByIdAsync(request.UserId)
            ?? throw new NotFoundException(nameof(User), request.UserId);

        user.DefaultDomainId = request.DomainId;
        user.LastModifiedAt = DateTime.UtcNow;
        user.LastModifiedById = _currentUser.UserId;
        await _users.UpdateAsync(user);
    }
}
