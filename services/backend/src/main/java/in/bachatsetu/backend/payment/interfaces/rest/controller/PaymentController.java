package in.bachatsetu.backend.payment.interfaces.rest.controller;

import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.auth.application.security.CurrentUserProvider;
import in.bachatsetu.backend.payment.application.query.PaymentResult;
import in.bachatsetu.backend.payment.application.usecase.CreatePaymentUseCase;
import in.bachatsetu.backend.payment.application.usecase.GetPaymentUseCase;
import in.bachatsetu.backend.payment.application.usecase.ListPaymentsUseCase;
import in.bachatsetu.backend.payment.application.usecase.UpdatePaymentStatusUseCase;
import in.bachatsetu.backend.payment.interfaces.rest.dto.CreatePaymentRequest;
import in.bachatsetu.backend.payment.interfaces.rest.dto.PageResponse;
import in.bachatsetu.backend.payment.interfaces.rest.dto.PaymentResponse;
import in.bachatsetu.backend.payment.interfaces.rest.dto.PaymentSummaryResponse;
import in.bachatsetu.backend.payment.interfaces.rest.dto.UpdatePaymentStatusRequest;
import in.bachatsetu.backend.payment.interfaces.rest.mapper.PaymentApiMapper;
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

/** Exposes Payment use cases without leaking domain or persistence models. */
@RestController
@Validated
@RequestMapping(path = "/api/v1/payments", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Payments", description = "Initiate and manage community payments")
@ConditionalOnProperty(
        prefix = "bachatsetu.payment.rest",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class PaymentController {

    private static final String PROBLEM_CONTENT_TYPE = MediaType.APPLICATION_PROBLEM_JSON_VALUE;

    private final CreatePaymentUseCase createPayment;
    private final GetPaymentUseCase getPayment;
    private final ListPaymentsUseCase listPayments;
    private final UpdatePaymentStatusUseCase updatePaymentStatus;
    private final CurrentUserProvider currentUserProvider;
    private final PaymentApiMapper mapper;

    public PaymentController(
            CreatePaymentUseCase createPayment,
            GetPaymentUseCase getPayment,
            ListPaymentsUseCase listPayments,
            UpdatePaymentStatusUseCase updatePaymentStatus,
            CurrentUserProvider currentUserProvider,
            PaymentApiMapper mapper) {
        this.createPayment = createPayment;
        this.getPayment = getPayment;
        this.listPayments = listPayments;
        this.updatePaymentStatus = updatePaymentStatus;
        this.currentUserProvider = currentUserProvider;
        this.mapper = mapper;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Initiate a payment",
            description = "Creates a new payment, or returns the existing payment for a repeated idempotency key.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Payment created"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "422", description = "Business validation failed", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<PaymentResponse> create(@Valid @RequestBody CreatePaymentRequest request) {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        PaymentResult result = createPayment.execute(mapper.toCreateCommand(request, currentUser));
        return ResponseEntity.created(URI.create("/api/v1/payments/" + result.paymentId()))
                .body(mapper.toResponse(result));
    }

    @GetMapping("/{paymentId}")
    @Operation(summary = "Get a payment", description = "Retrieves one tenant-scoped payment.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Payment returned"),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Payment not found", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public PaymentResponse get(@PathVariable String paymentId) {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        PaymentResult result = mapper.getPayment(getPayment, currentUser, paymentId);
        return mapper.toResponse(result);
    }

    @GetMapping
    @Operation(
            summary = "List payments",
            description = "Lists payments within the authenticated caller's tenant, page by page.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Page returned"),
        @ApiResponse(responseCode = "400", description = "Invalid pagination or sort parameters", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public PageResponse<PaymentSummaryResponse> list(
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
        return mapper.listPayments(listPayments, currentUser, page, size, sort, direction);
    }

    @PatchMapping(path = "/{paymentId}/status", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update a payment status", description = "Transitions a payment to a new lifecycle status.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Payment updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Payment not found", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "422", description = "Invalid lifecycle transition", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public PaymentResponse updateStatus(
            @PathVariable String paymentId,
            @Valid @RequestBody UpdatePaymentStatusRequest request) {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        PaymentResult result =
                updatePaymentStatus.execute(mapper.toUpdateStatusCommand(paymentId, request, currentUser));
        return mapper.toResponse(result);
    }
}
