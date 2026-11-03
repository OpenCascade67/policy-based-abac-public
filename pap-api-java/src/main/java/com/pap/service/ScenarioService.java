package com.pap.service;

import com.pap.common.exception.NotFoundException;
import com.pap.common.validation.Validator;
import com.pap.domain.model.Scenario;
import com.pap.repository.ScenarioRepository;
import com.pap.security.CurrentUser;
import com.pap.web.dto.scenario.ScenarioDto;
import com.pap.web.dto.scenario.ScenarioRequest;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
public class ScenarioService {

    /** Entity name reported in not-found errors (the .NET entity class name). */
    private static final String SCENARIO = "ABACScenario";

    private final ScenarioRepository scenarios;
    private final CurrentUser currentUser;
    private final Clock clock;

    public ScenarioService(ScenarioRepository scenarios, CurrentUser currentUser, Clock clock) {
        this.scenarios = scenarios;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    public List<ScenarioDto> list() {
        return scenarios.findAll().stream().map(ScenarioDto::from).toList();
    }

    public ScenarioDto create(ScenarioRequest request) {
        Validator.create()
                .notEmpty("Request.Name", request.name())
                .maxLength("Request.Name", request.name(), 200)
                .validate();

        Scenario scenario = new Scenario();
        scenario.setId(UUID.randomUUID());
        apply(scenario, request);
        scenario.markCreated(clock.instant(), currentUser.userIdOrNull());
        return ScenarioDto.from(scenarios.add(scenario));
    }

    public ScenarioDto update(UUID id, ScenarioRequest request) {
        Scenario scenario = scenarios.findById(id).orElseThrow(() -> new NotFoundException(SCENARIO, id));
        apply(scenario, request);
        scenario.markModified(clock.instant(), currentUser.userIdOrNull());
        return ScenarioDto.from(scenarios.update(scenario));
    }

    public void delete(UUID id) {
        if (!scenarios.existsById(id)) throw new NotFoundException(SCENARIO, id);
        scenarios.deleteById(id);
    }

    private static void apply(Scenario scenario, ScenarioRequest request) {
        scenario.setName(request.name());
        scenario.setDescription(request.description());
        scenario.setValue(request.value());
    }
}
