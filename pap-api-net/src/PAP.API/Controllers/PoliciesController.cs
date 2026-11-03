using MediatR;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using PAP.Application.Features.Policies;
using PAP.Application.Features.Policies.DTOs;

namespace PAP.API.Controllers;

[ApiController]
[Route("api/policies")]
[Authorize]
public class PoliciesController : ControllerBase
{
    private readonly IMediator _mediator;
    public PoliciesController(IMediator mediator) => _mediator = mediator;

    [HttpGet]
    public async Task<IActionResult> GetPolicies([FromQuery] int page = 0, [FromQuery] int pageSize = 10, [FromQuery] string? search = null)
        => Ok(await _mediator.Send(new GetPoliciesQuery(page, pageSize, search)));

    [HttpGet("{id:guid}")]
    public async Task<IActionResult> GetPolicy(Guid id)
        => Ok(await _mediator.Send(new GetPolicyByIdQuery(id)));

    [HttpPost]
    public async Task<IActionResult> CreatePolicy([FromBody] CreatePolicyRequest request)
    {
        var result = await _mediator.Send(new CreatePolicyCommand(request));
        return CreatedAtAction(nameof(GetPolicy), new { id = result.Id }, result);
    }

    [HttpPut("{id:guid}")]
    public async Task<IActionResult> UpdatePolicy(Guid id, [FromBody] UpdatePolicyRequest request)
        => Ok(await _mediator.Send(new UpdatePolicyCommand(id, request)));

    [HttpDelete("{id:guid}")]
    public async Task<IActionResult> DeletePolicy(Guid id)
    {
        await _mediator.Send(new DeletePolicyCommand(id));
        return NoContent();
    }

    [HttpGet("{id:guid}/export/{format}")]
    public async Task<IActionResult> ExportPolicy(Guid id, string format)
    {
        var (content, mimeType, fileName) = await _mediator.Send(new ExportPolicyQuery(id, format));
        return File(System.Text.Encoding.UTF8.GetBytes(content), mimeType, fileName);
    }
}
