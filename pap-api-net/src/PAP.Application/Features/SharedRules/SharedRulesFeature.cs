using AutoMapper;
using FluentValidation;
using MediatR;
using PAP.Application.Common.Exceptions;
using PAP.Application.Features.SharedRules.DTOs;
using PAP.Application.Interfaces;
using PAP.Domain.Entities;
using PAP.Domain.Enums;

namespace PAP.Application.Features.SharedRules;

public class SharedRuleMappingProfile : AutoMapper.Profile
{
    public SharedRuleMappingProfile()
    {
        CreateMap<SharedRule, SharedRuleDto>()
            .ForMember(d => d.Id, o => o.MapFrom(s => s.Id.ToString()))
            .ForMember(d => d.Effect, o => o.MapFrom(s => s.Effect.ToString()))
            .ForMember(d => d.UsedByPolicies, o => o.MapFrom(s => s.UsedByPolicies.Select(id => id.ToString()).ToList()))
            .ForMember(d => d.CreatedById, o => o.MapFrom(s => s.CreatedById.HasValue ? s.CreatedById.Value.ToString() : null))
            .ForMember(d => d.LastModifiedById, o => o.MapFrom(s => s.LastModifiedById.HasValue ? s.LastModifiedById.Value.ToString() : null));
    }
}

public record GetSharedRulesQuery : IRequest<List<SharedRuleDto>>;
public class GetSharedRulesQueryHandler : IRequestHandler<GetSharedRulesQuery, List<SharedRuleDto>>
{
    private readonly ISharedRuleRepository _rules; private readonly ICurrentUserService _cu; private readonly IMapper _mapper;
    public GetSharedRulesQueryHandler(ISharedRuleRepository r, ICurrentUserService cu, IMapper m) { _rules = r; _cu = cu; _mapper = m; }
    public async Task<List<SharedRuleDto>> Handle(GetSharedRulesQuery request, CancellationToken ct)
    {
        if (!_cu.IsAuthenticated) throw new ForbiddenException();
        return _mapper.Map<List<SharedRuleDto>>(await _rules.GetAllAsync());
    }
}

public record GetSharedRuleByIdQuery(Guid Id) : IRequest<SharedRuleDto>;
public class GetSharedRuleByIdQueryHandler : IRequestHandler<GetSharedRuleByIdQuery, SharedRuleDto>
{
    private readonly ISharedRuleRepository _rules; private readonly ICurrentUserService _cu; private readonly IMapper _mapper;
    public GetSharedRuleByIdQueryHandler(ISharedRuleRepository r, ICurrentUserService cu, IMapper m) { _rules = r; _cu = cu; _mapper = m; }
    public async Task<SharedRuleDto> Handle(GetSharedRuleByIdQuery request, CancellationToken ct)
    {
        if (!_cu.IsAuthenticated) throw new ForbiddenException();
        var rule = await _rules.GetByIdAsync(request.Id) ?? throw new NotFoundException(nameof(SharedRule), request.Id);
        return _mapper.Map<SharedRuleDto>(rule);
    }
}

public record CreateSharedRuleCommand(CreateSharedRuleRequest Request) : IRequest<SharedRuleDto>;
public class CreateSharedRuleCommandValidator : AbstractValidator<CreateSharedRuleCommand>
{
    public CreateSharedRuleCommandValidator()
    {
        RuleFor(x => x.Request.Name).NotEmpty().MaximumLength(200);
        RuleFor(x => x.Request.Category).NotEmpty();
        RuleFor(x => x.Request.Effect).Must(e => e == "Permit" || e == "Deny");
    }
}
public class CreateSharedRuleCommandHandler : IRequestHandler<CreateSharedRuleCommand, SharedRuleDto>
{
    private readonly ISharedRuleRepository _rules; private readonly ICurrentUserService _cu; private readonly IMapper _mapper;
    public CreateSharedRuleCommandHandler(ISharedRuleRepository r, ICurrentUserService cu, IMapper m) { _rules = r; _cu = cu; _mapper = m; }
    public async Task<SharedRuleDto> Handle(CreateSharedRuleCommand request, CancellationToken ct)
    {
        if (!_cu.IsAuthenticated) throw new ForbiddenException();
        var rule = new SharedRule
        {
            Id = Guid.NewGuid(), Name = request.Request.Name, Description = request.Request.Description,
            Category = request.Request.Category,
            Effect = request.Request.Effect == "Deny" ? RuleEffect.Deny : RuleEffect.Permit,
            Target = request.Request.Target, Condition = request.Request.Condition,
            CreatedAt = DateTime.UtcNow, LastModifiedAt = DateTime.UtcNow,
            CreatedById = _cu.UserId, LastModifiedById = _cu.UserId
        };
        return _mapper.Map<SharedRuleDto>(await _rules.AddAsync(rule));
    }
}

public record UpdateSharedRuleCommand(Guid Id, UpdateSharedRuleRequest Request) : IRequest<SharedRuleDto>;
public class UpdateSharedRuleCommandHandler : IRequestHandler<UpdateSharedRuleCommand, SharedRuleDto>
{
    private readonly ISharedRuleRepository _rules; private readonly ICurrentUserService _cu; private readonly IMapper _mapper;
    public UpdateSharedRuleCommandHandler(ISharedRuleRepository r, ICurrentUserService cu, IMapper m) { _rules = r; _cu = cu; _mapper = m; }
    public async Task<SharedRuleDto> Handle(UpdateSharedRuleCommand request, CancellationToken ct)
    {
        if (!_cu.IsAuthenticated) throw new ForbiddenException();
        var rule = await _rules.GetByIdAsync(request.Id) ?? throw new NotFoundException(nameof(SharedRule), request.Id);
        rule.Name = request.Request.Name; rule.Description = request.Request.Description;
        rule.Category = request.Request.Category;
        rule.Effect = request.Request.Effect == "Deny" ? RuleEffect.Deny : RuleEffect.Permit;
        rule.Target = request.Request.Target; rule.Condition = request.Request.Condition;
        rule.LastModifiedAt = DateTime.UtcNow; rule.LastModifiedById = _cu.UserId;
        return _mapper.Map<SharedRuleDto>(await _rules.UpdateAsync(rule));
    }
}

public record DeleteSharedRuleCommand(Guid Id) : IRequest;
public class DeleteSharedRuleCommandHandler : IRequestHandler<DeleteSharedRuleCommand>
{
    private readonly ISharedRuleRepository _rules; private readonly ICurrentUserService _cu;
    public DeleteSharedRuleCommandHandler(ISharedRuleRepository r, ICurrentUserService cu) { _rules = r; _cu = cu; }
    public async Task Handle(DeleteSharedRuleCommand request, CancellationToken ct)
    {
        if (!_cu.IsAuthenticated) throw new ForbiddenException();
        if (!await _rules.ExistsAsync(request.Id)) throw new NotFoundException(nameof(SharedRule), request.Id);
        await _rules.DeleteAsync(request.Id);
    }
}
