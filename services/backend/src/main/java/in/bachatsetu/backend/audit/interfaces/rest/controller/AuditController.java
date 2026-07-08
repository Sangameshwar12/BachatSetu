package in.bachatsetu.backend.audit.interfaces.rest.controller;

import in.bachatsetu.backend.audit.application.query.AuditEntryResult;
import in.bachatsetu.backend.audit.application.usecase.CreateAuditEntryUseCase;
import in.bachatsetu.backend.audit.application.usecase.GetAuditEntryUseCase;
import in.bachatsetu.backend.audit.application.usecase.SearchAuditUseCase;
import in.bachatsetu.backend.audit.domain.port.AuditPage;
import in.bachatsetu.backend.audit.interfaces.rest.dto.AuditEntryResponse;
import in.bachatsetu.backend.audit.interfaces.rest.dto.CreateAuditEntryRequest;
import in.bachatsetu.backend.audit.interfaces.rest.dto.PageResponse;
import in.bachatsetu.backend.audit.interfaces.rest.mapper.AuditApiMapper;
import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.auth.application.security.CurrentUserProvider;
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
import java.time.Instant;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes Audit use cases without leaking domain or persistence models. Authenticated and tenant-scoped:
 * every request is always scoped to the caller's own tenant, never a client-supplied one, so search never
 * leaks another tenant's entries.
 */
@RestController
@Validated
@RequestMapping(path = "/api/v1/audit", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Audit", description = "Generic, cross-module audit trail")
@ConditionalOnProperty(prefix = "bachatsetu.audit.rest", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AuditController {

    private static final String PROBLEM_CONTENT_TYPE = MediaType.APPLICATION_PROBLEM_JSON_VALUE;

    private final CreateAuditEntryUseCase createAuditEntry;
    private final GetAuditEntryUseCase getAuditEntry;
    private final SearchAuditUseCase searchAudit;
    private final CurrentUserProvider currentUserProvider;
    private final AuditApiMapper mapper;

    public AuditController(
            CreateAuditEntryUseCase createAuditEntry,
            GetAuditEntryUseCase getAuditEntry,
            SearchAuditUseCase searchAudit,
            CurrentUserProvider currentUserProvider,
            AuditApiMapper mapper) {
        this.createAuditEntry = createAuditEntry;
        this.getAuditEntry = getAuditEntry;
        this.searchAudit = searchAudit;
        this.currentUserProvider = currentUserProvider;
        this.mapper = mapper;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Record an audit entry", description = "Manually records one audit entry for the caller's tenant.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Audit entry recorded"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<AuditEntryResponse> create(@Valid @RequestBody CreateAuditEntryRequest request) {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        AuditEntryResult result = createAuditEntry.execute(mapper.toCreateCommand(request, currentUser));
        return ResponseEntity.created(URI.create("/api/v1/audit/" + result.auditId()))
                .body(mapper.toResponse(result));
    }

    @GetMapping("/{auditId}")
    @Operation(summary = "Get an audit entry", description = "Retrieves one tenant-scoped audit entry.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Audit entry returned"),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Audit entry not found", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public AuditEntryResponse get(@PathVariable String auditId) {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        AuditEntryResult result = getAuditEntry.execute(currentUser.tenantId(), mapper.toAuditId(auditId));
        return mapper.toResponse(result);
    }

    @GetMapping
    @Operation(
            summary = "Search audit entries",
            description = "Searches audit entries within the authenticated caller's tenant, page by page.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Page returned"),
        @ApiResponse(responseCode = "400", description = "Invalid filter, pagination, or sort parameters", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public PageResponse<AuditEntryResponse> search(
            @RequestParam(required = false) @Parameter(description = "Actor identifier filter") String actor,
            @RequestParam(required = false) @Parameter(description = "Module name filter") String module,
            @RequestParam(required = false) @Parameter(description = "Event type filter", example = "LOGIN") String event,
            @RequestParam(required = false) @Parameter(description = "Inclusive start of the date range") Instant dateFrom,
            @RequestParam(required = false) @Parameter(description = "Inclusive end of the date range") Instant dateTo,
            @RequestParam(defaultValue = "0") @Min(0) @Parameter(description = "Zero-based page index") int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) @Parameter(description = "Page size, up to 100") int size,
            @RequestParam(defaultValue = "createdAt") @Pattern(regexp = "createdAt")
            @Parameter(description = "Field to sort by") String sort,
            @RequestParam(defaultValue = "desc") @Pattern(regexp = "asc|desc")
            @Parameter(description = "Sort direction") String direction) {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        AuditPage<AuditEntryResult> result = searchAudit.execute(mapper.toSearchCriteria(
                currentUser, actor, module, event, dateFrom, dateTo, page, size, sort, direction));
        return mapper.toPageResponse(result);
    }
}
