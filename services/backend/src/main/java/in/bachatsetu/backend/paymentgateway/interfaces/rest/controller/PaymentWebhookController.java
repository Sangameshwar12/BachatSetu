package in.bachatsetu.backend.paymentgateway.interfaces.rest.controller;

import in.bachatsetu.backend.paymentgateway.application.command.ProcessWebhookCommand;
import in.bachatsetu.backend.paymentgateway.application.query.PaymentStatusResult;
import in.bachatsetu.backend.paymentgateway.application.usecase.ProcessPaymentWebhookUseCase;
import in.bachatsetu.backend.paymentgateway.interfaces.rest.dto.PaymentStatusResponse;
import in.bachatsetu.backend.paymentgateway.interfaces.rest.mapper.PaymentGatewayApiMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public inbound webhook endpoints for the three supported providers. Listed under
 * {@code bachatsetu.authentication.security.public-endpoints} — providers never carry a bearer token, so
 * these paths are exempted from JWT authentication at the security-configuration level, not by adding a new
 * filter (see {@code docs/application/payment-gateway.md}). Every request still requires mandatory
 * signature verification, performed inside {@link ProcessPaymentWebhookUseCase} before anything else is
 * trusted.
 */
@RestController
@RequestMapping(path = "/api/v1/payments/webhooks", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Payment Gateway Webhooks", description = "Inbound provider webhook callbacks")
@ConditionalOnProperty(
        prefix = "bachatsetu.payment.gateway",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class PaymentWebhookController {

    private static final String PROBLEM_CONTENT_TYPE = MediaType.APPLICATION_PROBLEM_JSON_VALUE;

    private final ProcessPaymentWebhookUseCase processWebhook;
    private final PaymentGatewayApiMapper mapper;

    public PaymentWebhookController(ProcessPaymentWebhookUseCase processWebhook, PaymentGatewayApiMapper mapper) {
        this.processWebhook = processWebhook;
        this.mapper = mapper;
    }

    @PostMapping(path = "/razorpay", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Razorpay webhook callback")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Processed (idempotently, if a duplicate)"),
        @ApiResponse(responseCode = "401", description = "Invalid signature", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Unknown provider order", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public PaymentStatusResponse razorpay(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        return process("RAZORPAY", rawBody, signature);
    }

    @PostMapping(path = "/stripe", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Stripe webhook callback")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Processed (idempotently, if a duplicate)"),
        @ApiResponse(responseCode = "401", description = "Invalid signature", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Unknown provider order", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public PaymentStatusResponse stripe(
            @RequestBody String rawBody,
            @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        return process("STRIPE", rawBody, signature);
    }

    @PostMapping(path = "/cashfree", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Cashfree webhook callback")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Processed (idempotently, if a duplicate)"),
        @ApiResponse(responseCode = "401", description = "Invalid signature", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Unknown provider order", content = @Content(
                mediaType = PROBLEM_CONTENT_TYPE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public PaymentStatusResponse cashfree(
            @RequestBody String rawBody,
            @RequestHeader(value = "x-webhook-signature", required = false) String signature) {
        return process("CASHFREE", rawBody, signature);
    }

    private PaymentStatusResponse process(String provider, String rawBody, String signature) {
        ProcessWebhookCommand command = mapper.toWebhookCommand(provider, rawBody, signature);
        PaymentStatusResult result = processWebhook.execute(command);
        return mapper.toStatusResponse(result);
    }
}
