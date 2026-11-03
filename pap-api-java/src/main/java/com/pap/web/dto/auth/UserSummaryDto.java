package com.pap.web.dto.auth;

import com.pap.domain.model.User;
import com.pap.json.EnumNames;
import com.pap.web.dto.DomainRoleDto;
import com.pap.web.dto.Ids;

import java.util.List;

public record UserSummaryDto(
        String id,
        String username,
        String email,
        String userType,
        String defaultDomainId,
        List<DomainRoleDto> domainRoles) {

    public static UserSummaryDto from(User user) {
        return new UserSummaryDto(
                Ids.format(user.getId()),
                user.getUsername(),
                user.getEmail(),
                EnumNames.lower(user.getUserType()),
                Ids.format(user.getDefaultDomainId()),
                user.getDomainRoles().stream().map(DomainRoleDto::from).toList());
    }
}
