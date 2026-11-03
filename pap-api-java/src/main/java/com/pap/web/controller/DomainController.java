package com.pap.web.controller;

import com.pap.service.DomainService;
import com.pap.web.dto.domain.DomainDto;
import com.pap.web.dto.domain.DomainRequest;
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

@Tag(name = "Domains")
@RestController
@RequestMapping("/api/domains")
public class DomainController {

    private final DomainService domainService;

    public DomainController(DomainService domainService) {
        this.domainService = domainService;
    }

    @GetMapping
    public List<DomainDto> list() {
        return domainService.list();
    }

    @GetMapping("/{id}")
    public DomainDto get(@PathVariable UUID id) {
        return domainService.get(id);
    }

    @PostMapping
    public ResponseEntity<DomainDto> create(@RequestBody DomainRequest request) {
        DomainDto created = domainService.create(request);
        return ResponseEntity.created(Locations.ofCreated(created.id())).body(created);
    }

    @PutMapping("/{id}")
    public DomainDto update(@PathVariable UUID id, @RequestBody DomainRequest request) {
        return domainService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        domainService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
