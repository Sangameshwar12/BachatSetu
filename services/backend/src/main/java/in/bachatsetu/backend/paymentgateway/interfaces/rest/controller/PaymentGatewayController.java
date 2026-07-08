package in.bachatsetu.backend.paymentgateway.interfaces.rest.controller;

import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.auth.application.security.CurrentUserProvider;
import in.bachatsetu.backend.paymentgateway.application.query.PaymentOrderResult;
import in.bachatsetu.backend.paymentgateway.application.query.PaymentStatusResult;
import in.bachatsetu.backend.paymentgateway.application.query.RefundResult;
import in.bachatsetu.backend.paymentgateway.application.usecase.CreatePaymentOrderUseCase;
import in.bachatsetu.backend.paymentgateway.application.usecase.InitiateRefundUseCase;
import in.bachatsetu.backend.paymentgateway.application.usecase.SyncPaymentStatusUseCase;
import in.bachatsetu.backend.paymentgateway.interfaces.rest.dto.CreatePaymentOrderRequest;
import in.bachatsetu.backend.paymentgateway.interfaces.rest.dto.PaymentOrderResponse;
import in.bachatsetu.backend.paymentgateway.interfaces.rest.dto.PaymentStatusResponse;
import in.bachatsetu.backend.paymentgateway.interfaces.rest.dto.RefundResponse;
import in.bachatsetu.backend.paymentgateway.interfaces.rest.mapper.PaymentGatewayApiMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authenticated Payment Gateway operations: creating a provider order, manually re-syncing a payment's
 * status, and initiating a refund. Webhook endpoints are separate and public — see
 * {@link PaymentWebhookController}.
 */
@RestController
@Validated
@RequestMapping(path = "/api/v1/payments", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Payment Gateway", description = "Provider order creation, status sync, and refunds")
@ConditionalOnProperty(
        prefix = "bachatsetu.payment.gateway",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class PaymentGatewayController {

    private static final String PROBLEM_CONTENT_TYPE = MediaType.APPLICATION_PROBLEM_JSON_VALUE;

    private final CreatePaymentOrderUseCase createOrder;
    private final SyncPaymentStatusUseCase syncStatus;
    private final InitiateRefundUseCase initiateRefund;
    private final CurrentUserProvider currentUserProvider;
    private final PaymentGatewayApiMapper mapper;

    public PaymentGatewayController(
            CreatePaymentOrderUseCase createOrder,
            SyncPaymentStatusUseCase syncStatus,
            InitiateRefundUseCase initiateRefund,
            CurrentUserProvider currentUserProvider,
            PaymentGatewayApiMapper mapper) {
        this.createOrder = createOrder;
        this.syncStatus = syncStatus;
        this.initiateRefund = initiateRefund;
        this.currentUserProvider = currentUserProvider;
        this.mapper = mapper;
    }

    @PostMapping(path = "/{paymentId}/gateway-orders", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Create a gateway order",
            description = "Creates a provider order for an existing payment, or returns the existing order "
                    + "if one was already created for it.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Order created (or already existed)"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Payment not found", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "422", description = "Amount mismatch", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<PaymentOrderResponse> createOrder(
            @PathVariable String paymentId, @Valid @RequestBody CreatePaymentOrderRequest request) {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        PaymentOrderResult result = createOrder.execute(mapper.toCreateOrderCommand(paymentId, request, currentUser));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toOrderResponse(result));
    }

    @PostMapping(path = "/{paymentId}/gateway-orders/sync")
    @Operation(
            summary = "Re-sync a payment's gateway status",
            description = "Pulls the payment's current status directly from its gateway, for when a webhook "
                    + "may have been missed.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Status synced"),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "No gateway order exists for this payment", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public PaymentStatusResponse syncStatus(@PathVariable String paymentId) {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        PaymentStatusResult result = syncStatus.execute(mapper.toSyncCommand(paymentId, currentUser));
        return mapper.toStatusResponse(result);
    }

    @PostMapping(path = "/{paymentId}/refunds")
    @Operation(
            summary = "Initiate a refund",
            description = "Initiates a full refund of a VERIFIED payment. Idempotent: repeating the call "
                    + "after a successful refund returns the same result without contacting the provider again.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Refund initiated (or already existed)"),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "No gateway order exists for this payment", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "422", description = "Payment is not VERIFIED", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public RefundResponse initiateRefund(@PathVariable String paymentId) {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        RefundResult result = initiateRefund.execute(mapper.toRefundCommand(paymentId, currentUser));
        return mapper.toRefundResponse(result);
    }
}
