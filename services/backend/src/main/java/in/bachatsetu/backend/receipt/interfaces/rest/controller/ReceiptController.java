package in.bachatsetu.backend.receipt.interfaces.rest.controller;

import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.auth.application.security.CurrentUserProvider;
import in.bachatsetu.backend.receipt.application.query.ReceiptResult;
import in.bachatsetu.backend.receipt.application.usecase.CreateReceiptUseCase;
import in.bachatsetu.backend.receipt.application.usecase.GetReceiptUseCase;
import in.bachatsetu.backend.receipt.application.usecase.ListReceiptsUseCase;
import in.bachatsetu.backend.receipt.interfaces.rest.dto.CreateReceiptRequest;
import in.bachatsetu.backend.receipt.interfaces.rest.dto.PageResponse;
import in.bachatsetu.backend.receipt.interfaces.rest.dto.ReceiptResponse;
import in.bachatsetu.backend.receipt.interfaces.rest.dto.ReceiptSummaryResponse;
import in.bachatsetu.backend.receipt.interfaces.rest.mapper.ReceiptApiMapper;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Exposes Receipt use cases without leaking domain or persistence models. */
@RestController
@Validated
@RequestMapping(path = "/api/v1/receipts", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Receipts", description = "Generate and retrieve payment receipts")
@ConditionalOnProperty(
        prefix = "bachatsetu.receipt.rest",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class ReceiptController {

    private static final String PROBLEM_CONTENT_TYPE = MediaType.APPLICATION_PROBLEM_JSON_VALUE;

    private final CreateReceiptUseCase createReceipt;
    private final GetReceiptUseCase getReceipt;
    private final ListReceiptsUseCase listReceipts;
    private final CurrentUserProvider currentUserProvider;
    private final ReceiptApiMapper mapper;

    public ReceiptController(
            CreateReceiptUseCase createReceipt,
            GetReceiptUseCase getReceipt,
            ListReceiptsUseCase listReceipts,
            CurrentUserProvider currentUserProvider,
            ReceiptApiMapper mapper) {
        this.createReceipt = createReceipt;
        this.getReceipt = getReceipt;
        this.listReceipts = listReceipts;
        this.currentUserProvider = currentUserProvider;
        this.mapper = mapper;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Generate a receipt",
            description = "Creates a new receipt, or returns the existing receipt for a repeated payment.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Receipt generated"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "422", description = "Business validation failed", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<ReceiptResponse> create(@Valid @RequestBody CreateReceiptRequest request) {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        ReceiptResult result = createReceipt.execute(mapper.toCreateCommand(request, currentUser));
        return ResponseEntity.created(URI.create("/api/v1/receipts/" + result.receiptId()))
                .body(mapper.toResponse(result));
    }

    @GetMapping("/{receiptId}")
    @Operation(summary = "Get a receipt", description = "Retrieves one tenant-scoped receipt.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Receipt returned"),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Receipt not found", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ReceiptResponse get(@PathVariable String receiptId) {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        ReceiptResult result = mapper.getReceipt(getReceipt, currentUser, receiptId);
        return mapper.toResponse(result);
    }

    @GetMapping
    @Operation(
            summary = "List receipts",
            description = "Lists receipts within the authenticated caller's tenant, page by page.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Page returned"),
        @ApiResponse(responseCode = "400", description = "Invalid pagination or sort parameters", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public PageResponse<ReceiptSummaryResponse> list(
            @RequestParam(defaultValue = "0")
            @Min(0)
            @Parameter(description = "Zero-based page index") int page,
            @RequestParam(defaultValue = "20")
            @Min(1)
            @Max(100)
            @Parameter(description = "Page size, up to 100") int size,
            @RequestParam(defaultValue = "createdAt")
            @Pattern(regexp = "amount|createdAt")
            @Parameter(description = "Field to sort by", example = "createdAt") String sort,
            @RequestParam(defaultValue = "asc")
            @Pattern(regexp = "asc|desc")
            @Parameter(description = "Sort direction", example = "asc") String direction) {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        return mapper.listReceipts(listReceipts, currentUser, page, size, sort, direction);
    }
}
