package com.pap.web.controller;

import com.pap.service.ScenarioService;
import com.pap.web.dto.scenario.ScenarioDto;
import com.pap.web.dto.scenario.ScenarioRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Scenarios")
@RestController
@RequestMapping("/api/scenarios")
public class ScenarioController {

    private final ScenarioService scenarioService;

    public ScenarioController(ScenarioService scenarioService) {
        this.scenarioService = scenarioService;
    }

    @GetMapping
    public List<ScenarioDto> list() {
        return scenarioService.list();
    }

    /** Responds 200 (not 201), as the UI expects. */
    @PostMapping
    public ScenarioDto create(@RequestBody ScenarioRequest request) {
        return scenarioService.create(request);
    }

    @PutMapping("/{id}")
    public ScenarioDto update(@PathVariable UUID id, @RequestBody ScenarioRequest request) {
        return scenarioService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        scenarioService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
