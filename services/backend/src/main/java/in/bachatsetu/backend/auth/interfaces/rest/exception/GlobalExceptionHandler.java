package in.bachatsetu.backend.auth.interfaces.rest.exception;

import in.bachatsetu.backend.auth.application.exception.OtpApplicationException;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Converts authentication boundary failures to RFC 7807 problem details. */
@RestControllerAdvice(basePackages = "in.bachatsetu.backend.auth.interfaces.rest")
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String TYPE_PREFIX = "urn:bachatsetu:problem:";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        List<Violation> violations = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new Violation(error.getField(), error.getDefaultMessage()))
                .sorted(Comparator.comparing(Violation::field).thenComparing(Violation::message))
                .toList();
        ProblemDetail detail = problem(
                HttpStatus.BAD_REQUEST,
                "validation-error",
                "Request validation failed",
                "One or more request fields are invalid.",
                request);
        detail.setProperty("violations", violations);
        return ResponseEntity.badRequest().body(detail);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ProblemDetail> handleUnreadableBody(HttpServletRequest request) {
        return response(
                HttpStatus.BAD_REQUEST,
                "malformed-request",
                "Malformed request",
                "The request body is missing or cannot be parsed.",
                request);
    }

    @ExceptionHandler(OtpApplicationException.class)
    ResponseEntity<ProblemDetail> handleApplicationFailure(
            OtpApplicationException exception,
            HttpServletRequest request) {
        return switch (exception.reason()) {
            case USER_NOT_FOUND -> response(
                    HttpStatus.NOT_FOUND,
                    "user-not-found",
                    "User not found",
                    exception.getMessage(),
                    request);
            case OTP_NOT_FOUND -> response(
                    HttpStatus.NOT_FOUND,
                    "otp-not-found",
                    "OTP challenge not found",
                    exception.getMessage(),
                    request);
            case ACTIVE_OTP_EXISTS -> response(
                    HttpStatus.CONFLICT,
                    "active-otp-exists",
                    "Active OTP already exists",
                    exception.getMessage(),
                    request);
            case RESEND_LIMIT_REACHED -> response(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "otp-resend-limit-exceeded",
                    "OTP resend limit exceeded",
                    exception.getMessage(),
                    request);
        };
    }

    @ExceptionHandler(OtpRestException.class)
    ResponseEntity<ProblemDetail> handleOtpFailure(
            OtpRestException exception,
            HttpServletRequest request) {
        ProblemDetail detail = problem(
                exception.status(),
                exception.code(),
                exception.status().getReasonPhrase(),
                exception.getMessage(),
                request);
        detail.setType(exception.type());
        return ResponseEntity.status(exception.status()).body(detail);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpected(
            Exception exception,
            HttpServletRequest request) {
        LOGGER.error("Unhandled authentication API failure for {}", request.getRequestURI(), exception);
        return response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "internal-error",
                "Internal server error",
                "The request could not be completed.",
                request);
    }

    private ResponseEntity<ProblemDetail> response(
            HttpStatus status,
            String code,
            String title,
            String message,
            HttpServletRequest request) {
        return ResponseEntity.status(status).body(problem(status, code, title, message, request));
    }

    private ProblemDetail problem(
            HttpStatus status,
            String code,
            String title,
            String message,
            HttpServletRequest request) {
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
