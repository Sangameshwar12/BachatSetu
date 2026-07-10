package in.bachatsetu.backend.admin.interfaces.rest.controller;

import in.bachatsetu.backend.admin.application.configuration.query.FeatureFlagResult;
import in.bachatsetu.backend.admin.application.configuration.query.PlatformConfigurationResult;
import in.bachatsetu.backend.admin.application.configuration.query.PlatformLimitResult;
import in.bachatsetu.backend.admin.application.configuration.usecase.GetConfigurationUseCase;
import in.bachatsetu.backend.admin.application.configuration.usecase.GetFeatureFlagsUseCase;
import in.bachatsetu.backend.admin.application.configuration.usecase.GetSystemLimitsUseCase;
import in.bachatsetu.backend.admin.application.configuration.usecase.UpdateConfigurationUseCase;
import in.bachatsetu.backend.admin.application.configuration.usecase.UpdateFeatureFlagsUseCase;
import in.bachatsetu.backend.admin.application.configuration.usecase.UpdateSystemLimitsUseCase;
import in.bachatsetu.backend.admin.interfaces.rest.dto.config.FeatureFlagResponse;
import in.bachatsetu.backend.admin.interfaces.rest.dto.config.PlatformConfigurationResponse;
import in.bachatsetu.backend.admin.interfaces.rest.dto.config.PlatformLimitResponse;
import in.bachatsetu.backend.admin.interfaces.rest.dto.config.UpdateConfigurationRequest;
import in.bachatsetu.backend.admin.interfaces.rest.dto.config.UpdateFeatureFlagsRequest;
import in.bachatsetu.backend.admin.interfaces.rest.dto.config.UpdateSystemLimitsRequest;
import in.bachatsetu.backend.admin.interfaces.rest.mapper.PlatformConfigApiMapper;
import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.auth.application.security.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lets platform administrators inspect and change platform-wide configuration: general settings,
 * maintenance mode, feature flags, and system limits. Restricted to {@code PLATFORM_ADMIN} for both reads
 * and writes, through the same authorization mechanism as the rest of the Admin module.
 */
@RestController
@RequestMapping(path = "/api/v1/admin/config", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Platform Configuration", description = "Platform-wide configuration, feature flags, and system limits")
@ConditionalOnProperty(
        prefix = "bachatsetu.admin.platform-config",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class PlatformConfigController {

    private static final String PROBLEM_CONTENT_TYPE = MediaType.APPLICATION_PROBLEM_JSON_VALUE;

    private final GetConfigurationUseCase getConfiguration;
    private final UpdateConfigurationUseCase updateConfiguration;
    private final GetFeatureFlagsUseCase getFeatureFlags;
    private final UpdateFeatureFlagsUseCase updateFeatureFlags;
    private final GetSystemLimitsUseCase getSystemLimits;
    private final UpdateSystemLimitsUseCase updateSystemLimits;
    private final CurrentUserProvider currentUserProvider;
    private final PlatformConfigApiMapper mapper;

    public PlatformConfigController(
            GetConfigurationUseCase getConfiguration,
            UpdateConfigurationUseCase updateConfiguration,
            GetFeatureFlagsUseCase getFeatureFlags,
            UpdateFeatureFlagsUseCase updateFeatureFlags,
            GetSystemLimitsUseCase getSystemLimits,
            UpdateSystemLimitsUseCase updateSystemLimits,
            CurrentUserProvider currentUserProvider,
            PlatformConfigApiMapper mapper) {
        this.getConfiguration = getConfiguration;
        this.updateConfiguration = updateConfiguration;
        this.getFeatureFlags = getFeatureFlags;
        this.updateFeatureFlags = updateFeatureFlags;
        this.getSystemLimits = getSystemLimits;
        this.updateSystemLimits = updateSystemLimits;
        this.currentUserProvider = currentUserProvider;
        this.mapper = mapper;
    }

    @GetMapping
    @Operation(summary = "Get platform configuration", description = "Returns the current platform-wide settings and maintenance-mode state.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Configuration returned"),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "403", description = "Platform administrator role required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public PlatformConfigurationResponse configuration() {
        PlatformConfigurationResult result = getConfiguration.execute();
        return mapper.toResponse(result);
    }

    @PutMapping
    @Operation(summary = "Update platform configuration", description = "Full-replace update of platform settings and maintenance-mode state.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Configuration updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "403", description = "Platform administrator role required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public PlatformConfigurationResponse updateConfiguration(@Valid @RequestBody UpdateConfigurationRequest request) {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        PlatformConfigurationResult result = updateConfiguration.execute(mapper.toCommand(request, currentUser));
        return mapper.toResponse(result);
    }

    @GetMapping("/feature-flags")
    @Operation(summary = "List feature flags", description = "Returns the enable/disable state of every platform feature.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Feature flags returned"),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "403", description = "Platform administrator role required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public List<FeatureFlagResponse> featureFlags() {
        return getFeatureFlags.execute().stream().map(mapper::toResponse).toList();
    }

    @PutMapping("/feature-flags")
    @Operation(summary = "Update feature flags", description = "Partial update: only the feature keys present in the request body are changed.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Feature flags updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "403", description = "Platform administrator role required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public List<FeatureFlagResponse> updateFeatureFlags(@Valid @RequestBody UpdateFeatureFlagsRequest request) {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        List<FeatureFlagResult> results = updateFeatureFlags.execute(mapper.toCommand(request, currentUser));
        return results.stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/limits")
    @Operation(summary = "List system limits", description = "Returns every configurable platform-wide ceiling.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "System limits returned"),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "403", description = "Platform administrator role required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public List<PlatformLimitResponse> systemLimits() {
        return getSystemLimits.execute().stream().map(mapper::toResponse).toList();
    }

    @PutMapping("/limits")
    @Operation(summary = "Update system limits", description = "Partial update: only the limit keys present in the request body are changed.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "System limits updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "403", description = "Platform administrator role required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public List<PlatformLimitResponse> updateSystemLimits(@Valid @RequestBody UpdateSystemLimitsRequest request) {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        List<PlatformLimitResult> results = updateSystemLimits.execute(mapper.toCommand(request, currentUser));
        return results.stream().map(mapper::toResponse).toList();
    }
}
