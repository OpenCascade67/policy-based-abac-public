using AutoMapper;
using PAP.Application.Features.Users.DTOs;
using PAP.Domain.Entities;
using PAP.Domain.ValueObjects;

namespace PAP.Application.Features.Users.Mappings;

public class UserMappingProfile : AutoMapper.Profile
{
    public UserMappingProfile()
    {
        CreateMap<User, UserDto>()
            .ForMember(d => d.Id, o => o.MapFrom(s => s.Id.ToString()))
            .ForMember(d => d.UserType, o => o.MapFrom(s => s.UserType.ToString().ToLower()))
            .ForMember(d => d.Status, o => o.MapFrom(s => s.Status.ToString().ToLower()))
            .ForMember(d => d.DefaultDomainId, o => o.MapFrom(s => s.DefaultDomainId.HasValue ? s.DefaultDomainId.Value.ToString() : null))
            .ForMember(d => d.CreatedById, o => o.MapFrom(s => s.CreatedById.HasValue ? s.CreatedById.Value.ToString() : null))
            .ForMember(d => d.LastModifiedById, o => o.MapFrom(s => s.LastModifiedById.HasValue ? s.LastModifiedById.Value.ToString() : null));

        CreateMap<DomainRole, DomainRoleItemDto>()
            .ForMember(d => d.DomainId, o => o.MapFrom(s => s.DomainId.ToString()))
            .ForMember(d => d.Roles, o => o.MapFrom(s => s.Roles.Select(r => r.ToString()).ToList()));
    }
}
