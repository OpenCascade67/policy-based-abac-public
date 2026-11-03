package com.pap.web.controller;

import com.pap.service.PolicyService;
import com.pap.service.export.ExportedFile;
import com.pap.web.ContentDispositions;
import com.pap.web.dto.PagedResult;
import com.pap.web.dto.policy.PolicyDto;
import com.pap.web.dto.policy.PolicyRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
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

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Tag(name = "Policies")
@RestController
@RequestMapping("/api/policies")
public class PolicyController {

    private final PolicyService policyService;

    public PolicyController(PolicyService policyService) {
        this.policyService = policyService;
    }

    @GetMapping
    public PagedResult<PolicyDto> list(@RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "10") int pageSize,
                                       @RequestParam(required = false) String search) {
        return policyService.list(page, pageSize, search);
    }

    @GetMapping("/{id}")
    public PolicyDto get(@PathVariable UUID id) {
        return policyService.get(id);
    }

    @PostMapping
    public ResponseEntity<PolicyDto> create(@RequestBody PolicyRequest request) {
        PolicyDto created = policyService.create(request);
        return ResponseEntity.created(Locations.ofCreated(created.id())).body(created);
    }

    @PutMapping("/{id}")
    public PolicyDto update(@PathVariable UUID id, @RequestBody PolicyRequest request) {
        return policyService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        policyService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Download the policy as XACML", description = "format is 'xml' or 'json'")
    @GetMapping("/{id}/export/{format}")
    public ResponseEntity<byte[]> export(@PathVariable UUID id, @PathVariable String format) {
        ExportedFile file = policyService.export(id, format);
        return ResponseEntity.ok()
                .contentType(file.mediaType())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDispositions.attachment(file.fileName()))
                .body(file.content().getBytes(StandardCharsets.UTF_8));
    }
}
