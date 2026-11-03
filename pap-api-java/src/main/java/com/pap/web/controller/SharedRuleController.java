package com.pap.web.controller;

import com.pap.service.SharedRuleService;
import com.pap.web.dto.sharedrule.SharedRuleDto;
import com.pap.web.dto.sharedrule.SharedRuleRequest;
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

@Tag(name = "Shared Rules")
@RestController
@RequestMapping("/api/shared-rules")
public class SharedRuleController {

    private final SharedRuleService sharedRuleService;

    public SharedRuleController(SharedRuleService sharedRuleService) {
        this.sharedRuleService = sharedRuleService;
    }

    @GetMapping
    public List<SharedRuleDto> list() {
        return sharedRuleService.list();
    }

    @GetMapping("/{id}")
    public SharedRuleDto get(@PathVariable UUID id) {
        return sharedRuleService.get(id);
    }

    @PostMapping
    public ResponseEntity<SharedRuleDto> create(@RequestBody SharedRuleRequest request) {
        SharedRuleDto created = sharedRuleService.create(request);
        return ResponseEntity.created(Locations.ofCreated(created.id())).body(created);
    }

    @PutMapping("/{id}")
    public SharedRuleDto update(@PathVariable UUID id, @RequestBody SharedRuleRequest request) {
        return sharedRuleService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        sharedRuleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
