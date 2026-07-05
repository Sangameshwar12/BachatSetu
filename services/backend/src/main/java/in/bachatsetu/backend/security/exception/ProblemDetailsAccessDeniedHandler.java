package in.bachatsetu.backend.security.exception;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

/** Produces a stable forbidden RFC 7807 response. */
public final class ProblemDetailsAccessDeniedHandler implements AccessDeniedHandler {

    private final SecurityProblemWriter writer;

    public ProblemDetailsAccessDeniedHandler(SecurityProblemWriter writer) {
        this.writer = Objects.requireNonNull(writer, "security problem writer must not be null");
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException, ServletException {
        writer.write(
                request,
                response,
                HttpStatus.FORBIDDEN,
                "access-denied",
                "Access denied",
                "The authenticated user is not permitted to access this resource.");
    }
}
