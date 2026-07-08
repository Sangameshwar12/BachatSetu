package in.bachatsetu.backend.paymentgateway.interfaces.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.paymentgateway.application.command.CreatePaymentOrderCommand;
import in.bachatsetu.backend.paymentgateway.application.command.ProcessWebhookCommand;
import in.bachatsetu.backend.paymentgateway.application.query.PaymentOrderResult;
import in.bachatsetu.backend.paymentgateway.application.query.PaymentStatusResult;
import in.bachatsetu.backend.paymentgateway.application.query.RefundResult;
import in.bachatsetu.backend.paymentgateway.domain.model.GatewayType;
import in.bachatsetu.backend.paymentgateway.interfaces.rest.dto.CreatePaymentOrderRequest;
import in.bachatsetu.backend.paymentgateway.interfaces.rest.dto.PaymentOrderResponse;
import in.bachatsetu.backend.paymentgateway.interfaces.rest.dto.PaymentStatusResponse;
import in.bachatsetu.backend.paymentgateway.interfaces.rest.dto.RefundResponse;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaymentGatewayApiMapperTest {

    private PaymentGatewayApiMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new PaymentGatewayApiMapper(new ObjectMapper());
    }

    @Test
    void mapsCreateOrderRequestToCommand() {
        AggregateId tenantId = AggregateId.newId();
        UserId userId = UserId.newId();
        AuthenticatedUser currentUser = new AuthenticatedUser(
                userId, MobileNumber.of("+919876543210"), tenantId, Set.of("GROUP_MEMBER"), Set.of("payment.write"));
        AggregateId paymentId = AggregateId.newId();
        CreatePaymentOrderRequest request = new CreatePaymentOrderRequest(500_000L, "INR");

        CreatePaymentOrderCommand command = mapper.toCreateOrderCommand(paymentId.toString(), request, currentUser);

        assertThat(command.tenantId()).isEqualTo(tenantId);
        assertThat(command.paymentId()).isEqualTo(paymentId);
        assertThat(command.confirmedAmount().minorUnits()).isEqualTo(500_000L);
        assertThat(command.actorId()).isEqualTo(userId.toAggregateId());
    }

    @Test
    void mapsOrderResultToResponse() {
        PaymentOrderResult result = new PaymentOrderResult(UUID.randomUUID(), GatewayType.RAZORPAY, "order_1", "link");

        PaymentOrderResponse response = mapper.toOrderResponse(result);

        assertThat(response.providerOrderId()).isEqualTo("order_1");
        assertThat(response.provider()).isEqualTo("RAZORPAY");
        assertThat(response.paymentLink()).isEqualTo("link");
    }

    @Test
    void mapsStatusResultToResponse() {
        PaymentStatusResult result = new PaymentStatusResult(
                UUID.randomUUID(), GatewayType.STRIPE, "pi_1", "succeeded", true, false);

        PaymentStatusResponse response = mapper.toStatusResponse(result);

        assertThat(response.provider()).isEqualTo("STRIPE");
        assertThat(response.successful()).isTrue();
    }

    @Test
    void mapsRefundResultToResponse() {
        RefundResult result = new RefundResult(UUID.randomUUID(), GatewayType.CASHFREE, "rfnd_1", true);

        RefundResponse response = mapper.toRefundResponse(result);

        assertThat(response.provider()).isEqualTo("CASHFREE");
        assertThat(response.providerRefundId()).isEqualTo("rfnd_1");
    }

    @Test
    void parsesAWellFormedWebhookBodyIntoACommand() {
        String body = "{\"providerOrderId\":\"order_1\",\"status\":\"SUCCESS\",\"providerReferenceId\":\"ref-1\"}";

        ProcessWebhookCommand command = mapper.toWebhookCommand("RAZORPAY", body, "sig-value");

        assertThat(command.provider()).isEqualTo(GatewayType.RAZORPAY);
        assertThat(command.rawPayload()).isEqualTo(body);
        assertThat(command.signatureHeader()).isEqualTo("sig-value");
        assertThat(command.providerOrderId()).isEqualTo("order_1");
        assertThat(command.status()).isEqualTo("SUCCESS");
        assertThat(command.providerReferenceId()).isEqualTo("ref-1");
    }

    @Test
    void treatsAMissingSignatureHeaderAsAnEmptyString() {
        String body = "{\"providerOrderId\":\"order_1\",\"status\":\"FAILED\",\"providerReferenceId\":\"ref-1\"}";

        ProcessWebhookCommand command = mapper.toWebhookCommand("RAZORPAY", body, null);

        assertThat(command.signatureHeader()).isEmpty();
    }

    @Test
    void rejectsAMalformedWebhookBody() {
        assertThatThrownBy(() -> mapper.toWebhookCommand("RAZORPAY", "not-json", "sig"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
