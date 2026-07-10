package in.bachatsetu.backend.admin.interfaces.rest.controller;

import in.bachatsetu.backend.admin.application.query.PlatformGroupResult;
import in.bachatsetu.backend.admin.application.query.PlatformStatisticsResult;
import in.bachatsetu.backend.admin.application.query.PlatformTenantResult;
import in.bachatsetu.backend.admin.application.query.PlatformUserResult;
import in.bachatsetu.backend.admin.application.usecase.DisableUserUseCase;
import in.bachatsetu.backend.admin.application.usecase.EnableUserUseCase;
import in.bachatsetu.backend.admin.application.usecase.GetPlatformStatisticsUseCase;
import in.bachatsetu.backend.admin.application.usecase.ListPlatformGroupsUseCase;
import in.bachatsetu.backend.admin.application.usecase.ListPlatformTenantsUseCase;
import in.bachatsetu.backend.admin.application.usecase.ListPlatformUsersUseCase;
import in.bachatsetu.backend.admin.domain.port.PlatformPage;
import in.bachatsetu.backend.admin.interfaces.rest.dto.PageResponse;
import in.bachatsetu.backend.admin.interfaces.rest.dto.PlatformGroupResponse;
import in.bachatsetu.backend.admin.interfaces.rest.dto.PlatformStatisticsResponse;
import in.bachatsetu.backend.admin.interfaces.rest.dto.PlatformTenantResponse;
import in.bachatsetu.backend.admin.interfaces.rest.dto.PlatformUserResponse;
import in.bachatsetu.backend.admin.interfaces.rest.mapper.AdminApiMapper;
import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.auth.application.security.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes platform administration use cases without leaking admin logic into any other module, and without
 * exposing any repository or domain type. Restricted to platform administrators only, through the existing
 * {@code PLATFORM_ADMIN} role and Spring Security's already-enabled method security — no new filter, no new
 * authorization mechanism.
 */
@RestController
@RequestMapping(path = "/api/v1/admin", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Admin", description = "Platform administration: statistics, users, groups, tenants")
@ConditionalOnProperty(prefix = "bachatsetu.admin", name = "enabled", havingValue = "true", matchIfMissing = true)
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class AdminController {

    private static final String PROBLEM_CONTENT_TYPE = MediaType.APPLICATION_PROBLEM_JSON_VALUE;

    private final GetPlatformStatisticsUseCase getPlatformStatistics;
    private final ListPlatformUsersUseCase listPlatformUsers;
    private final ListPlatformGroupsUseCase listPlatformGroups;
    private final ListPlatformTenantsUseCase listPlatformTenants;
    private final EnableUserUseCase enableUser;
    private final DisableUserUseCase disableUser;
    private final CurrentUserProvider currentUserProvider;
    private final AdminApiMapper mapper;

    public AdminController(
            GetPlatformStatisticsUseCase getPlatformStatistics,
            ListPlatformUsersUseCase listPlatformUsers,
            ListPlatformGroupsUseCase listPlatformGroups,
            ListPlatformTenantsUseCase listPlatformTenants,
            EnableUserUseCase enableUser,
            DisableUserUseCase disableUser,
            CurrentUserProvider currentUserProvider,
            AdminApiMapper mapper) {
        this.getPlatformStatistics = getPlatformStatistics;
        this.listPlatformUsers = listPlatformUsers;
        this.listPlatformGroups = listPlatformGroups;
        this.listPlatformTenants = listPlatformTenants;
        this.enableUser = enableUser;
        this.disableUser = disableUser;
        this.currentUserProvider = currentUserProvider;
        this.mapper = mapper;
    }

    @GetMapping("/statistics")
    @Operation(summary = "Platform statistics", description = "Computes platform-wide totals on demand.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Statistics returned"),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "403", description = "Platform administrator role required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public PlatformStatisticsResponse statistics() {
        currentUserProvider.requireCurrentUser();
        PlatformStatisticsResult result = getPlatformStatistics.execute();
        return mapper.toResponse(result);
    }

    @GetMapping("/users")
    @Operation(summary = "List platform users", description = "Searches users across every tenant, page by page.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Page returned"),
        @ApiResponse(responseCode = "400", description = "Invalid filter, pagination, or sort parameters", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public PageResponse<PlatformUserResponse> users(
            @RequestParam(required = false) @Parameter(description = "Authentication status filter", example = "ACTIVE") String status,
            @RequestParam(required = false) @Parameter(description = "Email filter (partial match)") String email,
            @RequestParam(required = false) @Parameter(description = "Phone number filter (partial match)") String phone,
            @RequestParam(required = false) @Parameter(description = "Inclusive start of the created-at range") Instant createdAfter,
            @RequestParam(required = false) @Parameter(description = "Inclusive end of the created-at range") Instant createdBefore,
            @RequestParam(required = false) @Parameter(description = "Zero-based page index") Integer page,
            @RequestParam(required = false) @Parameter(description = "Page size") Integer size,
            @RequestParam(defaultValue = "createdAt") @Parameter(description = "Field to sort by") String sort,
            @RequestParam(defaultValue = "desc") @Parameter(description = "Sort direction") String direction) {
        currentUserProvider.requireCurrentUser();
        PlatformPage<PlatformUserResult> result = listPlatformUsers.execute(mapper.toUserSearchCriteria(
                status, email, phone, createdAfter, createdBefore, page, size, sort, direction));
        return mapper.toUserPageResponse(result);
    }

    @GetMapping("/groups")
    @Operation(summary = "List savings groups", description = "Searches savings groups across every tenant, page by page.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Page returned"),
        @ApiResponse(responseCode = "400", description = "Invalid filter or pagination parameters", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public PageResponse<PlatformGroupResponse> groups(
            @RequestParam(required = false) @Parameter(description = "Group status filter", example = "ACTIVE") String status,
            @RequestParam(required = false) @Parameter(description = "Inclusive start of the created-at range") Instant createdAfter,
            @RequestParam(required = false) @Parameter(description = "Inclusive end of the created-at range") Instant createdBefore,
            @RequestParam(required = false) @Parameter(description = "Zero-based page index") Integer page,
            @RequestParam(required = false) @Parameter(description = "Page size") Integer size,
            @RequestParam(defaultValue = "desc") @Parameter(description = "Sort direction") String direction) {
        currentUserProvider.requireCurrentUser();
        PlatformPage<PlatformGroupResult> result = listPlatformGroups.execute(
                mapper.toGroupSearchCriteria(status, createdAfter, createdBefore, page, size, direction));
        return mapper.toGroupPageResponse(result);
    }

    @GetMapping("/tenants")
    @Operation(summary = "List tenants", description = "Lists tenants known to the platform, with per-tenant totals.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Page returned"),
        @ApiResponse(responseCode = "400", description = "Invalid pagination parameters", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public PageResponse<PlatformTenantResponse> tenants(
            @RequestParam(required = false) @Parameter(description = "Zero-based page index") Integer page,
            @RequestParam(required = false) @Parameter(description = "Page size") Integer size) {
        currentUserProvider.requireCurrentUser();
        PlatformPage<PlatformTenantResult> result = listPlatformTenants.execute(mapper.toPageRequest(page, size));
        return mapper.toTenantPageResponse(result);
    }

    @PostMapping("/users/{id}/enable")
    @Operation(summary = "Enable a platform user", description = "Enables one platform user, across any tenant.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User enabled"),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "User not found", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public PlatformUserResponse enable(@PathVariable String id) {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        PlatformUserResult result = enableUser.execute(mapper.toEnableCommand(id, currentUser));
        return mapper.toResponse(result);
    }

    @PostMapping("/users/{id}/disable")
    @Operation(summary = "Disable a platform user", description = "Disables one platform user, across any tenant.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User disabled"),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "User not found", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public PlatformUserResponse disable(@PathVariable String id) {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        PlatformUserResult result = disableUser.execute(mapper.toDisableCommand(id, currentUser));
        return mapper.toResponse(result);
    }
}
