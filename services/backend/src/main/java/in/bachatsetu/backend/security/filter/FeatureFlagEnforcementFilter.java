package in.bachatsetu.backend.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.bachatsetu.backend.admin.application.configuration.service.FeatureFlagQueryService;
import in.bachatsetu.backend.admin.domain.configuration.model.FeatureKey;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rejects requests to a disabled feature's endpoints with {@code 503 Service Unavailable}, keyed by request
 * path. This is the enforcement mechanism for "application services should consult FeatureFlagService before
 * executing business operations": rather than modifying every business module's constructors and services
 * (high blast radius across Payment/Notification/Storage/Auth/etc., explicitly out of scope for this
 * sprint), a single, additive, path-keyed filter consults {@link FeatureFlagQueryService} before any
 * matched request reaches a controller.
 *
 * <p>The {@code SIGNUP} feature has no dedicated REST path in this codebase (registration and login both go
 * through the same OTP-based {@code /api/v1/auth/otp} endpoints) and is therefore not enforced here; it
 * remains a stored, editable flag for future use. Every {@code /api/v1/admin/**} path except {@code
 * /api/v1/admin/analytics/**} is deliberately never gated, so an administrator can always reach the
 * platform-configuration endpoints (including to re-enable a feature they disabled).
 *
 * <p>If the underlying {@link FeatureFlagQueryService} bean is unavailable (e.g. persistence disabled in a
 * minimal test context), this filter fails open rather than blocking every request.
 */
public final class FeatureFlagEnforcementFilter extends OncePerRequestFilter {

    private final Supplier<FeatureFlagQueryService> featureFlagServiceSupplier;
    private final ProblemResponseWriter problemResponseWriter;

    public FeatureFlagEnforcementFilter(
            Supplier<FeatureFlagQueryService> featureFlagServiceSupplier, ObjectMapper objectMapper) {
        this.featureFlagServiceSupplier =
                Objects.requireNonNull(featureFlagServiceSupplier, "featureFlagServiceSupplier must not be null");
        this.problemResponseWriter = new ProblemResponseWriter(objectMapper);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Optional<FeatureKey> feature = featureFor(request.getRequestURI());
        if (feature.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }
        FeatureFlagQueryService featureFlagService = featureFlagServiceSupplier.get();
        if (featureFlagService == null || featureFlagService.isEnabled(feature.get())) {
            filterChain.doFilter(request, response);
            return;
        }
        problemResponseWriter.write(
                request, response, HttpStatus.SERVICE_UNAVAILABLE.value(), "feature-disabled", "Feature disabled",
                "The " + feature.get().name() + " feature is currently disabled.");
    }

    private Optional<FeatureKey> featureFor(String path) {
        if (path.startsWith("/api/v1/admin/analytics")) {
            return Optional.of(FeatureKey.ANALYTICS);
        }
        if (path.startsWith("/api/v1/admin")) {
            return Optional.empty();
        }
        if (path.startsWith("/api/v1/auth")) {
            return Optional.of(FeatureKey.AUTHENTICATION);
        }
        if (path.startsWith("/api/v1/payments")) {
            return Optional.of(FeatureKey.PAYMENTS);
        }
        if (path.startsWith("/api/v1/notifications")) {
            return Optional.of(FeatureKey.NOTIFICATIONS);
        }
        if (path.startsWith("/api/v1/storage")) {
            return Optional.of(FeatureKey.STORAGE);
        }
        if (path.startsWith("/api/v1/receipts")) {
            return Optional.of(FeatureKey.RECEIPTS);
        }
        if (path.startsWith("/api/v1/auctions")) {
            return Optional.of(FeatureKey.AUCTION);
        }
        if (path.startsWith("/api/v1/audit")) {
            return Optional.of(FeatureKey.AUDIT);
        }
        return Optional.empty();
    }
}
