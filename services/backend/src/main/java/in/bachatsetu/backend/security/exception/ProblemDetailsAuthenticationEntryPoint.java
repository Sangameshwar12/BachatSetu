package in.bachatsetu.backend.security.exception;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/** Produces a stable unauthenticated RFC 7807 response. */
public final class ProblemDetailsAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final SecurityProblemWriter writer;

    public ProblemDetailsAuthenticationEntryPoint(SecurityProblemWriter writer) {
        this.writer = Objects.requireNonNull(writer, "security problem writer must not be null");
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authenticationException) throws IOException, ServletException {
        writer.write(
                request,
                response,
                HttpStatus.UNAUTHORIZED,
                "authentication-required",
                "Authentication required",
                "A valid bearer access token is required.");
    }
}
