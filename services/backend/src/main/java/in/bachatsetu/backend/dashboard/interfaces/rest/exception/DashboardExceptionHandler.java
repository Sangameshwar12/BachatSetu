package in.bachatsetu.backend.dashboard.interfaces.rest.exception;

import in.bachatsetu.backend.auth.application.security.CurrentUserUnavailableException;
import in.bachatsetu.backend.dashboard.application.exception.NoActiveGroupException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Converts dashboard module REST boundary failures to RFC 7807 problem details. */
@RestControllerAdvice(basePackages = "in.bachatsetu.backend.dashboard.interfaces.rest")
public class DashboardExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DashboardExceptionHandler.class);
    private static final String TYPE_PREFIX = "urn:bachatsetu:problem:";

    @ExceptionHandler(NoActiveGroupException.class)
    ResponseEntity<ProblemDetail> handleNoActiveGroup(NoActiveGroupException exception, HttpServletRequest request) {
        return response(
                HttpStatus.NOT_FOUND, "no-active-group", "No active group", exception.getMessage(), request);
    }

    @ExceptionHandler(CurrentUserUnavailableException.class)
    ResponseEntity<ProblemDetail> handleUnauthenticated(HttpServletRequest request) {
        return response(
                HttpStatus.UNAUTHORIZED, "authentication-required", "Authentication required",
                "A valid bearer access token is required.", request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpected(Exception exception, HttpServletRequest request) {
        LOGGER.error("Unhandled dashboard API failure for {}", request.getRequestURI(), exception);
        return response(
                HttpStatus.INTERNAL_SERVER_ERROR, "internal-error", "Internal server error",
                "The request could not be completed.", request);
    }

    private ResponseEntity<ProblemDetail> response(
            HttpStatus status, String code, String title, String message, HttpServletRequest request) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, message);
        detail.setType(URI.create(TYPE_PREFIX + code));
        detail.setTitle(title);
        detail.setInstance(URI.create(request.getRequestURI()));
        detail.setProperty("code", code);
        detail.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(status).body(detail);
    }
}
