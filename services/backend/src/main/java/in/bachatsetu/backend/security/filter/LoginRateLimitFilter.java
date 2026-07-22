package in.bachatsetu.backend.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.bachatsetu.backend.auth.application.port.RateLimiterPort;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Throttles the OTP-triggering authentication endpoints per client IP address — a coarser,
 * complementary layer to {@code GenerateOtpApplicationService}'s per-mobile-number limit, since an
 * attacker enumerating many different mobile numbers from one address wouldn't otherwise be
 * slowed down by that per-number check alone.
 *
 * <p>If the underlying {@link RateLimiterPort} bean is unavailable (e.g. Redis auto-configuration
 * excluded in a minimal test context, exactly like {@link MaintenanceModeFilter}), this filter
 * fails open rather than blocking every request.
 */
public final class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> RATE_LIMITED_PATHS = Set.of(
            "/api/v1/auth/login/start",
            "/api/v1/auth/otp/request",
            "/api/v1/auth/otp/resend",
            "/api/v1/auth/signup");

    private final Supplier<RateLimiterPort> rateLimiterSupplier;
    private final int maxAttempts;
    private final Duration window;
    private final ProblemResponseWriter problemResponseWriter;

    public LoginRateLimitFilter(
            Supplier<RateLimiterPort> rateLimiterSupplier, int maxAttempts, Duration window,
            ObjectMapper objectMapper) {
        this.rateLimiterSupplier = Objects.requireNonNull(rateLimiterSupplier, "rateLimiterSupplier must not be null");
        if (maxAttempts <= 0) {
            throw new IllegalArgumentException("maxAttempts must be positive");
        }
        this.maxAttempts = maxAttempts;
        this.window = Objects.requireNonNull(window, "window must not be null");
        this.problemResponseWriter = new ProblemResponseWriter(objectMapper);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!HttpMethod.POST.matches(request.getMethod()) || !RATE_LIMITED_PATHS.contains(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }
        RateLimiterPort rateLimiter = rateLimiterSupplier.get();
        if (rateLimiter == null) {
            filterChain.doFilter(request, response);
            return;
        }
        boolean allowed = rateLimiter.tryConsume("login-ip:" + request.getRemoteAddr(), maxAttempts, window);
        if (allowed) {
            filterChain.doFilter(request, response);
            return;
        }
        problemResponseWriter.write(
                request, response, HttpStatus.TOO_MANY_REQUESTS.value(), "rate-limit-exceeded",
                "Too many requests",
                "Too many authentication requests from this address. Please try again shortly.");
    }
}
