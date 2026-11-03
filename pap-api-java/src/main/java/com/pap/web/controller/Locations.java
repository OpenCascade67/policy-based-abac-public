package com.pap.web.controller;

import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/** {@code Location} headers for 201 Created responses. */
final class Locations {

    private Locations() {
    }

    /** The URI of a newly created child of the current collection resource, e.g. {@code /api/domains/{id}}. */
    static URI ofCreated(String id) {
        return ServletUriComponentsBuilder.fromCurrentRequestUri().path("/{id}").buildAndExpand(id).toUri();
    }
}
