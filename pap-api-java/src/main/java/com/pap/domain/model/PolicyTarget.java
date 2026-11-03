package com.pap.domain.model;

import java.util.List;

public record PolicyTarget(
        List<AttributeMatch> subjects,
        List<AttributeMatch> resources,
        List<AttributeMatch> actions,
        List<AttributeMatch> environments) {
}
