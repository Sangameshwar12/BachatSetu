package in.bachatsetu.backend.platformoperations.interfaces.rest.controller;

import in.bachatsetu.backend.auth.application.security.CurrentUserProvider;
import in.bachatsetu.backend.platformoperations.application.query.SystemHealthResult;
import in.bachatsetu.backend.platformoperations.application.usecase.GetSystemHealthUseCase;
import in.bachatsetu.backend.platformoperations.interfaces.rest.dto.SystemHealthResponse;
import in.bachatsetu.backend.platformoperations.interfaces.rest.mapper.PlatformOperationsApiMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only, detailed system health for the platform team — distinct from the pre-existing, unauthenticated
 * {@code /actuator/health} endpoint, which is left exactly as configured (no actuator redesign). This
 * endpoint exposes memory/disk/version detail that should not be public, so it requires the platform
 * administrator role like every other Platform Operations endpoint.
 */
@RestController
@RequestMapping(path = "/api/v1/platform-operations/health", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Platform Operations", description = "Super admin dashboard, tenant management, announcements, broadcasts, and health")
@ConditionalOnProperty(
        prefix = "bachatsetu.platform-operations.rest", name = "enabled", havingValue = "true", matchIfMissing = true)
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class SystemHealthController {

    private final GetSystemHealthUseCase getSystemHealth;
    private final CurrentUserProvider currentUserProvider;
    private final PlatformOperationsApiMapper mapper;

    public SystemHealthController(
            GetSystemHealthUseCase getSystemHealth, CurrentUserProvider currentUserProvider,
            PlatformOperationsApiMapper mapper) {
        this.getSystemHealth = getSystemHealth;
        this.currentUserProvider = currentUserProvider;
        this.mapper = mapper;
    }

    @GetMapping
    @Operation(summary = "Detailed system health", description =
            "Database, storage, and notification component health plus JVM/host runtime facts.")
    public SystemHealthResponse health() {
        currentUserProvider.requireCurrentUser();
        SystemHealthResult result = getSystemHealth.execute();
        return mapper.toResponse(result);
    }
}
