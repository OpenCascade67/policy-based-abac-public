using AutoMapper;
using FluentValidation;
using MediatR;
using PAP.Application.Common;
using PAP.Application.Common.Exceptions;
using PAP.Application.Features.Policies.DTOs;
using PAP.Application.Interfaces;
using PAP.Domain.Entities;
using PAP.Domain.Enums;
using PAP.Domain.ValueObjects;

namespace PAP.Application.Features.Policies;

// ─── Mapping ─────────────────────────────────────────────────────────────────

public class PolicyMappingProfile : AutoMapper.Profile
{
    public PolicyMappingProfile()
    {
        CreateMap<Policy, PolicyDto>()
            .ForMember(d => d.Id, o => o.MapFrom(s => s.Id.ToString()))
            .ForMember(d => d.CreatedById, o => o.MapFrom(s => s.CreatedById.HasValue ? s.CreatedById.Value.ToString() : null))
            .ForMember(d => d.LastModifiedById, o => o.MapFrom(s => s.LastModifiedById.HasValue ? s.LastModifiedById.Value.ToString() : null))
            .ForMember(d => d.PolicyDefinition, o => o.MapFrom(s => MapDefinition(s.PolicyDefinition)));
    }

    private static PolicyDefinitionDto MapDefinition(PolicyDefinition pd) => new()
    {
        Version = pd.Version,
        PolicyId = pd.PolicyId,
        RuleCombiningAlgorithm = pd.RuleCombiningAlgorithm switch
        {
            RuleCombiningAlgorithm.PermitOverrides => "permit-overrides",
            RuleCombiningAlgorithm.DenyOverrides => "deny-overrides",
            RuleCombiningAlgorithm.FirstApplicable => "first-applicable",
            _ => "permit-overrides"
        },
        Rules = pd.Rules,
        RuleReferences = pd.RuleReferences,
        Obligations = pd.Obligations
    };
}

// ─── Queries ─────────────────────────────────────────────────────────────────

public record GetPoliciesQuery(int Page = 0, int PageSize = 10, string? Search = null)
    : IRequest<PagedResult<PolicyDto>>;

public class GetPoliciesQueryHandler : IRequestHandler<GetPoliciesQuery, PagedResult<PolicyDto>>
{
    private readonly IPolicyRepository _policies;
    private readonly ICurrentUserService _currentUser;
    private readonly IMapper _mapper;
    public GetPoliciesQueryHandler(IPolicyRepository p, ICurrentUserService cu, IMapper m)
    { _policies = p; _currentUser = cu; _mapper = m; }

    public async Task<PagedResult<PolicyDto>> Handle(GetPoliciesQuery request, CancellationToken ct)
    {
        if (!_currentUser.IsAuthenticated) throw new ForbiddenException();
        var all = await _policies.SearchAsync(request.Search);
        var total = all.Count;
        var items = all.Skip(request.Page * request.PageSize).Take(request.PageSize).ToList();
        return new PagedResult<PolicyDto>
        {
            Items = _mapper.Map<List<PolicyDto>>(items),
            TotalCount = total, Page = request.Page, PageSize = request.PageSize
        };
    }
}

public record GetPolicyByIdQuery(Guid Id) : IRequest<PolicyDto>;

public class GetPolicyByIdQueryHandler : IRequestHandler<GetPolicyByIdQuery, PolicyDto>
{
    private readonly IPolicyRepository _policies;
    private readonly ICurrentUserService _currentUser;
    private readonly IMapper _mapper;
    public GetPolicyByIdQueryHandler(IPolicyRepository p, ICurrentUserService cu, IMapper m)
    { _policies = p; _currentUser = cu; _mapper = m; }

    public async Task<PolicyDto> Handle(GetPolicyByIdQuery request, CancellationToken ct)
    {
        if (!_currentUser.IsAuthenticated) throw new ForbiddenException();
        var p = await _policies.GetByIdAsync(request.Id) ?? throw new NotFoundException(nameof(Policy), request.Id);
        return _mapper.Map<PolicyDto>(p);
    }
}

// ─── Export ───────────────────────────────────────────────────────────────────

public record ExportPolicyQuery(Guid Id, string Format) : IRequest<(string Content, string MimeType, string FileName)>;

public class ExportPolicyQueryHandler : IRequestHandler<ExportPolicyQuery, (string, string, string)>
{
    private readonly IPolicyRepository _policies;
    private readonly ICurrentUserService _currentUser;
    public ExportPolicyQueryHandler(IPolicyRepository p, ICurrentUserService cu)
    { _policies = p; _currentUser = cu; }

    public async Task<(string, string, string)> Handle(ExportPolicyQuery request, CancellationToken ct)
    {
        if (!_currentUser.IsAuthenticated) throw new ForbiddenException();
        var policy = await _policies.GetByIdAsync(request.Id)
            ?? throw new NotFoundException(nameof(Policy), request.Id);

        if (request.Format.Equals("xml", StringComparison.OrdinalIgnoreCase))
        {
            var xml = XacmlExporter.ToXml(policy);
            return (xml, "application/xml", $"{policy.Name}.xml");
        }
        var json = System.Text.Json.JsonSerializer.Serialize(XacmlExporter.ToJson(policy),
            new System.Text.Json.JsonSerializerOptions { WriteIndented = true });
        return (json, "application/json", $"{policy.Name}.json");
    }
}

// ─── XACML Exporter ──────────────────────────────────────────────────────────

internal static class XacmlExporter
{
    public static object ToJson(Policy policy)
    {
        var pd = policy.PolicyDefinition;
        return new
        {
            Policy = new
            {
                PolicyId = pd.PolicyId,
                Version = pd.Version,
                RuleCombiningAlgId = $"urn:oasis:names:tc:xacml:3.0:rule-combining-algorithm:{AlgorithmName(pd.RuleCombiningAlgorithm)}",
                Description = policy.Description,
                Target = new { },
                Rule = pd.Rules.Select(r => new { r.RuleId, Effect = r.Effect.ToString(), r.Description })
            }
        };
    }

    public static string ToXml(Policy policy)
    {
        var pd = policy.PolicyDefinition;
        var sb = new System.Text.StringBuilder();
        sb.AppendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.AppendLine($"<Policy xmlns=\"urn:oasis:names:tc:xacml:3.0:core:schema:wd-17\"");
        sb.AppendLine($"        PolicyId=\"{pd.PolicyId}\"");
        sb.AppendLine($"        Version=\"{pd.Version}\"");
        sb.AppendLine($"        RuleCombiningAlgId=\"urn:oasis:names:tc:xacml:3.0:rule-combining-algorithm:{AlgorithmName(pd.RuleCombiningAlgorithm)}\">");
        sb.AppendLine($"  <Description>{Escape(policy.Description)}</Description>");
        sb.AppendLine("  <Target/>");
        foreach (var rule in pd.Rules)
        {
            sb.AppendLine($"  <Rule RuleId=\"{rule.RuleId}\" Effect=\"{rule.Effect}\">");
            if (!string.IsNullOrEmpty(rule.Description))
                sb.AppendLine($"    <Description>{Escape(rule.Description)}</Description>");
            sb.AppendLine("    <Target/>");
            sb.AppendLine("  </Rule>");
        }
        sb.AppendLine("</Policy>");
        return sb.ToString();
    }

    private static string AlgorithmName(RuleCombiningAlgorithm alg) => alg switch
    {
        RuleCombiningAlgorithm.PermitOverrides => "permit-overrides",
        RuleCombiningAlgorithm.DenyOverrides => "deny-overrides",
        _ => "first-applicable"
    };

    private static string Escape(string s) => s
        .Replace("&", "&amp;").Replace("<", "&lt;").Replace(">", "&gt;")
        .Replace("\"", "&quot;").Replace("'", "&apos;");
}

// ─── Create Policy ────────────────────────────────────────────────────────────

public record CreatePolicyCommand(CreatePolicyRequest Request) : IRequest<PolicyDto>;

public class CreatePolicyCommandValidator : AbstractValidator<CreatePolicyCommand>
{
    public CreatePolicyCommandValidator()
    {
        RuleFor(x => x.Request.Name).NotEmpty().MaximumLength(200);
    }
}

public class CreatePolicyCommandHandler : IRequestHandler<CreatePolicyCommand, PolicyDto>
{
    private readonly IPolicyRepository _policies;
    private readonly ICurrentUserService _currentUser;
    private readonly IMapper _mapper;
    public CreatePolicyCommandHandler(IPolicyRepository p, ICurrentUserService cu, IMapper m)
    { _policies = p; _currentUser = cu; _mapper = m; }

    public async Task<PolicyDto> Handle(CreatePolicyCommand request, CancellationToken ct)
    {
        if (!_currentUser.IsAuthenticated) throw new ForbiddenException();
        var id = Guid.NewGuid();
        var policy = new Policy
        {
            Id = id,
            Name = request.Request.Name,
            Description = request.Request.Description,
            PolicyDefinition = MapDefinition(request.Request.PolicyDefinition, id.ToString()),
            CreatedAt = DateTime.UtcNow, LastModifiedAt = DateTime.UtcNow,
            CreatedById = _currentUser.UserId, LastModifiedById = _currentUser.UserId
        };
        return _mapper.Map<PolicyDto>(await _policies.AddAsync(policy));
    }

    internal static PolicyDefinition MapDefinition(PolicyDefinitionDto dto, string policyId) => new()
    {
        Version = "3.0", PolicyId = policyId,
        RuleCombiningAlgorithm = dto.RuleCombiningAlgorithm switch
        {
            "deny-overrides" => RuleCombiningAlgorithm.DenyOverrides,
            "first-applicable" => RuleCombiningAlgorithm.FirstApplicable,
            _ => RuleCombiningAlgorithm.PermitOverrides
        },
        Rules = dto.Rules,
        RuleReferences = dto.RuleReferences,
        Obligations = dto.Obligations
    };
}

// ─── Update Policy ────────────────────────────────────────────────────────────

public record UpdatePolicyCommand(Guid Id, UpdatePolicyRequest Request) : IRequest<PolicyDto>;

public class UpdatePolicyCommandValidator : AbstractValidator<UpdatePolicyCommand>
{
    public UpdatePolicyCommandValidator()
    {
        RuleFor(x => x.Request.Name).NotEmpty().MaximumLength(200);
    }
}

public class UpdatePolicyCommandHandler : IRequestHandler<UpdatePolicyCommand, PolicyDto>
{
    private readonly IPolicyRepository _policies;
    private readonly ICurrentUserService _currentUser;
    private readonly IMapper _mapper;
    public UpdatePolicyCommandHandler(IPolicyRepository p, ICurrentUserService cu, IMapper m)
    { _policies = p; _currentUser = cu; _mapper = m; }

    public async Task<PolicyDto> Handle(UpdatePolicyCommand request, CancellationToken ct)
    {
        if (!_currentUser.IsAuthenticated) throw new ForbiddenException();
        var policy = await _policies.GetByIdAsync(request.Id)
            ?? throw new NotFoundException(nameof(Policy), request.Id);

        policy.Name = request.Request.Name;
        policy.Description = request.Request.Description;
        policy.PolicyDefinition = CreatePolicyCommandHandler.MapDefinition(request.Request.PolicyDefinition, policy.Id.ToString());
        policy.LastModifiedAt = DateTime.UtcNow;
        policy.LastModifiedById = _currentUser.UserId;
        return _mapper.Map<PolicyDto>(await _policies.UpdateAsync(policy));
    }
}

// ─── Delete Policy ────────────────────────────────────────────────────────────

public record DeletePolicyCommand(Guid Id) : IRequest;

public class DeletePolicyCommandHandler : IRequestHandler<DeletePolicyCommand>
{
    private readonly IPolicyRepository _policies;
    private readonly ICurrentUserService _currentUser;
    public DeletePolicyCommandHandler(IPolicyRepository p, ICurrentUserService cu)
    { _policies = p; _currentUser = cu; }

    public async Task Handle(DeletePolicyCommand request, CancellationToken ct)
    {
        if (!_currentUser.IsAuthenticated) throw new ForbiddenException();
        if (!await _policies.ExistsAsync(request.Id)) throw new NotFoundException(nameof(Policy), request.Id);
        await _policies.DeleteAsync(request.Id);
    }
}
