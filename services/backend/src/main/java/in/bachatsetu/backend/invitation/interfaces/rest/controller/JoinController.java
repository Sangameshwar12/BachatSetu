package in.bachatsetu.backend.invitation.interfaces.rest.controller;

import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.auth.application.security.CurrentUserProvider;
import in.bachatsetu.backend.invitation.application.query.InvitationAcceptedResult;
import in.bachatsetu.backend.invitation.application.query.InvitationPreviewResult;
import in.bachatsetu.backend.invitation.application.usecase.AcceptInvitationUseCase;
import in.bachatsetu.backend.invitation.application.usecase.PreviewInvitationUseCase;
import in.bachatsetu.backend.invitation.interfaces.rest.dto.AcceptInvitationRequest;
import in.bachatsetu.backend.invitation.interfaces.rest.dto.AcceptInvitationResponse;
import in.bachatsetu.backend.invitation.interfaces.rest.dto.InvitationPreviewResponse;
import in.bachatsetu.backend.invitation.interfaces.rest.mapper.InvitationApiMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Member-facing endpoints: a public join preview and the authenticated join action. */
@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Group Joining", description = "Preview and accept a group invitation")
@ConditionalOnProperty(
        prefix = "bachatsetu.invitation.rest",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class JoinController {

    private static final String PROBLEM_CONTENT_TYPE = MediaType.APPLICATION_PROBLEM_JSON_VALUE;

    private final PreviewInvitationUseCase previewInvitation;
    private final AcceptInvitationUseCase acceptInvitation;
    private final CurrentUserProvider currentUserProvider;
    private final InvitationApiMapper mapper;

    public JoinController(
            PreviewInvitationUseCase previewInvitation,
            AcceptInvitationUseCase acceptInvitation,
            CurrentUserProvider currentUserProvider,
            InvitationApiMapper mapper) {
        this.previewInvitation = previewInvitation;
        this.acceptInvitation = acceptInvitation;
        this.currentUserProvider = currentUserProvider;
        this.mapper = mapper;
    }

    @GetMapping(path = "/api/v1/join/{token}")
    @Operation(summary = "Preview a group before joining", description =
            "Public endpoint: shows group name, organizer, contribution, members, and frequency for a QR/link token.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Preview available"),
        @ApiResponse(responseCode = "404", description = "Invitation not found", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public InvitationPreviewResponse preview(@PathVariable String token) {
        InvitationPreviewResult result = previewInvitation.execute(token);
        return mapper.toResponse(result);
    }

    @PostMapping(path = "/api/v1/groups/join", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Join a group by code, QR token, or link token")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Joined the group"),
        @ApiResponse(responseCode = "404", description = "Invitation not found", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "409", description = "Already a member or group at capacity", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "422", description = "Invitation expired, used, or cancelled", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<AcceptInvitationResponse> join(@Valid @RequestBody AcceptInvitationRequest request) {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        InvitationAcceptedResult result = acceptInvitation.execute(mapper.toCommand(currentUser, request));
        return ResponseEntity.created(mapper.toLocationUri(result)).body(mapper.toResponse(result));
    }
}
