package com.pap.domain.model;

import java.util.List;

public record Obligation(
        String obligationId,
        ObligationFulfillOn fulfillOn,
        String description,
        List<ObligationAttributeAssignment> attributeAssignments) {

    public Obligation {
        obligationId = obligationId == null ? "" : obligationId;
        fulfillOn = fulfillOn == null ? ObligationFulfillOn.PERMIT : fulfillOn;
        attributeAssignments = attributeAssignments == null ? List.of() : attributeAssignments;
    }
}
