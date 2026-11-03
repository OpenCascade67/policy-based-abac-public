package com.pap.web.dto.user;

import com.pap.domain.model.User;
import com.pap.json.EnumNames;
import com.pap.web.dto.DomainRoleDto;
import com.pap.web.dto.Ids;

import java.time.Instant;
import java.util.List;

public record UserDto(
        String id,
        String username,
        String email,
        String userType,
        String status,
        List<DomainRoleDto> domainRoles,
        String defaultDomainId,
        String createdById,
        String lastModifiedById,
        Instant createdAt,
        Instant lastModifiedAt) {

    public static UserDto from(User user) {
        return new UserDto(
                Ids.format(user.getId()),
                user.getUsername(),
                user.getEmail(),
                EnumNames.lower(user.getUserType()),
                EnumNames.lower(user.getStatus()),
                user.getDomainRoles().stream().map(DomainRoleDto::from).toList(),
                Ids.format(user.getDefaultDomainId()),
                Ids.format(user.getCreatedById()),
                Ids.format(user.getLastModifiedById()),
                user.getCreatedAt(),
                user.getLastModifiedAt());
    }
}
