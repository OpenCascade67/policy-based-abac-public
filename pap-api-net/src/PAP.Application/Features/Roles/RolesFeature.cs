using AutoMapper;
using FluentValidation;
using MediatR;
using PAP.Application.Common;
using PAP.Application.Common.Exceptions;
using PAP.Application.Features.Roles.DTOs;
using PAP.Application.Interfaces;
using PAP.Domain.Entities;

namespace PAP.Application.Features.Roles;

// ─── Mapping ──────────────────────────────────────────────────────────────────

public class RoleMappingProfile : AutoMapper.Profile
{
    public RoleMappingProfile()
    {
        CreateMap<Role, RoleDto>()
            .ForMember(d => d.Id, o => o.MapFrom(s => s.Id.ToString()))
            .ForMember(d => d.CreatedById, o => o.MapFrom(s => s.CreatedById.HasValue ? s.CreatedById.Value.ToString() : null))
            .ForMember(d => d.LastModifiedById, o => o.MapFrom(s => s.LastModifiedById.HasValue ? s.LastModifiedById.Value.ToString() : null));
    }
}

// ─── Queries ─────────────────────────────────────────────────────────────────

public record GetRolesQuery : IRequest<List<RoleDto>>;

public class GetRolesQueryHandler : IRequestHandler<GetRolesQuery, List<RoleDto>>
{
    private readonly IRoleRepository _roles;
    private readonly ICurrentUserService _currentUser;
    private readonly IMapper _mapper;
    public GetRolesQueryHandler(IRoleRepository roles, ICurrentUserService cu, IMapper mapper)
    { _roles = roles; _currentUser = cu; _mapper = mapper; }

    public async Task<List<RoleDto>> Handle(GetRolesQuery request, CancellationToken ct)
    {
        if (!_currentUser.IsAuthenticated) throw new ForbiddenException();
        var all = await _roles.GetAllAsync();
        return _mapper.Map<List<RoleDto>>(all);
    }
}

// ─── Create Role ──────────────────────────────────────────────────────────────

public record CreateRoleCommand(CreateRoleRequest Request) : IRequest<RoleDto>;

public class CreateRoleCommandValidator : AbstractValidator<CreateRoleCommand>
{
    public CreateRoleCommandValidator()
    {
        RuleFor(x => x.Request.Name).NotEmpty().MaximumLength(100);
    }
}

public class CreateRoleCommandHandler : IRequestHandler<CreateRoleCommand, RoleDto>
{
    private readonly IRoleRepository _roles;
    private readonly ICurrentUserService _currentUser;
    private readonly IMapper _mapper;
    public CreateRoleCommandHandler(IRoleRepository roles, ICurrentUserService cu, IMapper mapper)
    { _roles = roles; _currentUser = cu; _mapper = mapper; }

    public async Task<RoleDto> Handle(CreateRoleCommand request, CancellationToken ct)
    {
        if (!_currentUser.IsAuthenticated) throw new ForbiddenException();
        if (await _roles.NameExistsAsync(request.Request.Name))
            throw new ConflictException($"Role '{request.Request.Name}' already exists.");

        var role = new Role
        {
            Id = Guid.NewGuid(), Name = request.Request.Name,
            Description = request.Request.Description, IsSystemRole = false,
            Permissions = new List<string>(),
            CreatedAt = DateTime.UtcNow, LastModifiedAt = DateTime.UtcNow,
            CreatedById = _currentUser.UserId, LastModifiedById = _currentUser.UserId
        };
        return _mapper.Map<RoleDto>(await _roles.AddAsync(role));
    }
}

// ─── Update Role ──────────────────────────────────────────────────────────────

public record UpdateRoleCommand(Guid Id, UpdateRoleRequest Request) : IRequest<RoleDto>;

public class UpdateRoleCommandHandler : IRequestHandler<UpdateRoleCommand, RoleDto>
{
    private readonly IRoleRepository _roles;
    private readonly ICurrentUserService _currentUser;
    private readonly IMapper _mapper;
    public UpdateRoleCommandHandler(IRoleRepository roles, ICurrentUserService cu, IMapper mapper)
    { _roles = roles; _currentUser = cu; _mapper = mapper; }

    public async Task<RoleDto> Handle(UpdateRoleCommand request, CancellationToken ct)
    {
        if (!_currentUser.IsAuthenticated) throw new ForbiddenException();
        var role = await _roles.GetByIdAsync(request.Id)
            ?? throw new NotFoundException(nameof(Role), request.Id);
        if (role.IsSystemRole) throw new ForbiddenException("System roles cannot be modified.");

        role.Name = request.Request.Name;
        role.Description = request.Request.Description;
        role.LastModifiedAt = DateTime.UtcNow;
        role.LastModifiedById = _currentUser.UserId;
        return _mapper.Map<RoleDto>(await _roles.UpdateAsync(role));
    }
}

// ─── Delete Role ──────────────────────────────────────────────────────────────

public record DeleteRoleCommand(Guid Id) : IRequest;

public class DeleteRoleCommandHandler : IRequestHandler<DeleteRoleCommand>
{
    private readonly IRoleRepository _roles;
    private readonly ICurrentUserService _currentUser;
    public DeleteRoleCommandHandler(IRoleRepository roles, ICurrentUserService cu)
    { _roles = roles; _currentUser = cu; }

    public async Task Handle(DeleteRoleCommand request, CancellationToken ct)
    {
        if (!_currentUser.IsAuthenticated) throw new ForbiddenException();
        var role = await _roles.GetByIdAsync(request.Id)
            ?? throw new NotFoundException(nameof(Role), request.Id);
        if (role.IsSystemRole) throw new ForbiddenException("System roles cannot be deleted.");
        await _roles.DeleteAsync(request.Id);
    }
}
