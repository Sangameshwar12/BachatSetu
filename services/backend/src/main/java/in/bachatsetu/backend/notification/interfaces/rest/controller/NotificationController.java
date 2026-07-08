package in.bachatsetu.backend.notification.interfaces.rest.controller;

import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.auth.application.security.CurrentUserProvider;
import in.bachatsetu.backend.notification.application.query.NotificationResult;
import in.bachatsetu.backend.notification.application.usecase.CreateNotificationUseCase;
import in.bachatsetu.backend.notification.application.usecase.GetNotificationUseCase;
import in.bachatsetu.backend.notification.application.usecase.ListNotificationsUseCase;
import in.bachatsetu.backend.notification.application.usecase.MarkNotificationDeliveredUseCase;
import in.bachatsetu.backend.notification.application.usecase.MarkNotificationFailedUseCase;
import in.bachatsetu.backend.notification.interfaces.rest.dto.CreateNotificationRequest;
import in.bachatsetu.backend.notification.interfaces.rest.dto.FailNotificationRequest;
import in.bachatsetu.backend.notification.interfaces.rest.dto.NotificationResponse;
import in.bachatsetu.backend.notification.interfaces.rest.dto.NotificationSummaryResponse;
import in.bachatsetu.backend.notification.interfaces.rest.dto.PageResponse;
import in.bachatsetu.backend.notification.interfaces.rest.mapper.NotificationApiMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.net.URI;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Exposes Notification use cases without leaking domain or persistence models. */
@RestController
@Validated
@RequestMapping(path = "/api/v1/notifications", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Notifications", description = "Create, retrieve, and track community notifications")
@ConditionalOnProperty(
        prefix = "bachatsetu.notification.rest",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class NotificationController {

    private static final String PROBLEM_CONTENT_TYPE = MediaType.APPLICATION_PROBLEM_JSON_VALUE;

    private final CreateNotificationUseCase createNotification;
    private final GetNotificationUseCase getNotification;
    private final ListNotificationsUseCase listNotifications;
    private final MarkNotificationDeliveredUseCase markNotificationDelivered;
    private final MarkNotificationFailedUseCase markNotificationFailed;
    private final CurrentUserProvider currentUserProvider;
    private final NotificationApiMapper mapper;

    public NotificationController(
            CreateNotificationUseCase createNotification,
            GetNotificationUseCase getNotification,
            ListNotificationsUseCase listNotifications,
            MarkNotificationDeliveredUseCase markNotificationDelivered,
            MarkNotificationFailedUseCase markNotificationFailed,
            CurrentUserProvider currentUserProvider,
            NotificationApiMapper mapper) {
        this.createNotification = createNotification;
        this.getNotification = getNotification;
        this.listNotifications = listNotifications;
        this.markNotificationDelivered = markNotificationDelivered;
        this.markNotificationFailed = markNotificationFailed;
        this.currentUserProvider = currentUserProvider;
        this.mapper = mapper;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Create a notification",
            description = "Creates a notification and dispatches it synchronously over its channel.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Notification created"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "422", description = "Business validation failed", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "502", description = "Channel dispatch failed", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<NotificationResponse> create(@Valid @RequestBody CreateNotificationRequest request) {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        NotificationResult result = createNotification.execute(mapper.toCreateCommand(request, currentUser));
        return ResponseEntity.created(URI.create("/api/v1/notifications/" + result.notificationId()))
                .body(mapper.toResponse(result));
    }

    @GetMapping("/{notificationId}")
    @Operation(summary = "Get a notification", description = "Retrieves one tenant-scoped notification.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Notification returned"),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Notification not found", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public NotificationResponse get(@PathVariable String notificationId) {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        NotificationResult result = mapper.getNotification(getNotification, currentUser, notificationId);
        return mapper.toResponse(result);
    }

    @GetMapping
    @Operation(
            summary = "List notifications",
            description = "Lists notifications within the authenticated caller's tenant, page by page.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Page returned"),
        @ApiResponse(responseCode = "400", description = "Invalid pagination or sort parameters", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public PageResponse<NotificationSummaryResponse> list(
            @RequestParam(defaultValue = "0")
            @Min(0)
            @Parameter(description = "Zero-based page index") int page,
            @RequestParam(defaultValue = "20")
            @Min(1)
            @Max(100)
            @Parameter(description = "Page size, up to 100") int size,
            @RequestParam(defaultValue = "createdAt")
            @Pattern(regexp = "scheduledAt|createdAt")
            @Parameter(description = "Field to sort by", example = "createdAt") String sort,
            @RequestParam(defaultValue = "asc")
            @Pattern(regexp = "asc|desc")
            @Parameter(description = "Sort direction", example = "asc") String direction) {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        return mapper.listNotifications(listNotifications, currentUser, page, size, sort, direction);
    }

    @PatchMapping("/{notificationId}/delivered")
    @Operation(summary = "Mark a notification delivered", description = "Confirms delivery of a sent notification.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Notification marked delivered"),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Notification not found", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "422", description = "Invalid lifecycle transition", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public NotificationResponse markDelivered(@PathVariable String notificationId) {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        NotificationResult result =
                markNotificationDelivered.execute(mapper.toMarkDeliveredCommand(notificationId, currentUser));
        return mapper.toResponse(result);
    }

    @PatchMapping(path = "/{notificationId}/failed", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Mark a notification failed", description = "Records a dispatch failure for a notification.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Notification marked failed"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Notification not found", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "422", description = "Invalid lifecycle transition", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public NotificationResponse markFailed(
            @PathVariable String notificationId,
            @Valid @RequestBody FailNotificationRequest request) {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        NotificationResult result = markNotificationFailed.execute(
                mapper.toMarkFailedCommand(notificationId, request.failureCode(), currentUser));
        return mapper.toResponse(result);
    }
}
