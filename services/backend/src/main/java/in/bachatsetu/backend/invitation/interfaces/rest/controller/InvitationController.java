package in.bachatsetu.backend.invitation.interfaces.rest.controller;

import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.auth.application.security.CurrentUserProvider;
import in.bachatsetu.backend.invitation.application.query.InvitationResult;
import in.bachatsetu.backend.invitation.application.usecase.CreateInvitationUseCase;
import in.bachatsetu.backend.invitation.application.usecase.GetCurrentInvitationUseCase;
import in.bachatsetu.backend.invitation.application.usecase.RevokeInvitationUseCase;
import in.bachatsetu.backend.invitation.interfaces.rest.dto.CreateInvitationRequest;
import in.bachatsetu.backend.invitation.interfaces.rest.dto.InvitationResponse;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Organizer-only endpoints for generating, viewing, and revoking a group's invitation. */
@RestController
@RequestMapping(path = "/api/v1/groups/{groupId}/invite", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Group Invitations", description = "Organizer-managed group invitations")
@ConditionalOnProperty(
        prefix = "bachatsetu.invitation.rest",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class InvitationController {

    private static final String PROBLEM_CONTENT_TYPE = MediaType.APPLICATION_PROBLEM_JSON_VALUE;

    private final CreateInvitationUseCase createInvitation;
    private final GetCurrentInvitationUseCase getCurrentInvitation;
    private final RevokeInvitationUseCase revokeInvitation;
    private final CurrentUserProvider currentUserProvider;
    private final InvitationApiMapper mapper;

    public InvitationController(
            CreateInvitationUseCase createInvitation,
            GetCurrentInvitationUseCase getCurrentInvitation,
            RevokeInvitationUseCase revokeInvitation,
            CurrentUserProvider currentUserProvider,
            InvitationApiMapper mapper) {
        this.createInvitation = createInvitation;
        this.getCurrentInvitation = getCurrentInvitation;
        this.revokeInvitation = revokeInvitation;
        this.currentUserProvider = currentUserProvider;
        this.mapper = mapper;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Generate the group's invitation", description =
            "Creates a new invitation code/token, replacing (revoking) any prior active invitation.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Invitation generated"),
        @ApiResponse(responseCode = "403", description = "Only the group owner may invite", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Group not found", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public InvitationResponse create(
            @PathVariable String groupId, @Valid @RequestBody CreateInvitationRequest request) {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        InvitationResult result = createInvitation.execute(mapper.toCommand(groupId, currentUser, request));
        return mapper.toResponse(result);
    }

    @GetMapping
    @Operation(summary = "View the group's current invitation")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Current invitation"),
        @ApiResponse(responseCode = "404", description = "No active invitation", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public InvitationResponse current(@PathVariable String groupId) {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        InvitationResult result = mapper.current(getCurrentInvitation, groupId, currentUser);
        return mapper.toResponse(result);
    }

    @DeleteMapping
    @Operation(summary = "Revoke the group's current invitation")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Invitation revoked"),
        @ApiResponse(responseCode = "404", description = "No active invitation", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Void> revoke(@PathVariable String groupId) {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        revokeInvitation.execute(mapper.toRevokeCommand(groupId, currentUser));
        return ResponseEntity.noContent().build();
    }
}
