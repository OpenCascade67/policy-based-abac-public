using MediatR;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using PAP.Application.Features.SharedRules;
using PAP.Application.Features.SharedRules.DTOs;

namespace PAP.API.Controllers;

[ApiController]
[Route("api/shared-rules")]
[Authorize]
public class SharedRulesController : ControllerBase
{
    private readonly IMediator _mediator;
    public SharedRulesController(IMediator mediator) => _mediator = mediator;

    [HttpGet]
    public async Task<IActionResult> GetSharedRules()
        => Ok(await _mediator.Send(new GetSharedRulesQuery()));

    [HttpGet("{id:guid}")]
    public async Task<IActionResult> GetSharedRule(Guid id)
        => Ok(await _mediator.Send(new GetSharedRuleByIdQuery(id)));

    [HttpPost]
    public async Task<IActionResult> CreateSharedRule([FromBody] CreateSharedRuleRequest request)
    {
        var result = await _mediator.Send(new CreateSharedRuleCommand(request));
        return CreatedAtAction(nameof(GetSharedRule), new { id = result.Id }, result);
    }

    [HttpPut("{id:guid}")]
    public async Task<IActionResult> UpdateSharedRule(Guid id, [FromBody] UpdateSharedRuleRequest request)
        => Ok(await _mediator.Send(new UpdateSharedRuleCommand(id, request)));

    [HttpDelete("{id:guid}")]
    public async Task<IActionResult> DeleteSharedRule(Guid id)
    {
        await _mediator.Send(new DeleteSharedRuleCommand(id));
        return NoContent();
    }
}
