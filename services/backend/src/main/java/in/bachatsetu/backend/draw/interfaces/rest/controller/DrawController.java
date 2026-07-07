package in.bachatsetu.backend.draw.interfaces.rest.controller;

import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.auth.application.security.CurrentUserProvider;
import in.bachatsetu.backend.draw.application.query.DrawResult;
import in.bachatsetu.backend.draw.application.usecase.CloseDrawUseCase;
import in.bachatsetu.backend.draw.application.usecase.ConductDrawUseCase;
import in.bachatsetu.backend.draw.application.usecase.CreateDrawUseCase;
import in.bachatsetu.backend.draw.application.usecase.GetDrawUseCase;
import in.bachatsetu.backend.draw.application.usecase.ListDrawsUseCase;
import in.bachatsetu.backend.draw.interfaces.rest.dto.CloseDrawRequest;
import in.bachatsetu.backend.draw.interfaces.rest.dto.CreateDrawRequest;
import in.bachatsetu.backend.draw.interfaces.rest.dto.DrawResponse;
import in.bachatsetu.backend.draw.interfaces.rest.dto.DrawSummaryResponse;
import in.bachatsetu.backend.draw.interfaces.rest.dto.PageResponse;
import in.bachatsetu.backend.draw.interfaces.rest.mapper.DrawApiMapper;
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

/** Exposes Draw use cases without leaking domain or persistence models. */
@RestController
@Validated
@RequestMapping(path = "/api/v1/draws", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Draws", description = "Schedule and conduct savings group draws")
@ConditionalOnProperty(
        prefix = "bachatsetu.draw.rest",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class DrawController {

    private static final String PROBLEM_CONTENT_TYPE = MediaType.APPLICATION_PROBLEM_JSON_VALUE;

    private final CreateDrawUseCase createDraw;
    private final GetDrawUseCase getDraw;
    private final ListDrawsUseCase listDraws;
    private final ConductDrawUseCase conductDraw;
    private final CloseDrawUseCase closeDraw;
    private final CurrentUserProvider currentUserProvider;
    private final DrawApiMapper mapper;

    public DrawController(
            CreateDrawUseCase createDraw,
            GetDrawUseCase getDraw,
            ListDrawsUseCase listDraws,
            ConductDrawUseCase conductDraw,
            CloseDrawUseCase closeDraw,
            CurrentUserProvider currentUserProvider,
            DrawApiMapper mapper) {
        this.createDraw = createDraw;
        this.getDraw = getDraw;
        this.listDraws = listDraws;
        this.conductDraw = conductDraw;
        this.closeDraw = closeDraw;
        this.currentUserProvider = currentUserProvider;
        this.mapper = mapper;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Schedule a draw", description = "Creates a new scheduled draw for a savings group cycle.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Draw scheduled"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "422", description = "Business validation failed", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<DrawResponse> create(@Valid @RequestBody CreateDrawRequest request) {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        DrawResult result = createDraw.execute(mapper.toCreateCommand(request, currentUser));
        return ResponseEntity.created(URI.create("/api/v1/draws/" + result.drawId()))
                .body(mapper.toResponse(result));
    }

    @GetMapping("/{drawId}")
    @Operation(summary = "Get a draw", description = "Retrieves one tenant-scoped draw.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Draw returned"),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Draw not found", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public DrawResponse get(@PathVariable String drawId) {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        DrawResult result = mapper.getDraw(getDraw, currentUser, drawId);
        return mapper.toResponse(result);
    }

    @GetMapping
    @Operation(
            summary = "List draws",
            description = "Lists draws within the authenticated caller's tenant, page by page.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Page returned"),
        @ApiResponse(responseCode = "400", description = "Invalid pagination or sort parameters", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public PageResponse<DrawSummaryResponse> list(
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
        return mapper.listDraws(listDraws, currentUser, page, size, sort, direction);
    }

    @PatchMapping("/{drawId}/conduct")
    @Operation(
            summary = "Conduct a draw",
            description = "Opens a scheduled draw so it can be conducted.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Draw conducted"),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Draw not found", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "422", description = "Invalid lifecycle transition", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public DrawResponse conduct(@PathVariable String drawId) {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        DrawResult result = conductDraw.execute(mapper.toConductCommand(drawId, currentUser));
        return mapper.toResponse(result);
    }

    @PatchMapping(path = "/{drawId}/close", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Close a draw", description = "Closes an open draw with its winning member.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Draw closed"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Draw not found", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "422", description = "Invalid lifecycle transition", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public DrawResponse close(@PathVariable String drawId, @Valid @RequestBody CloseDrawRequest request) {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        DrawResult result = closeDraw.execute(mapper.toCloseCommand(drawId, request, currentUser));
        return mapper.toResponse(result);
    }
}
