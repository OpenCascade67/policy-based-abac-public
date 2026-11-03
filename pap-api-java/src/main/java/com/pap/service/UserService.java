package com.pap.service;

import com.pap.common.exception.ConflictException;
import com.pap.common.exception.NotFoundException;
import com.pap.common.validation.Validator;
import com.pap.domain.model.DomainRole;
import com.pap.domain.model.User;
import com.pap.domain.model.UserStatus;
import com.pap.domain.model.UserType;
import com.pap.repository.NameOrder;
import com.pap.repository.UserRepository;
import com.pap.security.CurrentUser;
import com.pap.web.dto.DomainRoleDto;
import com.pap.web.dto.Ids;
import com.pap.web.dto.PagedResult;
import com.pap.web.dto.user.CreateUserRequest;
import com.pap.web.dto.user.UpdateUserRequest;
import com.pap.web.dto.user.UserDto;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class UserService {

    private static final String USER = "User";

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUser currentUser;
    private final Clock clock;

    public UserService(UserRepository users, PasswordEncoder passwordEncoder, CurrentUser currentUser, Clock clock) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    /** Users whose username contains {@code search} (case-insensitive), ordered by username. */
    public PagedResult<UserDto> list(int page, int pageSize, String search) {
        String term = search == null || search.isBlank() ? null : search.toLowerCase(Locale.ROOT);
        List<User> matching = users.findAll().stream()
                .filter(u -> term == null || u.getUsername().toLowerCase(Locale.ROOT).contains(term))
                .sorted(Comparator.comparing(User::getUsername, NameOrder.INSTANCE))
                .toList();
        return PagedResult.of(matching, page, pageSize, UserDto::from);
    }

    public UserDto get(UUID id) {
        return UserDto.from(find(id));
    }

    public UserDto create(CreateUserRequest request) {
        Validator.create()
                .notEmpty("Request.Username", request.username())
                .maxLength("Request.Username", request.username(), 50)
                .notEmpty("Request.Password", request.password())
                .minLength("Request.Password", request.password(), 6)
                .must("Request.Status", isValidStatus(request.status()))
                .validate();
        if (users.existsByUsername(request.username())) {
            throw new ConflictException("Username '%s' already exists.".formatted(request.username()));
        }

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setUserType(UserType.LOCAL);
        user.setStatus(toStatus(request.status()));
        user.setDomainRoles(toDomainRoles(request.domainRoles()));
        user.markCreated(clock.instant(), currentUser.userIdOrNull());
        return UserDto.from(users.add(user));
    }

    public UserDto update(UUID id, UpdateUserRequest request) {
        Validator validator = Validator.create()
                .notEmpty("Id", id)
                .must("Request.Status", isValidStatus(request.status()));
        if (request.password() != null && !request.password().isEmpty()) {
            validator.minLength("Request.Password", request.password(), 6);
        }
        validator.validate();

        User user = find(id);
        user.setStatus(toStatus(request.status()));
        user.setDomainRoles(toDomainRoles(request.domainRoles()));
        if (user.getUserType() == UserType.LOCAL && request.password() != null && !request.password().isEmpty()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        user.markModified(clock.instant(), currentUser.userIdOrNull());
        return UserDto.from(users.update(user));
    }

    public void delete(UUID id) {
        if (!users.existsById(id)) throw new NotFoundException(USER, id);
        users.deleteById(id);
    }

    public void setDefaultDomain(UUID id, String domainId) {
        UUID defaultDomain = Ids.parse(domainId);
        User user = find(id);
        user.setDefaultDomainId(defaultDomain);
        user.markModified(clock.instant(), currentUser.userIdOrNull());
        users.update(user);
    }

    private User find(UUID id) {
        return users.findById(id).orElseThrow(() -> new NotFoundException(USER, id));
    }

    private static boolean isValidStatus(String status) {
        return "active".equals(status) || "inactive".equals(status);
    }

    private static UserStatus toStatus(String status) {
        return "active".equals(status) ? UserStatus.ACTIVE : UserStatus.INACTIVE;
    }

    private static List<DomainRole> toDomainRoles(List<DomainRoleDto> roles) {
        return roles.stream().map(DomainRoleDto::toDomainRole).toList();
    }
}
