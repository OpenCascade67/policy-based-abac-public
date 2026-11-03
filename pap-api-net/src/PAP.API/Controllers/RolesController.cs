using MediatR;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using PAP.Application.Features.Roles;
using PAP.Application.Features.Roles.DTOs;

namespace PAP.API.Controllers;

[ApiController]
[Route("api/roles")]
[Authorize]
public class RolesController : ControllerBase
{
    private readonly IMediator _mediator;
    public RolesController(IMediator mediator) => _mediator = mediator;

    [HttpGet]
    public async Task<IActionResult> GetRoles()
        => Ok(await _mediator.Send(new GetRolesQuery()));

    [HttpPost]
    public async Task<IActionResult> CreateRole([FromBody] CreateRoleRequest request)
        => Ok(await _mediator.Send(new CreateRoleCommand(request)));

    [HttpPut("{id:guid}")]
    public async Task<IActionResult> UpdateRole(Guid id, [FromBody] UpdateRoleRequest request)
        => Ok(await _mediator.Send(new UpdateRoleCommand(id, request)));

    [HttpDelete("{id:guid}")]
    public async Task<IActionResult> DeleteRole(Guid id)
    {
        await _mediator.Send(new DeleteRoleCommand(id));
        return NoContent();
    }
}
