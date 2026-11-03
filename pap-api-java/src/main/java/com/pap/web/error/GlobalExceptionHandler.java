package com.pap.web.error;

import com.pap.common.exception.ConflictException;
import com.pap.common.exception.ForbiddenException;
import com.pap.common.exception.NotFoundException;
import com.pap.common.exception.ValidationException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Maps exceptions to RFC 7807 problem responses, using the same {@code type}, {@code title} and
 * {@code errors} shapes as the .NET implementation:
 * <ul>
 *   <li>domain exceptions: {@code {type: https://httpstatuses.com/<status>, title: <message>, status}}</li>
 *   <li>malformed or incomplete request bodies: ASP.NET-style model validation problems with a {@code traceId}</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String MODEL_VALIDATION_TYPE = "https://tools.ietf.org/html/rfc9110#section-15.5.1";
    private static final String MODEL_VALIDATION_TITLE = "One or more validation errors occurred.";
    /** Name of the body parameter in the .NET controllers; reported when the body cannot be bound. */
    private static final String BODY_PARAMETER = "request";

    @ExceptionHandler(ValidationException.class)
    ResponseEntity<ProblemDetail> handleValidation(ValidationException ex) {
        ProblemDetail problem = statusProblem(HttpStatus.BAD_REQUEST, "Validation failed");
        problem.setProperty("errors", ex.getErrors());
        return respond(problem);
    }

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ProblemDetail> handleNotFound(NotFoundException ex) {
        return respond(statusProblem(HttpStatus.NOT_FOUND, ex.getMessage()));
    }

    @ExceptionHandler(ForbiddenException.class)
    ResponseEntity<ProblemDetail> handleForbidden(ForbiddenException ex) {
        return respond(statusProblem(HttpStatus.FORBIDDEN, ex.getMessage()));
    }

    @ExceptionHandler(ConflictException.class)
    ResponseEntity<ProblemDetail> handleConflict(ConflictException ex) {
        return respond(statusProblem(HttpStatus.CONFLICT, ex.getMessage()));
    }

    /** Required body properties that were missing or null. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> handleInvalidBody(MethodArgumentNotValidException ex) {
        Map<String, List<String>> errors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            String property = pascalPath(error.getField());
            errors.computeIfAbsent(property, p -> new ArrayList<>())
                    .add("The %s field is required.".formatted(lastSegment(property)));
        }
        return respond(modelValidationProblem(errors));
    }

    /** Missing, malformed or wrongly typed request body. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ProblemDetail> handleUnreadableBody(HttpMessageNotReadableException ex, HttpServletRequest request) {
        // A body-less request without a Content-Type cannot be matched to a JSON endpoint at all
        if (request.getContentType() == null) return unsupportedMediaType();

        Map<String, List<String>> errors = new LinkedHashMap<>();
        String message = ex.getMessage() == null ? "" : ex.getMessage();
        if (message.startsWith("Required request body is missing")) {
            errors.put("", List.of("A non-empty request body is required."));
        } else {
            errors.put("$", List.of("The request body is not valid JSON or does not match the expected shape."));
        }
        errors.put(BODY_PARAMETER, List.of("The %s field is required.".formatted(BODY_PARAMETER)));
        return respond(modelValidationProblem(errors));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ProblemDetail> handleUnsupportedMediaType() {
        return unsupportedMediaType();
    }

    private static ResponseEntity<ProblemDetail> unsupportedMediaType() {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        problem.setType(URI.create("https://tools.ietf.org/html/rfc9110#section-15.5.16"));
        problem.setTitle("Unsupported Media Type");
        problem.setProperty("traceId", traceId());
        return respond(problem);
    }

    /** A route parameter that is not a GUID matches no route (404); a malformed query value is a bad request. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ProblemDetail> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        if (ex.getParameter().hasParameterAnnotation(PathVariable.class)) return ResponseEntity.notFound().build();
        String message = "The value '%s' is not valid for %s.".formatted(ex.getValue(), ex.getName());
        return respond(modelValidationProblem(Map.of(ex.getName(), List.of(message))));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<Void> handleNoRoute() {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<Void> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        Set<HttpMethod> supported = ex.getSupportedHttpMethods();
        ResponseEntity.BodyBuilder response = ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED);
        if (supported != null) response.allow(supported.toArray(HttpMethod[]::new));
        return response.build();
    }

    /** The client disconnected; there is nobody to respond to. */
    @ExceptionHandler(AsyncRequestNotUsableException.class)
    void handleClientAbort(AsyncRequestNotUsableException ex) {
        log.debug("Client disconnected: {}", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return respond(statusProblem(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred."));
    }

    private static ProblemDetail statusProblem(HttpStatus status, String title) {
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setType(URI.create("https://httpstatuses.com/" + status.value()));
        problem.setTitle(title);
        return problem;
    }

    private static ProblemDetail modelValidationProblem(Map<String, List<String>> errors) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setType(URI.create(MODEL_VALIDATION_TYPE));
        problem.setTitle(MODEL_VALIDATION_TITLE);
        problem.setProperty("errors", errors);
        problem.setProperty("traceId", traceId());
        return problem;
    }

    private static ResponseEntity<ProblemDetail> respond(ProblemDetail problem) {
        return ResponseEntity.status(problem.getStatus()).body(problem);
    }

    /** W3C trace-context style id, as ASP.NET Core reports in problem responses. */
    private static String traceId() {
        HexFormat hex = HexFormat.of();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        byte[] traceId = new byte[16];
        byte[] spanId = new byte[8];
        random.nextBytes(traceId);
        random.nextBytes(spanId);
        return "00-" + hex.formatHex(traceId) + "-" + hex.formatHex(spanId) + "-00";
    }

    /** "policyDefinition.rules[0].ruleId" -> "PolicyDefinition.Rules[0].RuleId". */
    private static String pascalPath(String path) {
        StringBuilder sb = new StringBuilder(path.length());
        boolean capitalize = true;
        for (char c : path.toCharArray()) {
            sb.append(capitalize ? Character.toUpperCase(c) : c);
            capitalize = c == '.';
        }
        return sb.toString();
    }

    private static String lastSegment(String path) {
        return path.substring(path.lastIndexOf('.') + 1);
    }
}
