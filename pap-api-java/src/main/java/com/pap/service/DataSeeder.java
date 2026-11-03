package com.pap.service;

import com.pap.domain.model.Domain;
import com.pap.domain.model.DomainRole;
import com.pap.domain.model.DomainStatus;
import com.pap.domain.model.Role;
import com.pap.domain.model.User;
import com.pap.domain.model.UserStatus;
import com.pap.domain.model.UserType;
import com.pap.repository.DomainRepository;
import com.pap.repository.RoleRepository;
import com.pap.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Seeds the system roles, the default domain and an {@code admin} account on first start-up.
 * Each step only runs while its collection is empty, so restarts never overwrite data.
 */
@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    static final UUID ADMIN_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final String ADMIN_ROLE = "System Admin";

    private static final Map<String, List<String>> SYSTEM_ROLES = systemRoles();

    private final RoleRepository roles;
    private final DomainRepository domains;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public DataSeeder(RoleRepository roles, DomainRepository domains, UserRepository users,
                      PasswordEncoder passwordEncoder, Clock clock) {
        this.roles = roles;
        this.domains = domains;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedRoles();
        seedDomains();
        seedAdminUser();
    }

    private void seedRoles() {
        if (!roles.findAll().isEmpty()) return;
        Instant now = clock.instant();
        SYSTEM_ROLES.forEach((name, permissions) -> {
            Role role = new Role();
            role.setId(UUID.randomUUID());
            role.setName(name);
            role.setDescription(name + " role");
            role.setSystemRole(true);
            role.setPermissions(permissions);
            role.setCreatedAt(now);
            role.setLastModifiedAt(now);
            roles.add(role);
        });
        log.info("Seeded {} system roles", SYSTEM_ROLES.size());
    }

    private void seedDomains() {
        if (!domains.findAll().isEmpty()) return;
        Instant now = clock.instant();
        Domain domain = new Domain();
        domain.setId(Domain.DEFAULT_DOMAIN_ID);
        domain.setName("Default Domain");
        domain.setDescription("Default administration domain");
        domain.setStatus(DomainStatus.ACTIVE);
        domain.setCreatedAt(now);
        domain.setLastModifiedAt(now);
        domains.add(domain);
        log.info("Seeded the default domain");
    }

    private void seedAdminUser() {
        if (!users.findAll().isEmpty()) return;
        Instant now = clock.instant();
        User admin = new User();
        admin.setId(ADMIN_USER_ID);
        admin.setUsername("admin");
        admin.setPasswordHash(passwordEncoder.encode("admin123"));
        admin.setUserType(UserType.LOCAL);
        admin.setStatus(UserStatus.ACTIVE);
        admin.setDefaultDomainId(Domain.DEFAULT_DOMAIN_ID);
        roles.findAll().stream()
                .filter(r -> ADMIN_ROLE.equals(r.getName()))
                .findFirst()
                .ifPresent(r -> admin.setDomainRoles(List.of(new DomainRole(Domain.DEFAULT_DOMAIN_ID, List.of(r.getId())))));
        admin.setCreatedAt(now);
        admin.setLastModifiedAt(now);
        users.add(admin);
        log.warn("Seeded the default 'admin' account with the default password; change it before production use");
    }

    private static Map<String, List<String>> systemRoles() {
        Map<String, List<String>> systemRoles = new LinkedHashMap<>();
        systemRoles.put(ADMIN_ROLE, List.of("manage_users", "manage_domains", "manage_roles", "manage_policies", "test_policies", "manage_scenarios"));
        systemRoles.put("Policy Author", List.of("manage_policies", "test_policies", "manage_scenarios"));
        systemRoles.put("Policy Viewer", List.of("test_policies", "manage_scenarios"));
        systemRoles.put("Domain Admin", List.of("manage_users", "manage_domains", "manage_policies", "test_policies", "manage_scenarios"));
        systemRoles.put("Auditor", List.of("test_policies", "manage_scenarios"));
        return systemRoles;
    }
}
