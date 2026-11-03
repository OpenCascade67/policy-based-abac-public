using MediatR;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using PAP.Application.Features.Scenarios;
using PAP.Application.Features.Scenarios.DTOs;

namespace PAP.API.Controllers;

[ApiController]
[Route("api/scenarios")]
[Authorize]
public class ScenariosController : ControllerBase
{
    private readonly IMediator _mediator;
    public ScenariosController(IMediator mediator) => _mediator = mediator;

    [HttpGet]
    public async Task<IActionResult> GetScenarios()
        => Ok(await _mediator.Send(new GetScenariosQuery()));

    [HttpPost]
    public async Task<IActionResult> CreateScenario([FromBody] CreateScenarioRequest request)
        => Ok(await _mediator.Send(new CreateScenarioCommand(request)));

    [HttpPut("{id:guid}")]
    public async Task<IActionResult> UpdateScenario(Guid id, [FromBody] UpdateScenarioRequest request)
        => Ok(await _mediator.Send(new UpdateScenarioCommand(id, request)));

    [HttpDelete("{id:guid}")]
    public async Task<IActionResult> DeleteScenario(Guid id)
    {
        await _mediator.Send(new DeleteScenarioCommand(id));
        return NoContent();
    }
}
