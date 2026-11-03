package com.pap.service;

import com.pap.common.exception.ConflictException;
import com.pap.common.exception.ForbiddenException;
import com.pap.common.exception.NotFoundException;
import com.pap.common.validation.Validator;
import com.pap.domain.model.Role;
import com.pap.repository.RoleRepository;
import com.pap.security.CurrentUser;
import com.pap.web.dto.role.RoleDto;
import com.pap.web.dto.role.RoleRequest;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/** Custom roles; the seeded system roles are read-only. */
@Service
public class RoleService {

    private static final String ROLE = "Role";

    private final RoleRepository roles;
    private final CurrentUser currentUser;
    private final Clock clock;

    public RoleService(RoleRepository roles, CurrentUser currentUser, Clock clock) {
        this.roles = roles;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    public List<RoleDto> list() {
        return roles.findAll().stream().map(RoleDto::from).toList();
    }

    public RoleDto create(RoleRequest request) {
        Validator.create()
                .notEmpty("Request.Name", request.name())
                .maxLength("Request.Name", request.name(), 100)
                .validate();
        if (roles.existsByName(request.name())) {
            throw new ConflictException("Role '%s' already exists.".formatted(request.name()));
        }

        Role role = new Role();
        role.setId(UUID.randomUUID());
        role.setName(request.name());
        role.setDescription(request.description());
        role.setSystemRole(false);
        role.markCreated(clock.instant(), currentUser.userIdOrNull());
        return RoleDto.from(roles.add(role));
    }

    public RoleDto update(UUID id, RoleRequest request) {
        Role role = find(id);
        if (role.isSystemRole()) throw new ForbiddenException("System roles cannot be modified.");
        role.setName(request.name());
        role.setDescription(request.description());
        role.markModified(clock.instant(), currentUser.userIdOrNull());
        return RoleDto.from(roles.update(role));
    }

    public void delete(UUID id) {
        Role role = find(id);
        if (role.isSystemRole()) throw new ForbiddenException("System roles cannot be deleted.");
        roles.deleteById(id);
    }

    private Role find(UUID id) {
        return roles.findById(id).orElseThrow(() -> new NotFoundException(ROLE, id));
    }
}
