using AutoMapper;
using MediatR;
using PAP.Application.Common;
using PAP.Application.Common.Exceptions;
using PAP.Application.Features.Users.DTOs;
using PAP.Application.Interfaces;
using PAP.Domain.Entities;

namespace PAP.Application.Features.Users.Queries;

public record GetUsersQuery(int Page = 0, int PageSize = 10, string? Search = null)
    : IRequest<PagedResult<UserDto>>;

public class GetUsersQueryHandler : IRequestHandler<GetUsersQuery, PagedResult<UserDto>>
{
    private readonly IUserRepository _users;
    private readonly ICurrentUserService _currentUser;
    private readonly IMapper _mapper;

    public GetUsersQueryHandler(IUserRepository users, ICurrentUserService currentUser, IMapper mapper)
    { _users = users; _currentUser = currentUser; _mapper = mapper; }

    public async Task<PagedResult<UserDto>> Handle(GetUsersQuery request, CancellationToken ct)
    {
        if (!_currentUser.IsAuthenticated) throw new ForbiddenException();

        var all = await _users.GetAllAsync();
        if (!string.IsNullOrWhiteSpace(request.Search))
            all = all.Where(u => u.Username.Contains(request.Search, StringComparison.OrdinalIgnoreCase)).ToList();

        all = all.OrderBy(u => u.Username).ToList();
        var total = all.Count;
        var items = all.Skip(request.Page * request.PageSize).Take(request.PageSize).ToList();

        return new PagedResult<UserDto>
        {
            Items = _mapper.Map<List<UserDto>>(items),
            TotalCount = total, Page = request.Page, PageSize = request.PageSize
        };
    }
}

public record GetUserByIdQuery(Guid Id) : IRequest<UserDto>;

public class GetUserByIdQueryHandler : IRequestHandler<GetUserByIdQuery, UserDto>
{
    private readonly IUserRepository _users;
    private readonly ICurrentUserService _currentUser;
    private readonly IMapper _mapper;

    public GetUserByIdQueryHandler(IUserRepository users, ICurrentUserService currentUser, IMapper mapper)
    { _users = users; _currentUser = currentUser; _mapper = mapper; }

    public async Task<UserDto> Handle(GetUserByIdQuery request, CancellationToken ct)
    {
        if (!_currentUser.IsAuthenticated) throw new ForbiddenException();
        var user = await _users.GetByIdAsync(request.Id)
            ?? throw new NotFoundException(nameof(User), request.Id);
        return _mapper.Map<UserDto>(user);
    }
}
