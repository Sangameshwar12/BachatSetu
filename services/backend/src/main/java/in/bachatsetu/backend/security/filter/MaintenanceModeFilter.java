package in.bachatsetu.backend.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.bachatsetu.backend.admin.application.configuration.service.MaintenanceStatus;
import in.bachatsetu.backend.admin.application.configuration.service.MaintenanceStatusQueryService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rejects requests with {@code 503 Service Unavailable} while the platform is in maintenance, except:
 * authentication endpoints, the health endpoint, API docs, the Admin module's own endpoints (so an
 * administrator can always inspect/undo maintenance mode), and any request already authenticated as
 * {@code PLATFORM_ADMIN} (who "continue to access everything", per the sprint's maintenance-mode contract).
 *
 * <p>Registered as a plain singleton bean, exactly like {@link JwtAuthenticationFilter}, running immediately
 * after it so the security context (and therefore the caller's roles) is already resolved. If the
 * underlying {@link MaintenanceStatusQueryService} bean is unavailable (e.g. persistence disabled in a
 * minimal test context), this filter fails open rather than blocking every request.
 */
public final class MaintenanceModeFilter extends OncePerRequestFilter {

    private static final String ROLE_PLATFORM_ADMIN = "ROLE_PLATFORM_ADMIN";
    private static final String DEFAULT_MESSAGE = "The platform is currently undergoing scheduled maintenance.";

    private final Supplier<MaintenanceStatusQueryService> statusServiceSupplier;
    private final Clock clock;
    private final ProblemResponseWriter problemResponseWriter;

    public MaintenanceModeFilter(
            Supplier<MaintenanceStatusQueryService> statusServiceSupplier, Clock clock, ObjectMapper objectMapper) {
        this.statusServiceSupplier =
                Objects.requireNonNull(statusServiceSupplier, "statusServiceSupplier must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.problemResponseWriter = new ProblemResponseWriter(objectMapper);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (isAlwaysAllowed(request.getRequestURI()) || isPlatformAdministrator()) {
            filterChain.doFilter(request, response);
            return;
        }
        MaintenanceStatusQueryService statusService = statusServiceSupplier.get();
        if (statusService == null) {
            filterChain.doFilter(request, response);
            return;
        }
        MaintenanceStatus status = statusService.currentStatus(Instant.now(clock));
        if (!status.active()) {
            filterChain.doFilter(request, response);
            return;
        }
        problemResponseWriter.write(
                request, response, HttpStatus.SERVICE_UNAVAILABLE.value(), "maintenance-mode",
                "Platform under maintenance",
                status.message() == null || status.message().isBlank() ? DEFAULT_MESSAGE : status.message());
    }

    private boolean isAlwaysAllowed(String path) {
        return path.startsWith("/actuator")
                || path.startsWith("/api/v1/auth")
                || path.startsWith("/api/v1/admin")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui");
    }

    private boolean isPlatformAdministrator() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> ROLE_PLATFORM_ADMIN.equals(authority.getAuthority()));
    }
}
