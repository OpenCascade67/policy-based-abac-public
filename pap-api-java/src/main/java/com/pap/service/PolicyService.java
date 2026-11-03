package com.pap.service;

import com.pap.common.exception.NotFoundException;
import com.pap.common.validation.Validator;
import com.pap.domain.model.Policy;
import com.pap.repository.PolicyRepository;
import com.pap.security.CurrentUser;
import com.pap.service.export.ExportedFile;
import com.pap.service.export.XacmlExporter;
import com.pap.web.dto.PagedResult;
import com.pap.web.dto.policy.PolicyDto;
import com.pap.web.dto.policy.PolicyRequest;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.UUID;

@Service
public class PolicyService {

    private static final String POLICY = "Policy";

    private final PolicyRepository policies;
    private final XacmlExporter exporter;
    private final CurrentUser currentUser;
    private final Clock clock;

    public PolicyService(PolicyRepository policies, XacmlExporter exporter, CurrentUser currentUser, Clock clock) {
        this.policies = policies;
        this.exporter = exporter;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    public PagedResult<PolicyDto> list(int page, int pageSize, String search) {
        return PagedResult.of(policies.search(search), page, pageSize, PolicyDto::from);
    }

    public PolicyDto get(UUID id) {
        return PolicyDto.from(find(id));
    }

    public PolicyDto create(PolicyRequest request) {
        validate(request);
        UUID id = UUID.randomUUID();
        Policy policy = new Policy();
        policy.setId(id);
        policy.setName(request.name());
        policy.setDescription(request.description());
        policy.setPolicyDefinition(request.policyDefinition().toDefinition(id.toString()));
        policy.markCreated(clock.instant(), currentUser.userIdOrNull());
        return PolicyDto.from(policies.add(policy));
    }

    public PolicyDto update(UUID id, PolicyRequest request) {
        validate(request);
        Policy policy = find(id);
        policy.setName(request.name());
        policy.setDescription(request.description());
        policy.setPolicyDefinition(request.policyDefinition().toDefinition(policy.getId().toString()));
        policy.markModified(clock.instant(), currentUser.userIdOrNull());
        return PolicyDto.from(policies.update(policy));
    }

    public void delete(UUID id) {
        if (!policies.existsById(id)) throw new NotFoundException(POLICY, id);
        policies.deleteById(id);
    }

    public ExportedFile export(UUID id, String format) {
        return exporter.export(find(id), format);
    }

    private Policy find(UUID id) {
        return policies.findById(id).orElseThrow(() -> new NotFoundException(POLICY, id));
    }

    private static void validate(PolicyRequest request) {
        Validator.create()
                .notEmpty("Request.Name", request.name())
                .maxLength("Request.Name", request.name(), 200)
                .validate();
    }
}
