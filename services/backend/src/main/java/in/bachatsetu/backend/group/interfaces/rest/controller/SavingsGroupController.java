package in.bachatsetu.backend.group.interfaces.rest.controller;

import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.auth.application.security.CurrentUserProvider;
import in.bachatsetu.backend.group.application.query.SavingsGroupResult;
import in.bachatsetu.backend.group.application.usecase.CreateSavingsGroupUseCase;
import in.bachatsetu.backend.group.interfaces.rest.dto.CreateSavingsGroupRequest;
import in.bachatsetu.backend.group.interfaces.rest.dto.SavingsGroupResponse;
import in.bachatsetu.backend.group.interfaces.rest.mapper.SavingsGroupApiMapper;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Exposes Savings Group use cases without leaking domain or persistence models. */
@RestController
@RequestMapping(path = "/api/v1/groups", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Savings Groups", description = "Create and manage Bhishi savings groups")
@ConditionalOnProperty(
        prefix = "bachatsetu.group.rest",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class SavingsGroupController {

    private final CreateSavingsGroupUseCase createSavingsGroup;
    private final CurrentUserProvider currentUserProvider;
    private final SavingsGroupApiMapper mapper;

    public SavingsGroupController(
            CreateSavingsGroupUseCase createSavingsGroup,
            CurrentUserProvider currentUserProvider,
            SavingsGroupApiMapper mapper) {
        this.createSavingsGroup = createSavingsGroup;
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
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "409", description = "Generated group code already exists", content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "422", description = "Business validation failed", content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<SavingsGroupResponse> create(@Valid @RequestBody CreateSavingsGroupRequest request) {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        SavingsGroupResult result = createSavingsGroup.execute(mapper.toCommand(request, currentUser));
        return ResponseEntity.created(URI.create("/api/v1/groups/" + result.groupId()))
                .body(mapper.toResponse(result));
    }
}
