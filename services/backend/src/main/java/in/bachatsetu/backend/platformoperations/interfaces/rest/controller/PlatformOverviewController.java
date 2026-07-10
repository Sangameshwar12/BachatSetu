package in.bachatsetu.backend.platformoperations.interfaces.rest.controller;

import in.bachatsetu.backend.auth.application.security.CurrentUserProvider;
import in.bachatsetu.backend.platformoperations.application.query.PlatformOverviewResult;
import in.bachatsetu.backend.platformoperations.application.usecase.GetPlatformOverviewUseCase;
import in.bachatsetu.backend.platformoperations.interfaces.rest.dto.PlatformOverviewResponse;
import in.bachatsetu.backend.platformoperations.interfaces.rest.mapper.PlatformOperationsApiMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Super Admin Dashboard: platform-wide totals computed on demand. Platform administrator only. */
@RestController
@RequestMapping(path = "/api/v1/platform-operations/overview", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Platform Operations", description = "Super admin dashboard, tenant management, announcements, broadcasts, and health")
@ConditionalOnProperty(
        prefix = "bachatsetu.platform-operations.rest", name = "enabled", havingValue = "true", matchIfMissing = true)
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class PlatformOverviewController {

    private final GetPlatformOverviewUseCase getPlatformOverview;
    private final CurrentUserProvider currentUserProvider;
    private final PlatformOperationsApiMapper mapper;

    public PlatformOverviewController(
            GetPlatformOverviewUseCase getPlatformOverview, CurrentUserProvider currentUserProvider,
            PlatformOperationsApiMapper mapper) {
        this.getPlatformOverview = getPlatformOverview;
        this.currentUserProvider = currentUserProvider;
        this.mapper = mapper;
    }

    @GetMapping
    @Operation(summary = "Platform overview", description = "Computes platform-wide totals and today's activity on demand.")
    public PlatformOverviewResponse overview() {
        currentUserProvider.requireCurrentUser();
        PlatformOverviewResult result = getPlatformOverview.execute();
        return mapper.toResponse(result);
    }
}
