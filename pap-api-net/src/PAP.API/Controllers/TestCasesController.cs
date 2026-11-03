using MediatR;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using PAP.Application.Features.TestCases;
using PAP.Application.Features.TestCases.DTOs;

namespace PAP.API.Controllers;

[ApiController]
[Route("api/test-cases")]
[Authorize]
public class TestCasesController : ControllerBase
{
    private readonly IMediator _mediator;
    public TestCasesController(IMediator mediator) => _mediator = mediator;

    [HttpGet]
    public async Task<IActionResult> GetTestCases([FromQuery] int page = 0, [FromQuery] int pageSize = 20)
        => Ok(await _mediator.Send(new GetTestCasesQuery(page, pageSize)));

    [HttpPost]
    public async Task<IActionResult> CreateTestCase([FromBody] CreateTestCaseRequest request)
        => Ok(await _mediator.Send(new CreateTestCaseCommand(request)));

    [HttpPut("{id:guid}")]
    public async Task<IActionResult> UpdateTestCase(Guid id, [FromBody] UpdateTestCaseRequest request)
        => Ok(await _mediator.Send(new UpdateTestCaseCommand(id, request)));

    [HttpDelete("{id:guid}")]
    public async Task<IActionResult> DeleteTestCase(Guid id)
    {
        await _mediator.Send(new DeleteTestCaseCommand(id));
        return NoContent();
    }

    [HttpPost("{id:guid}/run")]
    public async Task<IActionResult> RunTestCase(Guid id, [FromBody] RunTestCaseRequest? request)
        => Ok(await _mediator.Send(new RunTestCaseCommand(id, request?.InputData)));
}
