package in.bachatsetu.backend.platformoperations.interfaces.rest.controller;

import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.auth.application.security.CurrentUserProvider;
import in.bachatsetu.backend.platformoperations.application.query.AnnouncementResult;
import in.bachatsetu.backend.platformoperations.application.usecase.ListActiveAnnouncementsUseCase;
import in.bachatsetu.backend.platformoperations.application.usecase.ListAnnouncementsUseCase;
import in.bachatsetu.backend.platformoperations.application.usecase.PublishAnnouncementUseCase;
import in.bachatsetu.backend.platformoperations.interfaces.rest.dto.AnnouncementResponse;
import in.bachatsetu.backend.platformoperations.interfaces.rest.dto.PlatformOperationsPageResponse;
import in.bachatsetu.backend.platformoperations.interfaces.rest.dto.PublishAnnouncementRequest;
import in.bachatsetu.backend.platformoperations.interfaces.rest.mapper.PlatformOperationsApiMapper;
import in.bachatsetu.backend.shared.domain.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Platform-wide announcements. Publishing and listing every announcement is platform administrator only;
 * listing the currently active ones is open to any authenticated user, since a future mobile/web frontend
 * needs to display them to every user, not just administrators.
 */
@RestController
@RequestMapping(path = "/api/v1/platform-operations/announcements", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Platform Operations", description = "Super admin dashboard, tenant management, announcements, broadcasts, and health")
@ConditionalOnProperty(
        prefix = "bachatsetu.platform-operations.rest", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AnnouncementController {

    private final PublishAnnouncementUseCase publishAnnouncement;
    private final ListAnnouncementsUseCase listAnnouncements;
    private final ListActiveAnnouncementsUseCase listActiveAnnouncements;
    private final CurrentUserProvider currentUserProvider;
    private final PlatformOperationsApiMapper mapper;

    public AnnouncementController(
            PublishAnnouncementUseCase publishAnnouncement,
            ListAnnouncementsUseCase listAnnouncements,
            ListActiveAnnouncementsUseCase listActiveAnnouncements,
            CurrentUserProvider currentUserProvider,
            PlatformOperationsApiMapper mapper) {
        this.publishAnnouncement = publishAnnouncement;
        this.listAnnouncements = listAnnouncements;
        this.listActiveAnnouncements = listActiveAnnouncements;
        this.currentUserProvider = currentUserProvider;
        this.mapper = mapper;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Publish a platform announcement", description = "Platform administrator only.")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public AnnouncementResponse publish(@Valid @RequestBody PublishAnnouncementRequest request) {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        AnnouncementResult result = publishAnnouncement.execute(mapper.toPublishCommand(currentUser, request));
        return mapper.toResponse(result);
    }

    @GetMapping
    @Operation(summary = "List every announcement", description = "Platform administrator only.")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public PlatformOperationsPageResponse<AnnouncementResponse> list(
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        currentUserProvider.requireCurrentUser();
        Page<AnnouncementResult> result = listAnnouncements.execute(mapper.toPageQuery(page, size));
        return mapper.toAnnouncementPageResponse(result);
    }

    @GetMapping("/active")
    @Operation(summary = "List currently active announcements", description = "Any authenticated user.")
    public List<AnnouncementResponse> active() {
        currentUserProvider.requireCurrentUser();
        return listActiveAnnouncements.execute().stream().map(mapper::toResponse).toList();
    }
}
