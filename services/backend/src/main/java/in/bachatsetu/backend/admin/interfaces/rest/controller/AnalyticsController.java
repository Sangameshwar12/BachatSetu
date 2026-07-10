package in.bachatsetu.backend.admin.interfaces.rest.controller;

import in.bachatsetu.backend.admin.application.analytics.query.GroupAnalyticsResult;
import in.bachatsetu.backend.admin.application.analytics.query.NotificationAnalyticsResult;
import in.bachatsetu.backend.admin.application.analytics.query.OverviewAnalyticsResult;
import in.bachatsetu.backend.admin.application.analytics.query.PaymentAnalyticsResult;
import in.bachatsetu.backend.admin.application.analytics.query.StorageAnalyticsResult;
import in.bachatsetu.backend.admin.application.analytics.query.UserAnalyticsResult;
import in.bachatsetu.backend.admin.application.analytics.usecase.GetGroupAnalyticsUseCase;
import in.bachatsetu.backend.admin.application.analytics.usecase.GetNotificationAnalyticsUseCase;
import in.bachatsetu.backend.admin.application.analytics.usecase.GetOverviewAnalyticsUseCase;
import in.bachatsetu.backend.admin.application.analytics.usecase.GetPaymentAnalyticsUseCase;
import in.bachatsetu.backend.admin.application.analytics.usecase.GetStorageAnalyticsUseCase;
import in.bachatsetu.backend.admin.application.analytics.usecase.GetUserAnalyticsUseCase;
import in.bachatsetu.backend.admin.interfaces.rest.dto.analytics.GroupAnalyticsResponse;
import in.bachatsetu.backend.admin.interfaces.rest.dto.analytics.NotificationAnalyticsResponse;
import in.bachatsetu.backend.admin.interfaces.rest.dto.analytics.OverviewAnalyticsResponse;
import in.bachatsetu.backend.admin.interfaces.rest.dto.analytics.PaymentAnalyticsResponse;
import in.bachatsetu.backend.admin.interfaces.rest.dto.analytics.StorageAnalyticsResponse;
import in.bachatsetu.backend.admin.interfaces.rest.dto.analytics.UserAnalyticsResponse;
import in.bachatsetu.backend.admin.interfaces.rest.mapper.AnalyticsApiMapper;
import in.bachatsetu.backend.auth.application.security.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes read-only platform analytics without leaking analytics computation into any other module.
 * Restricted to platform administrators only, through the same {@code PLATFORM_ADMIN} role check as the
 * rest of the Admin module — no new authorization mechanism.
 */
@RestController
@RequestMapping(path = "/api/v1/admin/analytics", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Admin Analytics", description = "Platform-wide, read-only analytics for platform administrators")
@ConditionalOnProperty(
        prefix = "bachatsetu.admin.analytics",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class AnalyticsController {

    private static final String PROBLEM_CONTENT_TYPE = MediaType.APPLICATION_PROBLEM_JSON_VALUE;

    private final GetOverviewAnalyticsUseCase getOverviewAnalytics;
    private final GetPaymentAnalyticsUseCase getPaymentAnalytics;
    private final GetGroupAnalyticsUseCase getGroupAnalytics;
    private final GetUserAnalyticsUseCase getUserAnalytics;
    private final GetNotificationAnalyticsUseCase getNotificationAnalytics;
    private final GetStorageAnalyticsUseCase getStorageAnalytics;
    private final CurrentUserProvider currentUserProvider;
    private final AnalyticsApiMapper mapper;

    public AnalyticsController(
            GetOverviewAnalyticsUseCase getOverviewAnalytics,
            GetPaymentAnalyticsUseCase getPaymentAnalytics,
            GetGroupAnalyticsUseCase getGroupAnalytics,
            GetUserAnalyticsUseCase getUserAnalytics,
            GetNotificationAnalyticsUseCase getNotificationAnalytics,
            GetStorageAnalyticsUseCase getStorageAnalytics,
            CurrentUserProvider currentUserProvider,
            AnalyticsApiMapper mapper) {
        this.getOverviewAnalytics = getOverviewAnalytics;
        this.getPaymentAnalytics = getPaymentAnalytics;
        this.getGroupAnalytics = getGroupAnalytics;
        this.getUserAnalytics = getUserAnalytics;
        this.getNotificationAnalytics = getNotificationAnalytics;
        this.getStorageAnalytics = getStorageAnalytics;
        this.currentUserProvider = currentUserProvider;
        this.mapper = mapper;
    }

    @GetMapping("/overview")
    @Operation(summary = "Platform overview analytics", description = "Computes the platform-wide overview snapshot on demand.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Analytics returned"),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "403", description = "Platform administrator role required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public OverviewAnalyticsResponse overview() {
        OverviewAnalyticsResult result = getOverviewAnalytics.execute(mapper.toCommand(currentUserProvider.requireCurrentUser()));
        return mapper.toResponse(result);
    }

    @GetMapping("/payments")
    @Operation(summary = "Payment analytics", description = "Computes payment analytics on demand, including a 30-day trend.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Analytics returned"),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "403", description = "Platform administrator role required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public PaymentAnalyticsResponse payments() {
        PaymentAnalyticsResult result = getPaymentAnalytics.execute(mapper.toCommand(currentUserProvider.requireCurrentUser()));
        return mapper.toResponse(result);
    }

    @GetMapping("/groups")
    @Operation(summary = "Savings group analytics", description = "Computes savings group analytics on demand.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Analytics returned"),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "403", description = "Platform administrator role required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public GroupAnalyticsResponse groups() {
        GroupAnalyticsResult result = getGroupAnalytics.execute(mapper.toCommand(currentUserProvider.requireCurrentUser()));
        return mapper.toResponse(result);
    }

    @GetMapping("/users")
    @Operation(summary = "Platform user analytics", description = "Computes platform user analytics on demand.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Analytics returned"),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "403", description = "Platform administrator role required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public UserAnalyticsResponse users() {
        UserAnalyticsResult result = getUserAnalytics.execute(mapper.toCommand(currentUserProvider.requireCurrentUser()));
        return mapper.toResponse(result);
    }

    @GetMapping("/storage")
    @Operation(summary = "Storage analytics", description = "Computes storage analytics on demand, including a 30-day upload trend.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Analytics returned"),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "403", description = "Platform administrator role required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public StorageAnalyticsResponse storage() {
        StorageAnalyticsResult result = getStorageAnalytics.execute(mapper.toCommand(currentUserProvider.requireCurrentUser()));
        return mapper.toResponse(result);
    }

    @GetMapping("/notifications")
    @Operation(summary = "Notification analytics", description = "Computes notification analytics on demand.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Analytics returned"),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "403", description = "Platform administrator role required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public NotificationAnalyticsResponse notifications() {
        NotificationAnalyticsResult result =
                getNotificationAnalytics.execute(mapper.toCommand(currentUserProvider.requireCurrentUser()));
        return mapper.toResponse(result);
    }
}
