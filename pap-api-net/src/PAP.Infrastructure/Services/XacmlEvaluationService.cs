using PAP.Application.Interfaces;

namespace PAP.Infrastructure.Services;

public class XacmlEvaluationService : IXacmlEvaluationService
{
    private readonly IPolicyRepository _policies;
    private readonly ISharedRuleRepository _sharedRules;

    public XacmlEvaluationService(IPolicyRepository policies, ISharedRuleRepository sharedRules)
    { _policies = policies; _sharedRules = sharedRules; }

    public async Task<EvaluationResult> EvaluateAsync(EvaluationRequest request)
    {
        var policies = request.PolicyId.HasValue
            ? new[] { await _policies.GetByIdAsync(request.PolicyId.Value) }.OfType<Domain.Entities.Policy>().ToList()
            : await _policies.GetAllAsync();

        return PolicyEvaluator.Evaluate(policies, await _sharedRules.GetAllAsync(), request.InputData);
    }
}
