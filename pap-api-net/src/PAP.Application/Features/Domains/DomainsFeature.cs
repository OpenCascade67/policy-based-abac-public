using AutoMapper;
using FluentValidation;
using MediatR;
using PAP.Application.Common;
using PAP.Application.Common.Exceptions;
using PAP.Application.Features.Domains.DTOs;
using PAP.Application.Interfaces;
using DomainEntity = PAP.Domain.Entities.Domain;

namespace PAP.Application.Features.Domains;

// ─── Mapping ─────────────────────────────────────────────────────────────────

public class DomainMappingProfile : AutoMapper.Profile
{
    public DomainMappingProfile()
    {
        CreateMap<DomainEntity, DomainDto>()
            .ForMember(d => d.Id, o => o.MapFrom(s => s.Id.ToString()))
            .ForMember(d => d.Status, o => o.MapFrom(s => s.Status.ToString().ToLower()))
            .ForMember(d => d.CreatedById, o => o.MapFrom(s => s.CreatedById.HasValue ? s.CreatedById.Value.ToString() : null))
            .ForMember(d => d.LastModifiedById, o => o.MapFrom(s => s.LastModifiedById.HasValue ? s.LastModifiedById.Value.ToString() : null));
    }
}

// ─── Queries ─────────────────────────────────────────────────────────────────

public record GetDomainsQuery : IRequest<List<DomainDto>>;

public class GetDomainsQueryHandler : IRequestHandler<GetDomainsQuery, List<DomainDto>>
{
    private readonly IDomainRepository _domains;
    private readonly ICurrentUserService _currentUser;
    private readonly IMapper _mapper;
    public GetDomainsQueryHandler(IDomainRepository d, ICurrentUserService cu, IMapper m)
    { _domains = d; _currentUser = cu; _mapper = m; }

    public async Task<List<DomainDto>> Handle(GetDomainsQuery request, CancellationToken ct)
    {
        if (!_currentUser.IsAuthenticated) throw new ForbiddenException();
        return _mapper.Map<List<DomainDto>>(await _domains.GetAllAsync());
    }
}

public record GetDomainByIdQuery(Guid Id) : IRequest<DomainDto>;

public class GetDomainByIdQueryHandler : IRequestHandler<GetDomainByIdQuery, DomainDto>
{
    private readonly IDomainRepository _domains;
    private readonly ICurrentUserService _currentUser;
    private readonly IMapper _mapper;
    public GetDomainByIdQueryHandler(IDomainRepository d, ICurrentUserService cu, IMapper m)
    { _domains = d; _currentUser = cu; _mapper = m; }

    public async Task<DomainDto> Handle(GetDomainByIdQuery request, CancellationToken ct)
    {
        if (!_currentUser.IsAuthenticated) throw new ForbiddenException();
        var domain = await _domains.GetByIdAsync(request.Id)
            ?? throw new NotFoundException("Domain", request.Id);
        return _mapper.Map<DomainDto>(domain);
    }
}

// ─── Create Domain ────────────────────────────────────────────────────────────

public record CreateDomainCommand(CreateDomainRequest Request) : IRequest<DomainDto>;

public class CreateDomainCommandValidator : AbstractValidator<CreateDomainCommand>
{
    public CreateDomainCommandValidator()
    {
        RuleFor(x => x.Request.Name).NotEmpty().MaximumLength(100);
        RuleFor(x => x.Request.Status).Must(s => s == "active" || s == "inactive");
    }
}

public class CreateDomainCommandHandler : IRequestHandler<CreateDomainCommand, DomainDto>
{
    private readonly IDomainRepository _domains;
    private readonly ICurrentUserService _currentUser;
    private readonly IMapper _mapper;
    public CreateDomainCommandHandler(IDomainRepository d, ICurrentUserService cu, IMapper m)
    { _domains = d; _currentUser = cu; _mapper = m; }

    public async Task<DomainDto> Handle(CreateDomainCommand request, CancellationToken ct)
    {
        if (!_currentUser.IsAuthenticated) throw new ForbiddenException();
        if (await _domains.NameExistsAsync(request.Request.Name))
            throw new ConflictException($"Domain '{request.Request.Name}' already exists.");

        var domain = new DomainEntity
        {
            Id = Guid.NewGuid(), Name = request.Request.Name,
            Description = request.Request.Description,
            Status = request.Request.Status == "active"
                ? PAP.Domain.Enums.DomainStatus.Active
                : PAP.Domain.Enums.DomainStatus.Inactive,
            CreatedAt = DateTime.UtcNow, LastModifiedAt = DateTime.UtcNow,
            CreatedById = _currentUser.UserId, LastModifiedById = _currentUser.UserId
        };
        return _mapper.Map<DomainDto>(await _domains.AddAsync(domain));
    }
}

// ─── Update Domain ────────────────────────────────────────────────────────────

public record UpdateDomainCommand(Guid Id, UpdateDomainRequest Request) : IRequest<DomainDto>;

public class UpdateDomainCommandHandler : IRequestHandler<UpdateDomainCommand, DomainDto>
{
    private readonly IDomainRepository _domains;
    private readonly ICurrentUserService _currentUser;
    private readonly IMapper _mapper;
    public UpdateDomainCommandHandler(IDomainRepository d, ICurrentUserService cu, IMapper m)
    { _domains = d; _currentUser = cu; _mapper = m; }

    public async Task<DomainDto> Handle(UpdateDomainCommand request, CancellationToken ct)
    {
        if (!_currentUser.IsAuthenticated) throw new ForbiddenException();
        var domain = await _domains.GetByIdAsync(request.Id)
            ?? throw new NotFoundException("Domain", request.Id);

        domain.Name = request.Request.Name;
        domain.Description = request.Request.Description;
        domain.Status = request.Request.Status == "active"
            ? PAP.Domain.Enums.DomainStatus.Active
            : PAP.Domain.Enums.DomainStatus.Inactive;
        domain.LastModifiedAt = DateTime.UtcNow;
        domain.LastModifiedById = _currentUser.UserId;
        return _mapper.Map<DomainDto>(await _domains.UpdateAsync(domain));
    }
}

// ─── Delete Domain ────────────────────────────────────────────────────────────

public record DeleteDomainCommand(Guid Id) : IRequest;

public class DeleteDomainCommandHandler : IRequestHandler<DeleteDomainCommand>
{
    private readonly IDomainRepository _domains;
    private readonly ICurrentUserService _currentUser;
    public DeleteDomainCommandHandler(IDomainRepository d, ICurrentUserService cu)
    { _domains = d; _currentUser = cu; }

    public async Task Handle(DeleteDomainCommand request, CancellationToken ct)
    {
        if (!_currentUser.IsAuthenticated) throw new ForbiddenException();
        if (!await _domains.ExistsAsync(request.Id)) throw new NotFoundException("Domain", request.Id);
        await _domains.DeleteAsync(request.Id);
    }
}
