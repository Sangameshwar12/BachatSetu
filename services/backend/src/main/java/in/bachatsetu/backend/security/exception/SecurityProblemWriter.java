package in.bachatsetu.backend.security.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.time.Clock;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;

/** Writes RFC 7807 responses from the servlet security boundary. */
public final class SecurityProblemWriter {

    private static final String TYPE_PREFIX = "urn:bachatsetu:problem:";

    private final ObjectMapper objectMapper;
    private final Clock clock;

    public SecurityProblemWriter(ObjectMapper objectMapper, Clock clock) {
        this.objectMapper = java.util.Objects.requireNonNull(objectMapper, "object mapper must not be null");
        this.clock = java.util.Objects.requireNonNull(clock, "security clock must not be null");
    }

    public void write(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpStatus status,
            String code,
            String title,
            String detail) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create(TYPE_PREFIX + code));
        problem.setTitle(title);
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", code);
        problem.setProperty("timestamp", clock.instant());
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
