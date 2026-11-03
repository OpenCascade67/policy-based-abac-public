package com.pdp.web.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;

/** RFC 7807 problem bodies in the same shape the PAP uses: {@code {type, title, status}}. */
public final class Problems {

    private Problems() {
    }

    public static ProblemDetail of(HttpStatus status, String title) {
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setType(URI.create("https://httpstatuses.com/" + status.value()));
        problem.setTitle(title);
        return problem;
    }
}
