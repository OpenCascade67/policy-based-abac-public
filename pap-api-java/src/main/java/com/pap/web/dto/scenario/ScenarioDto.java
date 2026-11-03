package com.pap.web.dto.scenario;

import com.pap.domain.model.Scenario;
import com.pap.web.dto.Ids;

import java.time.Instant;
import java.util.List;

public record ScenarioDto(
        String id,
        String name,
        String description,
        String value,
        List<String> testCases,
        String createdById,
        String lastModifiedById,
        Instant createdAt,
        Instant lastModifiedAt) {

    public static ScenarioDto from(Scenario scenario) {
        return new ScenarioDto(
                Ids.format(scenario.getId()),
                scenario.getName(),
                scenario.getDescription(),
                scenario.getValue(),
                scenario.getTestCases().stream().map(Ids::format).toList(),
                Ids.format(scenario.getCreatedById()),
                Ids.format(scenario.getLastModifiedById()),
                scenario.getCreatedAt(),
                scenario.getLastModifiedAt());
    }
}
