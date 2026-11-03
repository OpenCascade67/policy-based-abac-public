using AutoMapper;
using FluentValidation;
using MediatR;
using PAP.Application.Common;
using PAP.Application.Common.Exceptions;
using PAP.Application.Features.TestCases.DTOs;
using PAP.Application.Interfaces;
using PAP.Domain.Entities;
using PAP.Domain.Enums;
using PAP.Domain.ValueObjects;

namespace PAP.Application.Features.TestCases;

public class TestCaseMappingProfile : AutoMapper.Profile
{
    public TestCaseMappingProfile()
    {
        CreateMap<TestCase, TestCaseDto>()
            .ForMember(d => d.Id, o => o.MapFrom(s => s.Id.ToString()))
            .ForMember(d => d.PolicyId, o => o.MapFrom(s => s.PolicyId.HasValue ? s.PolicyId.Value.ToString() : null))
            .ForMember(d => d.ScenarioId, o => o.MapFrom(s => s.ScenarioId.HasValue ? s.ScenarioId.Value.ToString() : null))
            .ForMember(d => d.CreatedById, o => o.MapFrom(s => s.CreatedById.HasValue ? s.CreatedById.Value.ToString() : null))
            .ForMember(d => d.LastModifiedById, o => o.MapFrom(s => s.LastModifiedById.HasValue ? s.LastModifiedById.Value.ToString() : null))
            .ForMember(d => d.Result, o => o.MapFrom(s => s.Result == null ? null : new TestResultDto
            {
                Decision = s.Result.Decision.ToString(),
                MatchedPolicies = s.Result.MatchedPolicies,
                ExecutedAt = s.Result.ExecutedAt
            }));
    }
}

public record GetTestCasesQuery(int Page = 0, int PageSize = 10) : IRequest<PagedResult<TestCaseDto>>;
public class GetTestCasesQueryHandler : IRequestHandler<GetTestCasesQuery, PagedResult<TestCaseDto>>
{
    private readonly ITestCaseRepository _testCases; private readonly ICurrentUserService _cu; private readonly IMapper _mapper;
    public GetTestCasesQueryHandler(ITestCaseRepository t, ICurrentUserService cu, IMapper m) { _testCases = t; _cu = cu; _mapper = m; }
    public async Task<PagedResult<TestCaseDto>> Handle(GetTestCasesQuery request, CancellationToken ct)
    {
        if (!_cu.IsAuthenticated) throw new ForbiddenException();
        var all = await _testCases.GetAllAsync();
        var total = all.Count;
        var items = all.Skip(request.Page * request.PageSize).Take(request.PageSize).ToList();
        return new PagedResult<TestCaseDto> { Items = _mapper.Map<List<TestCaseDto>>(items), TotalCount = total, Page = request.Page, PageSize = request.PageSize };
    }
}

public record CreateTestCaseCommand(CreateTestCaseRequest Request) : IRequest<TestCaseDto>;
public class CreateTestCaseCommandValidator : AbstractValidator<CreateTestCaseCommand>
{
    public CreateTestCaseCommandValidator() { RuleFor(x => x.Request.Name).NotEmpty().MaximumLength(200); }
}
public class CreateTestCaseCommandHandler : IRequestHandler<CreateTestCaseCommand, TestCaseDto>
{
    private readonly ITestCaseRepository _testCases; private readonly ICurrentUserService _cu; private readonly IMapper _mapper;
    public CreateTestCaseCommandHandler(ITestCaseRepository t, ICurrentUserService cu, IMapper m) { _testCases = t; _cu = cu; _mapper = m; }
    public async Task<TestCaseDto> Handle(CreateTestCaseCommand request, CancellationToken ct)
    {
        if (!_cu.IsAuthenticated) throw new ForbiddenException();
        var tc = new TestCase
        {
            Id = Guid.NewGuid(), Name = request.Request.Name, Description = request.Request.Description,
            PolicyId = string.IsNullOrEmpty(request.Request.PolicyId) ? null : Guid.Parse(request.Request.PolicyId),
            ScenarioId = string.IsNullOrEmpty(request.Request.ScenarioId) ? null : Guid.Parse(request.Request.ScenarioId),
            InputData = request.Request.InputData,
            CreatedAt = DateTime.UtcNow, LastModifiedAt = DateTime.UtcNow,
            CreatedById = _cu.UserId, LastModifiedById = _cu.UserId
        };
        return _mapper.Map<TestCaseDto>(await _testCases.AddAsync(tc));
    }
}

public record UpdateTestCaseCommand(Guid Id, UpdateTestCaseRequest Request) : IRequest<TestCaseDto>;
public class UpdateTestCaseCommandHandler : IRequestHandler<UpdateTestCaseCommand, TestCaseDto>
{
    private readonly ITestCaseRepository _testCases; private readonly ICurrentUserService _cu; private readonly IMapper _mapper;
    public UpdateTestCaseCommandHandler(ITestCaseRepository t, ICurrentUserService cu, IMapper m) { _testCases = t; _cu = cu; _mapper = m; }
    public async Task<TestCaseDto> Handle(UpdateTestCaseCommand request, CancellationToken ct)
    {
        if (!_cu.IsAuthenticated) throw new ForbiddenException();
        var tc = await _testCases.GetByIdAsync(request.Id) ?? throw new NotFoundException(nameof(TestCase), request.Id);
        tc.Name = request.Request.Name; tc.Description = request.Request.Description;
        tc.PolicyId = string.IsNullOrEmpty(request.Request.PolicyId) ? null : Guid.Parse(request.Request.PolicyId);
        tc.ScenarioId = string.IsNullOrEmpty(request.Request.ScenarioId) ? null : Guid.Parse(request.Request.ScenarioId);
        tc.InputData = request.Request.InputData; tc.LastModifiedAt = DateTime.UtcNow; tc.LastModifiedById = _cu.UserId;
        if (request.Request.Result != null)
            tc.Result = new TestResult { Decision = Enum.Parse<TestDecision>(request.Request.Result.Decision), MatchedPolicies = request.Request.Result.MatchedPolicies, ExecutedAt = request.Request.Result.ExecutedAt };
        return _mapper.Map<TestCaseDto>(await _testCases.UpdateAsync(tc));
    }
}

public record DeleteTestCaseCommand(Guid Id) : IRequest;
public class DeleteTestCaseCommandHandler : IRequestHandler<DeleteTestCaseCommand>
{
    private readonly ITestCaseRepository _testCases; private readonly ICurrentUserService _cu;
    public DeleteTestCaseCommandHandler(ITestCaseRepository t, ICurrentUserService cu) { _testCases = t; _cu = cu; }
    public async Task Handle(DeleteTestCaseCommand request, CancellationToken ct)
    {
        if (!_cu.IsAuthenticated) throw new ForbiddenException();
        if (!await _testCases.ExistsAsync(request.Id)) throw new NotFoundException(nameof(TestCase), request.Id);
        await _testCases.DeleteAsync(request.Id);
    }
}

public record RunTestCaseCommand(Guid Id, string? InputData = null) : IRequest<RunTestCaseResponse>;
public class RunTestCaseCommandHandler : IRequestHandler<RunTestCaseCommand, RunTestCaseResponse>
{
    private readonly ITestCaseRepository _testCases; private readonly IXacmlEvaluationService _xacml; private readonly ICurrentUserService _cu; private readonly IMapper _mapper;
    public RunTestCaseCommandHandler(ITestCaseRepository t, IXacmlEvaluationService x, ICurrentUserService cu, IMapper m) { _testCases = t; _xacml = x; _cu = cu; _mapper = m; }
    public async Task<RunTestCaseResponse> Handle(RunTestCaseCommand request, CancellationToken ct)
    {
        if (!_cu.IsAuthenticated) throw new ForbiddenException();
        var tc = await _testCases.GetByIdAsync(request.Id) ?? throw new NotFoundException(nameof(TestCase), request.Id);
        var inputToUse = request.InputData ?? tc.InputData;
        var result = await _xacml.EvaluateAsync(new EvaluationRequest { PolicyId = tc.PolicyId, InputData = inputToUse });
        tc.Result = new TestResult { Decision = Enum.Parse<TestDecision>(result.Decision), MatchedPolicies = result.MatchedPolicies, ExecutedAt = DateTime.UtcNow };
        tc.LastModifiedAt = DateTime.UtcNow;
        await _testCases.UpdateAsync(tc);
        return new RunTestCaseResponse { Decision = result.Decision, MatchedPolicies = result.MatchedPolicies, ExecutedAt = tc.Result.ExecutedAt };
    }
}
