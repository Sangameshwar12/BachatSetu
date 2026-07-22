package in.bachatsetu.backend.invitation.interfaces.rest.exception;

import in.bachatsetu.backend.auth.application.security.CurrentUserUnavailableException;
import in.bachatsetu.backend.group.application.exception.GroupAccessDeniedException;
import in.bachatsetu.backend.invitation.application.exception.InvitationApplicationException;
import in.bachatsetu.backend.shared.domain.DomainException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Converts invitation module REST boundary failures to RFC 7807 problem details. */
@RestControllerAdvice(basePackages = "in.bachatsetu.backend.invitation.interfaces.rest")
public class InvitationExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(InvitationExceptionHandler.class);
    private static final String TYPE_PREFIX = "urn:bachatsetu:problem:";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> handleValidation(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        List<Violation> violations = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new Violation(error.getField(), error.getDefaultMessage()))
                .sorted(Comparator.comparing(Violation::field).thenComparing(Violation::message))
                .toList();
        ProblemDetail detail = problem(
                HttpStatus.BAD_REQUEST, "validation-error", "Request validation failed",
                "One or more request fields are invalid.", request);
        detail.setProperty("violations", violations);
        return ResponseEntity.badRequest().body(detail);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ProblemDetail> handleUnreadableBody(HttpServletRequest request) {
        return response(
                HttpStatus.BAD_REQUEST, "malformed-request", "Malformed request",
                "The request body is missing or cannot be parsed.", request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ProblemDetail> handleParameterValidation(
            ConstraintViolationException exception, HttpServletRequest request) {
        List<Violation> violations = exception.getConstraintViolations().stream()
                .map(violation -> new Violation(violation.getPropertyPath().toString(), violation.getMessage()))
                .sorted(Comparator.comparing(Violation::field).thenComparing(Violation::message))
                .toList();
        ProblemDetail detail = problem(
                HttpStatus.BAD_REQUEST, "validation-error", "Request validation failed",
                "One or more request parameters are invalid.", request);
        detail.setProperty("violations", violations);
        return ResponseEntity.badRequest().body(detail);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ProblemDetail> handleIllegalArgument(
            IllegalArgumentException exception, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "invalid-request", "Invalid request", exception.getMessage(), request);
    }

    @ExceptionHandler(InvitationApplicationException.class)
    ResponseEntity<ProblemDetail> handleInvitationFailure(
            InvitationApplicationException exception, HttpServletRequest request) {
        return switch (exception.reason()) {
            case GROUP_NOT_FOUND -> response(
                    HttpStatus.NOT_FOUND, "group-not-found", "Group not found", exception.getMessage(), request);
            case NO_ACTIVE_INVITATION -> response(
                    HttpStatus.NOT_FOUND, "no-active-invitation", "No active invitation", exception.getMessage(),
                    request);
            case INVITATION_NOT_FOUND -> response(
                    HttpStatus.NOT_FOUND, "invitation-not-found", "Invitation not found", exception.getMessage(),
                    request);
        };
    }

    @ExceptionHandler(GroupAccessDeniedException.class)
    ResponseEntity<ProblemDetail> handleAccessDenied(GroupAccessDeniedException exception, HttpServletRequest request) {
        return response(HttpStatus.FORBIDDEN, "access-denied", "Access denied", exception.getMessage(), request);
    }

    @ExceptionHandler(DomainException.class)
    ResponseEntity<ProblemDetail> handleDomainFailure(DomainException exception, HttpServletRequest request) {
        return response(
                HttpStatus.UNPROCESSABLE_ENTITY, "invitation-validation-failed", "Business validation failed",
                exception.getMessage(), request);
    }

    @ExceptionHandler(CurrentUserUnavailableException.class)
    ResponseEntity<ProblemDetail> handleUnauthenticated(HttpServletRequest request) {
        return response(
                HttpStatus.UNAUTHORIZED, "authentication-required", "Authentication required",
                "A valid bearer access token is required.", request);
    }

    /**
     * Every JPA entity in this codebase carries an optimistic-lock version (see
     * {@code BaseJpaEntity}), so two near-simultaneous joins against the same group aggregate
     * cannot both commit — one always loses this way, rather than silently double-joining. Mapped
     * to 409 (a retryable conflict) instead of falling through to the generic 500 handler below.
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ResponseEntity<ProblemDetail> handleConcurrentModification(HttpServletRequest request) {
        return response(
                HttpStatus.CONFLICT, "concurrent-modification", "Concurrent modification",
                "This group was modified concurrently by another request; please retry.", request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpected(Exception exception, HttpServletRequest request) {
        LOGGER.error("Unhandled invitation API failure for {}", request.getRequestURI(), exception);
        return response(
                HttpStatus.INTERNAL_SERVER_ERROR, "internal-error", "Internal server error",
                "The request could not be completed.", request);
    }

    private ResponseEntity<ProblemDetail> response(
            HttpStatus status, String code, String title, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(problem(status, code, title, message, request));
    }

    private ProblemDetail problem(
            HttpStatus status, String code, String title, String message, HttpServletRequest request) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, message);
        detail.setType(URI.create(TYPE_PREFIX + code));
        detail.setTitle(title);
        detail.setInstance(URI.create(request.getRequestURI()));
        detail.setProperty("code", code);
        detail.setProperty("timestamp", Instant.now());
        return detail;
    }

    record Violation(String field, String message) {
    }
}
