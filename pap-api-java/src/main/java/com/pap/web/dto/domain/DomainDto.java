package com.pap.web.dto.domain;

import com.pap.domain.model.Domain;
import com.pap.json.EnumNames;
import com.pap.web.dto.Ids;

import java.time.Instant;

public record DomainDto(
        String id,
        String name,
        String description,
        String status,
        String createdById,
        String lastModifiedById,
        Instant createdAt,
        Instant lastModifiedAt) {

    public static DomainDto from(Domain domain) {
        return new DomainDto(
                Ids.format(domain.getId()),
                domain.getName(),
                domain.getDescription(),
                EnumNames.lower(domain.getStatus()),
                Ids.format(domain.getCreatedById()),
                Ids.format(domain.getLastModifiedById()),
                domain.getCreatedAt(),
                domain.getLastModifiedAt());
    }
}
