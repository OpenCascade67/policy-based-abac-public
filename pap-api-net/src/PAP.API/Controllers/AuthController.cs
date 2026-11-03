using MediatR;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using PAP.Application.Features.Auth.Commands;
using PAP.Application.Features.Auth.DTOs;

namespace PAP.API.Controllers;

[ApiController]
[Route("api/auth")]
public class AuthController : ControllerBase
{
    private readonly IMediator _mediator;
    public AuthController(IMediator mediator) => _mediator = mediator;

    [HttpPost("login")]
    [AllowAnonymous]
    public async Task<IActionResult> Login([FromBody] LoginRequest request)
        => Ok(await _mediator.Send(new LoginCommand(request.Username, request.Password)));

    [HttpPost("otp/send")]
    [AllowAnonymous]
    public async Task<IActionResult> SendOtp([FromBody] SendOtpRequest request)
    {
        var code = await _mediator.Send(new SendOtpCommand(request.Email));
        // In production, do NOT return the code — send via email only
        return Ok(new { message = "OTP sent", code });
    }

    [HttpPost("otp/verify")]
    [AllowAnonymous]
    public async Task<IActionResult> VerifyOtp([FromBody] VerifyOtpRequest request)
        => Ok(await _mediator.Send(new VerifyOtpCommand(request.Email, request.Code)));

    [HttpPost("refresh")]
    [AllowAnonymous]
    public async Task<IActionResult> RefreshToken([FromBody] RefreshTokenRequest request)
        => Ok(await _mediator.Send(new RefreshTokenCommand(request.RefreshToken)));

    [HttpPost("switch-domain")]
    [Authorize]
    public async Task<IActionResult> SwitchDomain([FromBody] SwitchDomainRequest request)
        => Ok(await _mediator.Send(new SwitchDomainCommand(request.DomainId)));

    [HttpPost("logout")]
    [Authorize]
    public async Task<IActionResult> Logout([FromBody] RefreshTokenRequest request)
    {
        await _mediator.Send(new LogoutCommand(request.RefreshToken));
        return NoContent();
    }
}
