package com.pap.service;

import com.pap.common.exception.ConflictException;
import com.pap.common.exception.NotFoundException;
import com.pap.common.validation.Validator;
import com.pap.domain.model.Domain;
import com.pap.domain.model.DomainStatus;
import com.pap.repository.DomainRepository;
import com.pap.security.CurrentUser;
import com.pap.web.dto.domain.DomainDto;
import com.pap.web.dto.domain.DomainRequest;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
public class DomainService {

    private static final String DOMAIN = "Domain";

    private final DomainRepository domains;
    private final CurrentUser currentUser;
    private final Clock clock;

    public DomainService(DomainRepository domains, CurrentUser currentUser, Clock clock) {
        this.domains = domains;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    public List<DomainDto> list() {
        return domains.findAll().stream().map(DomainDto::from).toList();
    }

    public DomainDto get(UUID id) {
        return DomainDto.from(find(id));
    }

    public DomainDto create(DomainRequest request) {
        Validator.create()
                .notEmpty("Request.Name", request.name())
                .maxLength("Request.Name", request.name(), 100)
                .must("Request.Status", "active".equals(request.status()) || "inactive".equals(request.status()))
                .validate();
        if (domains.existsByName(request.name())) {
            throw new ConflictException("Domain '%s' already exists.".formatted(request.name()));
        }

        Domain domain = new Domain();
        domain.setId(UUID.randomUUID());
        apply(domain, request);
        domain.markCreated(clock.instant(), currentUser.userIdOrNull());
        return DomainDto.from(domains.add(domain));
    }

    public DomainDto update(UUID id, DomainRequest request) {
        Domain domain = find(id);
        apply(domain, request);
        domain.markModified(clock.instant(), currentUser.userIdOrNull());
        return DomainDto.from(domains.update(domain));
    }

    public void delete(UUID id) {
        if (!domains.existsById(id)) throw new NotFoundException(DOMAIN, id);
        domains.deleteById(id);
    }

    private Domain find(UUID id) {
        return domains.findById(id).orElseThrow(() -> new NotFoundException(DOMAIN, id));
    }

    private static void apply(Domain domain, DomainRequest request) {
        domain.setName(request.name());
        domain.setDescription(request.description());
        domain.setStatus("active".equals(request.status()) ? DomainStatus.ACTIVE : DomainStatus.INACTIVE);
    }
}
