package in.bachatsetu.backend.platformoperations.interfaces.rest.controller;

import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.auth.application.security.CurrentUserProvider;
import in.bachatsetu.backend.platformoperations.application.query.BroadcastResult;
import in.bachatsetu.backend.platformoperations.application.usecase.SendBroadcastNotificationUseCase;
import in.bachatsetu.backend.platformoperations.interfaces.rest.dto.BroadcastRequest;
import in.bachatsetu.backend.platformoperations.interfaces.rest.dto.BroadcastResponse;
import in.bachatsetu.backend.platformoperations.interfaces.rest.mapper.PlatformOperationsApiMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Broadcast notifications, reusing the existing Notification module. Platform administrator only. */
@RestController
@RequestMapping(path = "/api/v1/platform-operations/broadcast", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Platform Operations", description = "Super admin dashboard, tenant management, announcements, broadcasts, and health")
@ConditionalOnProperty(
        prefix = "bachatsetu.platform-operations.rest", name = "enabled", havingValue = "true", matchIfMissing = true)
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class BroadcastController {

    private final SendBroadcastNotificationUseCase sendBroadcastNotification;
    private final CurrentUserProvider currentUserProvider;
    private final PlatformOperationsApiMapper mapper;

    public BroadcastController(
            SendBroadcastNotificationUseCase sendBroadcastNotification, CurrentUserProvider currentUserProvider,
            PlatformOperationsApiMapper mapper) {
        this.sendBroadcastNotification = sendBroadcastNotification;
        this.currentUserProvider = currentUserProvider;
        this.mapper = mapper;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Send a broadcast notification", description =
            "Scopes: ALL_USERS, TENANT (requires tenantId), ORGANIZERS, MEMBERS. Reuses the Notification module.")
    public BroadcastResponse send(@Valid @RequestBody BroadcastRequest request) {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        BroadcastResult result = sendBroadcastNotification.execute(mapper.toBroadcastCommand(currentUser, request));
        return mapper.toResponse(result);
    }
}
