package com.pdp.web.error;

import com.pdp.common.exception.BadRequestException;
import com.pdp.common.exception.ForbiddenException;
import com.pdp.common.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Maps exceptions to RFC 7807 problem responses ({@code application/problem+json}). */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ProblemDetail> handleNotFound(NotFoundException ex) {
        return respond(Problems.of(HttpStatus.NOT_FOUND, ex.getMessage()));
    }

    @ExceptionHandler(ForbiddenException.class)
    ResponseEntity<ProblemDetail> handleForbidden(ForbiddenException ex) {
        return respond(Problems.of(HttpStatus.FORBIDDEN, ex.getMessage()));
    }

    @ExceptionHandler(BadRequestException.class)
    ResponseEntity<ProblemDetail> handleBadRequest(BadRequestException ex) {
        return respond(Problems.of(HttpStatus.BAD_REQUEST, ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> handleInvalidBody(MethodArgumentNotValidException ex) {
        Map<String, List<String>> errors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.computeIfAbsent(error.getField(), f -> new ArrayList<>()).add(error.getDefaultMessage());
        }
        ProblemDetail problem = Problems.of(HttpStatus.BAD_REQUEST, "Validation failed");
        problem.setProperty("errors", errors);
        return respond(problem);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ProblemDetail> handleUnreadableBody() {
        return respond(Problems.of(HttpStatus.BAD_REQUEST, "The request body is missing or is not valid JSON."));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ProblemDetail> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return respond(Problems.of(HttpStatus.BAD_REQUEST, "'%s' is not a valid value for %s.".formatted(ex.getValue(), ex.getName())));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ProblemDetail> handleUnsupportedMediaType() {
        return respond(Problems.of(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "The request body must be JSON."));
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    ResponseEntity<ProblemDetail> handleNotAcceptable() {
        return respond(Problems.of(HttpStatus.NOT_ACCEPTABLE, "The response can only be produced as JSON."));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ProblemDetail> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        return respond(Problems.of(HttpStatus.METHOD_NOT_ALLOWED, "Method %s is not supported here.".formatted(ex.getMethod())));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ProblemDetail> handleNoRoute() {
        return respond(Problems.of(HttpStatus.NOT_FOUND, "No such endpoint."));
    }

    /** The client disconnected; there is nobody to respond to. */
    @ExceptionHandler(AsyncRequestNotUsableException.class)
    void handleClientAbort(AsyncRequestNotUsableException ex) {
        log.debug("Client disconnected: {}", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return respond(Problems.of(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred."));
    }

    private static ResponseEntity<ProblemDetail> respond(ProblemDetail problem) {
        return ResponseEntity.status(problem.getStatus()).body(problem);
    }
}
