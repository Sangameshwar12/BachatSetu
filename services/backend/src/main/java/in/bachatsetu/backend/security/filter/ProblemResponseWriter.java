package in.bachatsetu.backend.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.MediaType;

/**
 * Writes an RFC 7807 problem body directly to the servlet response, for use by filters that run outside
 * the DispatcherServlet's exception-handling machinery (so a {@code @RestControllerAdvice} cannot apply).
 * The JSON shape mirrors {@code AdminExceptionHandler}'s problem builder exactly.
 */
final class ProblemResponseWriter {

    private static final String TYPE_PREFIX = "urn:bachatsetu:problem:";

    private final ObjectMapper objectMapper;

    ProblemResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    void write(
            HttpServletRequest request,
            HttpServletResponse response,
            int status,
            String code,
            String title,
            String detail) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", TYPE_PREFIX + code);
        body.put("title", title);
        body.put("status", status);
        body.put("detail", detail);
        body.put("instance", request.getRequestURI());
        body.put("code", code);
        body.put("timestamp", Instant.now().toString());
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
