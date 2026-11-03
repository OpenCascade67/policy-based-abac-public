package com.pap.web.dto.user;

public record SetDefaultDomainRequest(String domainId) {

    public SetDefaultDomainRequest {
        domainId = domainId == null ? "" : domainId;
    }
}
