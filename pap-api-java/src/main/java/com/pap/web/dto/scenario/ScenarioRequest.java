package com.pap.web.dto.scenario;

/** Body for creating or updating a scenario; {@code value} is the request JSON as a string. */
public record ScenarioRequest(String name, String description, String value) {

    public ScenarioRequest {
        name = name == null ? "" : name;
        description = description == null ? "" : description;
        value = value == null ? "{}" : value;
    }
}
