package com.pap.web.controller;

import com.pap.service.UserService;
import com.pap.web.dto.PagedResult;
import com.pap.web.dto.user.CreateUserRequest;
import com.pap.web.dto.user.SetDefaultDomainRequest;
import com.pap.web.dto.user.UpdateUserRequest;
import com.pap.web.dto.user.UserDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Users")
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public PagedResult<UserDto> list(@RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "10") int pageSize,
                                     @RequestParam(required = false) String search) {
        return userService.list(page, pageSize, search);
    }

    @GetMapping("/{id}")
    public UserDto get(@PathVariable UUID id) {
        return userService.get(id);
    }

    @PostMapping
    public ResponseEntity<UserDto> create(@RequestBody CreateUserRequest request) {
        UserDto created = userService.create(request);
        return ResponseEntity.created(Locations.ofCreated(created.id())).body(created);
    }

    @PutMapping("/{id}")
    public UserDto update(@PathVariable UUID id, @RequestBody UpdateUserRequest request) {
        return userService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/default-domain")
    public ResponseEntity<Void> setDefaultDomain(@PathVariable UUID id, @RequestBody SetDefaultDomainRequest request) {
        userService.setDefaultDomain(id, request.domainId());
        return ResponseEntity.noContent().build();
    }
}
