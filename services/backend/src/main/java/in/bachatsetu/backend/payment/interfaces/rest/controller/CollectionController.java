package in.bachatsetu.backend.payment.interfaces.rest.controller;

import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.auth.application.security.CurrentUserProvider;
import in.bachatsetu.backend.payment.application.query.CollectionSummaryResult;
import in.bachatsetu.backend.payment.application.usecase.GetCollectionSummaryUseCase;
import in.bachatsetu.backend.payment.application.usecase.RecordManualPaymentUseCase;
import in.bachatsetu.backend.payment.interfaces.rest.dto.CollectionSummaryResponse;
import in.bachatsetu.backend.payment.interfaces.rest.mapper.CollectionApiMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Exposes a group's contribution collection status and organizer-recorded manual payments. */
@RestController
@RequestMapping(path = "/api/v1/groups/{groupId}/collection", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Collection", description = "Track and record group contribution collection")
@ConditionalOnProperty(
        prefix = "bachatsetu.payment.rest",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class CollectionController {

    private static final String PROBLEM_CONTENT_TYPE = MediaType.APPLICATION_PROBLEM_JSON_VALUE;

    private final GetCollectionSummaryUseCase getCollectionSummary;
    private final RecordManualPaymentUseCase recordManualPayment;
    private final CurrentUserProvider currentUserProvider;
    private final CollectionApiMapper mapper;

    public CollectionController(
            GetCollectionSummaryUseCase getCollectionSummary,
            RecordManualPaymentUseCase recordManualPayment,
            CurrentUserProvider currentUserProvider,
            CollectionApiMapper mapper) {
        this.getCollectionSummary = getCollectionSummary;
        this.recordManualPayment = recordManualPayment;
        this.currentUserProvider = currentUserProvider;
        this.mapper = mapper;
    }

    @GetMapping
    @Operation(
            summary = "Get collection summary",
            description = "Returns the group's current-cycle contribution collection status, derived from its "
                    + "own contribution schedule.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Summary returned"),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Group not found", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public CollectionSummaryResponse getSummary(@PathVariable String groupId) {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        CollectionSummaryResult result = mapper.getSummary(getCollectionSummary, currentUser, groupId);
        return mapper.toResponse(result);
    }

    @PostMapping("/members/{memberId}/mark-paid")
    @Operation(
            summary = "Record a manual payment",
            description = "Organizer-only. Records a member's contribution for the current cycle as collected "
                    + "in cash.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Payment recorded"),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "403", description = "Only the group owner may record a payment", content =
                @Content(mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Group not found", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "409", description = "Member has already paid this cycle", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "422", description = "No active contribution cycle", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Void> markPaid(@PathVariable String groupId, @PathVariable String memberId) {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        recordManualPayment.execute(mapper.toMarkPaidCommand(groupId, memberId, currentUser));
        return ResponseEntity.noContent().build();
    }
}
