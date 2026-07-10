package in.bachatsetu.backend.platformoperations.interfaces.rest.controller;

import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.auth.application.security.CurrentUserProvider;
import in.bachatsetu.backend.platformoperations.application.query.TenantResult;
import in.bachatsetu.backend.platformoperations.application.usecase.ActivateTenantUseCase;
import in.bachatsetu.backend.platformoperations.application.usecase.ArchiveTenantUseCase;
import in.bachatsetu.backend.platformoperations.application.usecase.GetTenantUseCase;
import in.bachatsetu.backend.platformoperations.application.usecase.SearchTenantsUseCase;
import in.bachatsetu.backend.platformoperations.application.usecase.SuspendTenantUseCase;
import in.bachatsetu.backend.platformoperations.interfaces.rest.dto.PlatformOperationsPageResponse;
import in.bachatsetu.backend.platformoperations.interfaces.rest.dto.SuspendTenantRequest;
import in.bachatsetu.backend.platformoperations.interfaces.rest.dto.TenantResponse;
import in.bachatsetu.backend.platformoperations.interfaces.rest.mapper.PlatformOperationsApiMapper;
import in.bachatsetu.backend.shared.domain.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Platform-wide tenant management: view, suspend, activate, archive. Platform administrator only. */
@RestController
@RequestMapping(path = "/api/v1/platform-operations/tenants", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Platform Operations", description = "Super admin dashboard, tenant management, announcements, broadcasts, and health")
@ConditionalOnProperty(
        prefix = "bachatsetu.platform-operations.rest", name = "enabled", havingValue = "true", matchIfMissing = true)
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class TenantController {

    private final SearchTenantsUseCase searchTenants;
    private final GetTenantUseCase getTenant;
    private final SuspendTenantUseCase suspendTenant;
    private final ActivateTenantUseCase activateTenant;
    private final ArchiveTenantUseCase archiveTenant;
    private final CurrentUserProvider currentUserProvider;
    private final PlatformOperationsApiMapper mapper;

    public TenantController(
            SearchTenantsUseCase searchTenants,
            GetTenantUseCase getTenant,
            SuspendTenantUseCase suspendTenant,
            ActivateTenantUseCase activateTenant,
            ArchiveTenantUseCase archiveTenant,
            CurrentUserProvider currentUserProvider,
            PlatformOperationsApiMapper mapper) {
        this.searchTenants = searchTenants;
        this.getTenant = getTenant;
        this.suspendTenant = suspendTenant;
        this.activateTenant = activateTenant;
        this.archiveTenant = archiveTenant;
        this.currentUserProvider = currentUserProvider;
        this.mapper = mapper;
    }

    @GetMapping
    @Operation(summary = "List tenants", description = "Lists every known tenant, with per-tenant statistics and lifecycle status.")
    public PlatformOperationsPageResponse<TenantResponse> search(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        currentUserProvider.requireCurrentUser();
        Page<TenantResult> result = searchTenants.execute(
                mapper.toStatusFilter(status), mapper.toPageQuery(page, size));
        return mapper.toTenantPageResponse(result);
    }

    @GetMapping("/{tenantId}")
    @Operation(summary = "Get tenant statistics", description = "Statistics and lifecycle status for one tenant.")
    public TenantResponse get(@PathVariable String tenantId) {
        currentUserProvider.requireCurrentUser();
        return mapper.get(getTenant, tenantId);
    }

    @PostMapping(path = "/{tenantId}/suspend", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Suspend a tenant")
    public TenantResponse suspend(@PathVariable String tenantId, @Valid @RequestBody SuspendTenantRequest request) {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        TenantResult result = suspendTenant.execute(mapper.toSuspendCommand(tenantId, currentUser, request));
        return mapper.toResponse(result);
    }

    @PostMapping("/{tenantId}/activate")
    @Operation(summary = "Activate a suspended tenant")
    public TenantResponse activate(@PathVariable String tenantId) {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        TenantResult result = activateTenant.execute(mapper.toActivateCommand(tenantId, currentUser));
        return mapper.toResponse(result);
    }

    @PostMapping("/{tenantId}/archive")
    @Operation(summary = "Archive a tenant")
    public TenantResponse archive(@PathVariable String tenantId) {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        TenantResult result = archiveTenant.execute(mapper.toArchiveCommand(tenantId, currentUser));
        return mapper.toResponse(result);
    }
}
