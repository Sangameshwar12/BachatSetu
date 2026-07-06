package in.bachatsetu.backend.member.interfaces.rest.controller;

import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.auth.application.security.CurrentUserProvider;
import in.bachatsetu.backend.member.application.query.MemberProfileResult;
import in.bachatsetu.backend.member.application.usecase.CreateMemberProfileUseCase;
import in.bachatsetu.backend.member.application.usecase.GetMemberProfileUseCase;
import in.bachatsetu.backend.member.application.usecase.JoinGroupParticipationUseCase;
import in.bachatsetu.backend.member.interfaces.rest.dto.CreateMemberProfileRequest;
import in.bachatsetu.backend.member.interfaces.rest.dto.JoinGroupParticipationRequest;
import in.bachatsetu.backend.member.interfaces.rest.dto.MemberProfileResponse;
import in.bachatsetu.backend.member.interfaces.rest.mapper.MemberApiMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Exposes Member use cases without leaking domain or persistence models. */
@RestController
@Validated
@RequestMapping(path = "/api/v1/members", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Members", description = "Create and manage community member profiles")
@ConditionalOnProperty(
        prefix = "bachatsetu.member.rest",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class MemberController {

    private static final String PROBLEM_CONTENT_TYPE = MediaType.APPLICATION_PROBLEM_JSON_VALUE;

    private final CreateMemberProfileUseCase createMemberProfile;
    private final JoinGroupParticipationUseCase joinGroupParticipation;
    private final GetMemberProfileUseCase getMemberProfile;
    private final CurrentUserProvider currentUserProvider;
    private final MemberApiMapper mapper;

    public MemberController(
            CreateMemberProfileUseCase createMemberProfile,
            JoinGroupParticipationUseCase joinGroupParticipation,
            GetMemberProfileUseCase getMemberProfile,
            CurrentUserProvider currentUserProvider,
            MemberApiMapper mapper) {
        this.createMemberProfile = createMemberProfile;
        this.joinGroupParticipation = joinGroupParticipation;
        this.getMemberProfile = getMemberProfile;
        this.currentUserProvider = currentUserProvider;
        this.mapper = mapper;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Create a member profile",
            description = "Creates a new member profile together with its first group participation.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Member profile created"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "409", description = "Generated member number already exists", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "422", description = "Business validation failed", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<MemberProfileResponse> create(@Valid @RequestBody CreateMemberProfileRequest request) {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        MemberProfileResult result = createMemberProfile.execute(mapper.toCreateCommand(request, currentUser));
        return ResponseEntity.created(URI.create("/api/v1/members/" + result.memberId()))
                .body(mapper.toResponse(result));
    }

    @GetMapping("/{memberId}")
    @Operation(summary = "Get a member profile", description = "Retrieves one tenant-scoped member profile.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Member profile returned"),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Member profile not found", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public MemberProfileResponse get(@PathVariable String memberId) {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        MemberProfileResult result = mapper.getMember(getMemberProfile, currentUser, memberId);
        return mapper.toResponse(result);
    }

    @PostMapping(path = "/{memberId}/participations", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Join an additional group",
            description = "Adds a further group participation to an existing member profile.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Participation added"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Member profile not found", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "409", description = "Member already participates in the group", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "422", description = "Business validation failed", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<MemberProfileResponse> joinGroup(
            @PathVariable String memberId,
            @Valid @RequestBody JoinGroupParticipationRequest request) {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        MemberProfileResult result =
                joinGroupParticipation.execute(mapper.toJoinCommand(memberId, request, currentUser));
        return ResponseEntity.created(URI.create("/api/v1/members/" + memberId + "/participations/" + request.groupId()))
                .body(mapper.toResponse(result));
    }
}
