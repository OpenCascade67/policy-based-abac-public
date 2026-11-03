using MediatR;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using PAP.Application.Features.Domains;
using PAP.Application.Features.Domains.DTOs;

namespace PAP.API.Controllers;

[ApiController]
[Route("api/domains")]
[Authorize]
public class DomainsController : ControllerBase
{
    private readonly IMediator _mediator;
    public DomainsController(IMediator mediator) => _mediator = mediator;

    [HttpGet]
    public async Task<IActionResult> GetDomains()
        => Ok(await _mediator.Send(new GetDomainsQuery()));

    [HttpGet("{id:guid}")]
    public async Task<IActionResult> GetDomain(Guid id)
        => Ok(await _mediator.Send(new GetDomainByIdQuery(id)));

    [HttpPost]
    public async Task<IActionResult> CreateDomain([FromBody] CreateDomainRequest request)
    {
        var result = await _mediator.Send(new CreateDomainCommand(request));
        return CreatedAtAction(nameof(GetDomain), new { id = result.Id }, result);
    }

    [HttpPut("{id:guid}")]
    public async Task<IActionResult> UpdateDomain(Guid id, [FromBody] UpdateDomainRequest request)
        => Ok(await _mediator.Send(new UpdateDomainCommand(id, request)));

    [HttpDelete("{id:guid}")]
    public async Task<IActionResult> DeleteDomain(Guid id)
    {
        await _mediator.Send(new DeleteDomainCommand(id));
        return NoContent();
    }
}
