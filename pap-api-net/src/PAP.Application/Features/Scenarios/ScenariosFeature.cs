using AutoMapper;
using FluentValidation;
using MediatR;
using PAP.Application.Common.Exceptions;
using PAP.Application.Features.Scenarios.DTOs;
using PAP.Application.Interfaces;
using PAP.Domain.Entities;

namespace PAP.Application.Features.Scenarios;

public class ScenarioMappingProfile : AutoMapper.Profile
{
    public ScenarioMappingProfile()
    {
        CreateMap<ABACScenario, ScenarioDto>()
            .ForMember(d => d.Id, o => o.MapFrom(s => s.Id.ToString()))
            .ForMember(d => d.TestCases, o => o.MapFrom(s => s.TestCases.Select(id => id.ToString()).ToList()))
            .ForMember(d => d.CreatedById, o => o.MapFrom(s => s.CreatedById.HasValue ? s.CreatedById.Value.ToString() : null))
            .ForMember(d => d.LastModifiedById, o => o.MapFrom(s => s.LastModifiedById.HasValue ? s.LastModifiedById.Value.ToString() : null));
    }
}

public record GetScenariosQuery : IRequest<List<ScenarioDto>>;
public class GetScenariosQueryHandler : IRequestHandler<GetScenariosQuery, List<ScenarioDto>>
{
    private readonly IScenarioRepository _scenarios; private readonly ICurrentUserService _cu; private readonly IMapper _mapper;
    public GetScenariosQueryHandler(IScenarioRepository s, ICurrentUserService cu, IMapper m) { _scenarios = s; _cu = cu; _mapper = m; }
    public async Task<List<ScenarioDto>> Handle(GetScenariosQuery request, CancellationToken ct)
    {
        if (!_cu.IsAuthenticated) throw new ForbiddenException();
        return _mapper.Map<List<ScenarioDto>>(await _scenarios.GetAllAsync());
    }
}

public record CreateScenarioCommand(CreateScenarioRequest Request) : IRequest<ScenarioDto>;
public class CreateScenarioCommandValidator : AbstractValidator<CreateScenarioCommand>
{
    public CreateScenarioCommandValidator() { RuleFor(x => x.Request.Name).NotEmpty().MaximumLength(200); }
}
public class CreateScenarioCommandHandler : IRequestHandler<CreateScenarioCommand, ScenarioDto>
{
    private readonly IScenarioRepository _scenarios; private readonly ICurrentUserService _cu; private readonly IMapper _mapper;
    public CreateScenarioCommandHandler(IScenarioRepository s, ICurrentUserService cu, IMapper m) { _scenarios = s; _cu = cu; _mapper = m; }
    public async Task<ScenarioDto> Handle(CreateScenarioCommand request, CancellationToken ct)
    {
        if (!_cu.IsAuthenticated) throw new ForbiddenException();
        var scenario = new ABACScenario
        {
            Id = Guid.NewGuid(), Name = request.Request.Name, Description = request.Request.Description,
            Value = request.Request.Value, CreatedAt = DateTime.UtcNow, LastModifiedAt = DateTime.UtcNow,
            CreatedById = _cu.UserId, LastModifiedById = _cu.UserId
        };
        return _mapper.Map<ScenarioDto>(await _scenarios.AddAsync(scenario));
    }
}

public record UpdateScenarioCommand(Guid Id, UpdateScenarioRequest Request) : IRequest<ScenarioDto>;
public class UpdateScenarioCommandHandler : IRequestHandler<UpdateScenarioCommand, ScenarioDto>
{
    private readonly IScenarioRepository _scenarios; private readonly ICurrentUserService _cu; private readonly IMapper _mapper;
    public UpdateScenarioCommandHandler(IScenarioRepository s, ICurrentUserService cu, IMapper m) { _scenarios = s; _cu = cu; _mapper = m; }
    public async Task<ScenarioDto> Handle(UpdateScenarioCommand request, CancellationToken ct)
    {
        if (!_cu.IsAuthenticated) throw new ForbiddenException();
        var scenario = await _scenarios.GetByIdAsync(request.Id) ?? throw new NotFoundException(nameof(ABACScenario), request.Id);
        scenario.Name = request.Request.Name; scenario.Description = request.Request.Description;
        scenario.Value = request.Request.Value; scenario.LastModifiedAt = DateTime.UtcNow;
        scenario.LastModifiedById = _cu.UserId;
        return _mapper.Map<ScenarioDto>(await _scenarios.UpdateAsync(scenario));
    }
}

public record DeleteScenarioCommand(Guid Id) : IRequest;
public class DeleteScenarioCommandHandler : IRequestHandler<DeleteScenarioCommand>
{
    private readonly IScenarioRepository _scenarios; private readonly ICurrentUserService _cu;
    public DeleteScenarioCommandHandler(IScenarioRepository s, ICurrentUserService cu) { _scenarios = s; _cu = cu; }
    public async Task Handle(DeleteScenarioCommand request, CancellationToken ct)
    {
        if (!_cu.IsAuthenticated) throw new ForbiddenException();
        if (!await _scenarios.ExistsAsync(request.Id)) throw new NotFoundException(nameof(ABACScenario), request.Id);
        await _scenarios.DeleteAsync(request.Id);
    }
}
