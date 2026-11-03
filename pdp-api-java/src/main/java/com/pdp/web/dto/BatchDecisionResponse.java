package com.pdp.web.dto;

import java.util.List;

/** One result per request, in request order. */
public record BatchDecisionResponse(List<DecisionResponse> results) {
}
