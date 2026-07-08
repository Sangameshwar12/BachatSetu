package in.bachatsetu.backend.paymentgateway.interfaces.rest.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.paymentgateway.application.command.CreatePaymentOrderCommand;
import in.bachatsetu.backend.paymentgateway.application.command.InitiateRefundCommand;
import in.bachatsetu.backend.paymentgateway.application.command.ProcessWebhookCommand;
import in.bachatsetu.backend.paymentgateway.application.command.SyncPaymentStatusCommand;
import in.bachatsetu.backend.paymentgateway.application.query.PaymentOrderResult;
import in.bachatsetu.backend.paymentgateway.application.query.PaymentStatusResult;
import in.bachatsetu.backend.paymentgateway.application.query.RefundResult;
import in.bachatsetu.backend.paymentgateway.domain.model.GatewayType;
import in.bachatsetu.backend.paymentgateway.interfaces.rest.dto.CreatePaymentOrderRequest;
import in.bachatsetu.backend.paymentgateway.interfaces.rest.dto.GatewayWebhookPayload;
import in.bachatsetu.backend.paymentgateway.interfaces.rest.dto.PaymentOrderResponse;
import in.bachatsetu.backend.paymentgateway.interfaces.rest.dto.PaymentStatusResponse;
import in.bachatsetu.backend.paymentgateway.interfaces.rest.dto.RefundResponse;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Money;
import java.util.Currency;
import java.util.Objects;
import org.springframework.stereotype.Component;

/** Maps validated HTTP contracts to Payment Gateway application commands and safe responses. */
@Component
public class PaymentGatewayApiMapper {

    private final ObjectMapper objectMapper;

    public PaymentGatewayApiMapper(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "object mapper must not be null");
    }

    public CreatePaymentOrderCommand toCreateOrderCommand(
            String paymentId, CreatePaymentOrderRequest request, AuthenticatedUser currentUser) {
        Objects.requireNonNull(paymentId, "payment id must not be null");
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(currentUser, "current user must not be null");
        Money confirmedAmount = new Money(request.amountPaise(), Currency.getInstance(request.currencyCode()));
        return new CreatePaymentOrderCommand(
                currentUser.tenantId(), AggregateId.from(paymentId), confirmedAmount,
                currentUser.userId().toAggregateId());
    }

    public PaymentOrderResponse toOrderResponse(PaymentOrderResult result) {
        Objects.requireNonNull(result, "result must not be null");
        return new PaymentOrderResponse(
                result.paymentId().toString(), result.provider().name(), result.providerOrderId(),
                result.paymentLink());
    }

    public SyncPaymentStatusCommand toSyncCommand(String paymentId, AuthenticatedUser currentUser) {
        Objects.requireNonNull(paymentId, "payment id must not be null");
        Objects.requireNonNull(currentUser, "current user must not be null");
        return new SyncPaymentStatusCommand(
                currentUser.tenantId(), AggregateId.from(paymentId), currentUser.userId().toAggregateId());
    }

    public PaymentStatusResponse toStatusResponse(PaymentStatusResult result) {
        Objects.requireNonNull(result, "result must not be null");
        return new PaymentStatusResponse(
                result.provider().name(), result.providerOrderId(), result.providerStatus(),
                result.successful(), result.failed());
    }

    public InitiateRefundCommand toRefundCommand(String paymentId, AuthenticatedUser currentUser) {
        Objects.requireNonNull(paymentId, "payment id must not be null");
        Objects.requireNonNull(currentUser, "current user must not be null");
        return new InitiateRefundCommand(
                currentUser.tenantId(), AggregateId.from(paymentId), currentUser.userId().toAggregateId());
    }

    public RefundResponse toRefundResponse(RefundResult result) {
        Objects.requireNonNull(result, "result must not be null");
        return new RefundResponse(result.provider().name(), result.providerRefundId(), result.successful());
    }

    /**
     * @param provider the {@link GatewayType} constant name (for example {@code "RAZORPAY"}), passed as a
     *                 plain {@code String} rather than the enum itself so that the calling controller —
     *                 which may depend only on the application boundary, never on {@code ..domain..} — never
     *                 references {@link GatewayType} directly.
     */
    public ProcessWebhookCommand toWebhookCommand(String provider, String rawBody, String signatureHeader) {
        Objects.requireNonNull(provider, "provider must not be null");
        Objects.requireNonNull(rawBody, "raw body must not be null");
        String signature = signatureHeader == null ? "" : signatureHeader;
        GatewayWebhookPayload payload = parseWebhookPayload(rawBody);
        return new ProcessWebhookCommand(
                GatewayType.valueOf(provider), rawBody, signature, payload.providerOrderId(), payload.status(),
                payload.providerReferenceId());
    }

    private GatewayWebhookPayload parseWebhookPayload(String rawBody) {
        try {
            return objectMapper.readValue(rawBody, GatewayWebhookPayload.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("malformed webhook payload", exception);
        }
    }
}
