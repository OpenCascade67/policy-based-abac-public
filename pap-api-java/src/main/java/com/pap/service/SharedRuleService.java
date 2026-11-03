package com.pap.service;

import com.pap.common.exception.NotFoundException;
import com.pap.common.validation.Validator;
import com.pap.domain.model.RuleEffect;
import com.pap.domain.model.SharedRule;
import com.pap.repository.SharedRuleRepository;
import com.pap.security.CurrentUser;
import com.pap.web.dto.sharedrule.SharedRuleDto;
import com.pap.web.dto.sharedrule.SharedRuleRequest;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
public class SharedRuleService {

    private static final String SHARED_RULE = "SharedRule";

    private final SharedRuleRepository rules;
    private final CurrentUser currentUser;
    private final Clock clock;

    public SharedRuleService(SharedRuleRepository rules, CurrentUser currentUser, Clock clock) {
        this.rules = rules;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    public List<SharedRuleDto> list() {
        return rules.findAll().stream().map(SharedRuleDto::from).toList();
    }

    public SharedRuleDto get(UUID id) {
        return SharedRuleDto.from(find(id));
    }

    public SharedRuleDto create(SharedRuleRequest request) {
        Validator.create()
                .notEmpty("Request.Name", request.name())
                .maxLength("Request.Name", request.name(), 200)
                .notEmpty("Request.Category", request.category())
                .must("Request.Effect", "Permit".equals(request.effect()) || "Deny".equals(request.effect()))
                .validate();

        SharedRule rule = new SharedRule();
        rule.setId(UUID.randomUUID());
        apply(rule, request);
        rule.markCreated(clock.instant(), currentUser.userIdOrNull());
        return SharedRuleDto.from(rules.add(rule));
    }

    public SharedRuleDto update(UUID id, SharedRuleRequest request) {
        SharedRule rule = find(id);
        apply(rule, request);
        rule.markModified(clock.instant(), currentUser.userIdOrNull());
        return SharedRuleDto.from(rules.update(rule));
    }

    public void delete(UUID id) {
        if (!rules.existsById(id)) throw new NotFoundException(SHARED_RULE, id);
        rules.deleteById(id);
    }

    private SharedRule find(UUID id) {
        return rules.findById(id).orElseThrow(() -> new NotFoundException(SHARED_RULE, id));
    }

    private static void apply(SharedRule rule, SharedRuleRequest request) {
        rule.setName(request.name());
        rule.setDescription(request.description());
        rule.setCategory(request.category());
        rule.setEffect("Deny".equals(request.effect()) ? RuleEffect.DENY : RuleEffect.PERMIT);
        rule.setTarget(request.target());
        rule.setCondition(request.condition());
    }
}
