using MediatR;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using PAP.Application.Features.Users.Commands;
using PAP.Application.Features.Users.DTOs;
using PAP.Application.Features.Users.Queries;

namespace PAP.API.Controllers;

[ApiController]
[Route("api/users")]
[Authorize]
public class UsersController : ControllerBase
{
    private readonly IMediator _mediator;
    public UsersController(IMediator mediator) => _mediator = mediator;

    [HttpGet]
    public async Task<IActionResult> GetUsers([FromQuery] int page = 0, [FromQuery] int pageSize = 10, [FromQuery] string? search = null)
        => Ok(await _mediator.Send(new GetUsersQuery(page, pageSize, search)));

    [HttpGet("{id:guid}")]
    public async Task<IActionResult> GetUser(Guid id)
        => Ok(await _mediator.Send(new GetUserByIdQuery(id)));

    [HttpPost]
    public async Task<IActionResult> CreateUser([FromBody] CreateUserRequest request)
    {
        var result = await _mediator.Send(new CreateUserCommand(request));
        return CreatedAtAction(nameof(GetUser), new { id = result.Id }, result);
    }

    [HttpPut("{id:guid}")]
    public async Task<IActionResult> UpdateUser(Guid id, [FromBody] UpdateUserRequest request)
        => Ok(await _mediator.Send(new UpdateUserCommand(id, request)));

    [HttpDelete("{id:guid}")]
    public async Task<IActionResult> DeleteUser(Guid id)
    {
        await _mediator.Send(new DeleteUserCommand(id));
        return NoContent();
    }

    [HttpPut("{id:guid}/default-domain")]
    public async Task<IActionResult> SetDefaultDomain(Guid id, [FromBody] SetDefaultDomainRequest request)
    {
        await _mediator.Send(new SetDefaultDomainCommand(id, Guid.Parse(request.DomainId)));
        return NoContent();
    }
}
