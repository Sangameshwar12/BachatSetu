package in.bachatsetu.backend.group.interfaces.rest.controller;

import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.auth.application.security.CurrentUserProvider;
import in.bachatsetu.backend.group.application.query.SavingsGroupResult;
import in.bachatsetu.backend.group.application.query.SavingsGroupSummary;
import in.bachatsetu.backend.group.application.usecase.ActivateGroupUseCase;
import in.bachatsetu.backend.group.application.usecase.CloseGroupUseCase;
import in.bachatsetu.backend.group.application.usecase.CreateSavingsGroupUseCase;
import in.bachatsetu.backend.group.application.usecase.GetSavingsGroupUseCase;
import in.bachatsetu.backend.group.application.usecase.JoinGroupUseCase;
import in.bachatsetu.backend.group.application.usecase.ListSavingsGroupsUseCase;
import in.bachatsetu.backend.group.application.usecase.RemoveMemberUseCase;
import in.bachatsetu.backend.group.application.usecase.SuspendGroupUseCase;
import in.bachatsetu.backend.group.interfaces.rest.dto.AddGroupMemberRequest;
import in.bachatsetu.backend.group.interfaces.rest.dto.CreateSavingsGroupRequest;
import in.bachatsetu.backend.group.interfaces.rest.dto.GroupMemberResponse;
import in.bachatsetu.backend.group.interfaces.rest.dto.PageResponse;
import in.bachatsetu.backend.group.interfaces.rest.dto.SavingsGroupResponse;
import in.bachatsetu.backend.group.interfaces.rest.dto.SavingsGroupSummaryResponse;
import in.bachatsetu.backend.group.interfaces.rest.mapper.SavingsGroupApiMapper;
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
import java.net.URI;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Exposes Savings Group use cases without leaking domain or persistence models. */
@RestController
@Validated
@RequestMapping(path = "/api/v1/groups", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Savings Groups", description = "Create and manage Bhishi savings groups")
@ConditionalOnProperty(
        prefix = "bachatsetu.group.rest",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class SavingsGroupController {

    private static final String PROBLEM_CONTENT_TYPE = MediaType.APPLICATION_PROBLEM_JSON_VALUE;

    private final CreateSavingsGroupUseCase createSavingsGroup;
    private final GetSavingsGroupUseCase getSavingsGroup;
    private final ListSavingsGroupsUseCase listSavingsGroups;
    private final ActivateGroupUseCase activateGroup;
    private final SuspendGroupUseCase suspendGroup;
    private final CloseGroupUseCase closeGroup;
    private final JoinGroupUseCase joinGroup;
    private final RemoveMemberUseCase removeMember;
    private final CurrentUserProvider currentUserProvider;
    private final SavingsGroupApiMapper mapper;

    public SavingsGroupController(
            CreateSavingsGroupUseCase createSavingsGroup,
            GetSavingsGroupUseCase getSavingsGroup,
            ListSavingsGroupsUseCase listSavingsGroups,
            ActivateGroupUseCase activateGroup,
            SuspendGroupUseCase suspendGroup,
            CloseGroupUseCase closeGroup,
            JoinGroupUseCase joinGroup,
            RemoveMemberUseCase removeMember,
            CurrentUserProvider currentUserProvider,
            SavingsGroupApiMapper mapper) {
        this.createSavingsGroup = createSavingsGroup;
        this.getSavingsGroup = getSavingsGroup;
        this.listSavingsGroups = listSavingsGroups;
        this.activateGroup = activateGroup;
        this.suspendGroup = suspendGroup;
        this.closeGroup = closeGroup;
        this.joinGroup = joinGroup;
        this.removeMember = removeMember;
        this.currentUserProvider = currentUserProvider;
        this.mapper = mapper;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Create a savings group",
            description = "Creates a new inactive savings group owned by the authenticated caller.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Group created"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "409", description = "Generated group code already exists", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "422", description = "Business validation failed", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<SavingsGroupResponse> create(@Valid @RequestBody CreateSavingsGroupRequest request) {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        SavingsGroupResult result = createSavingsGroup.execute(mapper.toCommand(request, currentUser));
        return ResponseEntity.created(URI.create("/api/v1/groups/" + result.groupId()))
                .body(mapper.toResponse(result));
    }

    @GetMapping("/{groupId}")
    @Operation(summary = "Get a savings group", description = "Retrieves one tenant-scoped savings group.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Group returned"),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Group not found", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public SavingsGroupResponse get(@PathVariable String groupId) {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        SavingsGroupResult result = mapper.getGroup(getSavingsGroup, currentUser, groupId);
        return mapper.toResponse(result);
    }

    @GetMapping
    @Operation(
            summary = "List savings groups",
            description = "Lists savings groups owned by the authenticated caller's tenant, page by page.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Page returned"),
        @ApiResponse(responseCode = "400", description = "Invalid pagination parameters", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public PageResponse<SavingsGroupSummaryResponse> list(
            @RequestParam(defaultValue = "0")
            @Min(0)
            @Parameter(description = "Zero-based page index") int page,
            @RequestParam(defaultValue = "20")
            @Min(1)
            @Max(100)
            @Parameter(description = "Page size, up to 100") int size) {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        List<SavingsGroupSummary> summaries = mapper.listGroups(listSavingsGroups, currentUser);
        return mapper.toSummaryPage(summaries, page, size);
    }

    @PatchMapping("/{groupId}/activate")
    @Operation(summary = "Activate a savings group", description = "Activates an inactive or suspended group.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Group activated"),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Group not found", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "422", description = "Invalid lifecycle transition", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public SavingsGroupResponse activate(@PathVariable String groupId) {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        SavingsGroupResult result = activateGroup.execute(mapper.toActivateCommand(groupId, currentUser));
        return mapper.toResponse(result);
    }

    @PatchMapping("/{groupId}/suspend")
    @Operation(summary = "Suspend a savings group", description = "Suspends an active group.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Group suspended"),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Group not found", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "422", description = "Invalid lifecycle transition", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public SavingsGroupResponse suspend(@PathVariable String groupId) {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        SavingsGroupResult result = suspendGroup.execute(mapper.toSuspendCommand(groupId, currentUser));
        return mapper.toResponse(result);
    }

    @PatchMapping("/{groupId}/close")
    @Operation(summary = "Close a savings group", description = "Permanently closes an active or inactive group.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Group closed"),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Group not found", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "422", description = "Invalid lifecycle transition", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public SavingsGroupResponse close(@PathVariable String groupId) {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        SavingsGroupResult result = closeGroup.execute(mapper.toCloseCommand(groupId, currentUser));
        return mapper.toResponse(result);
    }

    @PostMapping(path = "/{groupId}/members", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Add a group member", description = "Adds a member to an active savings group.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Member added"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Group not found", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "409", description = "Member already joined", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "422", description = "Group cannot accept members", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<GroupMemberResponse> addMember(
            @PathVariable String groupId,
            @Valid @RequestBody AddGroupMemberRequest request) {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        SavingsGroupResult result = joinGroup.execute(mapper.toJoinCommand(groupId, request, currentUser));
        return ResponseEntity.created(
                        URI.create("/api/v1/groups/" + groupId + "/members/" + request.memberId()))
                .body(mapper.toMemberResponse(result, request.memberId()));
    }

    @DeleteMapping("/{groupId}/members/{memberId}")
    @Operation(summary = "Remove a group member", description = "Removes a non-owner member from a savings group.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Member removed"),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Group not found", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "422", description = "Member cannot be removed", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Void> removeMember(@PathVariable String groupId, @PathVariable String memberId) {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        removeMember.execute(mapper.toRemoveMemberCommand(groupId, memberId, currentUser));
        return ResponseEntity.noContent().build();
    }
}
