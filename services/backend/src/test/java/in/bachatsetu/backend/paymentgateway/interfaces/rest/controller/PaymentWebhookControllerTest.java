package in.bachatsetu.backend.paymentgateway.interfaces.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import in.bachatsetu.backend.paymentgateway.application.exception.GatewayOrderNotFoundException;
import in.bachatsetu.backend.paymentgateway.application.exception.InvalidWebhookSignatureException;
import in.bachatsetu.backend.paymentgateway.application.query.PaymentStatusResult;
import in.bachatsetu.backend.paymentgateway.application.usecase.ProcessPaymentWebhookUseCase;
import in.bachatsetu.backend.paymentgateway.domain.model.GatewayType;
import in.bachatsetu.backend.paymentgateway.interfaces.rest.exception.PaymentGatewayExceptionHandler;
import in.bachatsetu.backend.paymentgateway.interfaces.rest.mapper.PaymentGatewayApiMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

// bachatsetu.payment.gateway.enabled defaults to false (MVP mode) as of the deployment-mode
// refactor — PaymentWebhookController is itself gated on this property, so tests exercising its
// success paths must explicitly enable it, matching a real "gateway enabled" deployment.
@WebMvcTest(PaymentWebhookController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({PaymentGatewayApiMapper.class, PaymentGatewayExceptionHandler.class})
@TestPropertySource(properties = "bachatsetu.payment.gateway.enabled=true")
class PaymentWebhookControllerTest {

    private static final String BODY = """
            {"providerOrderId": "order_1", "status": "SUCCESS", "providerReferenceId": "ref-1"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProcessPaymentWebhookUseCase processWebhook;

    @Test
    void processesAValidRazorpayWebhook() throws Exception {
        when(processWebhook.execute(any())).thenReturn(new PaymentStatusResult(
                UUID.randomUUID(), GatewayType.RAZORPAY, "order_1", "captured", true, false));

        mockMvc.perform(post("/api/v1/payments/webhooks/razorpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", "sig")
                        .content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.successful").value(true));
    }

    @Test
    void processesAValidStripeWebhook() throws Exception {
        when(processWebhook.execute(any())).thenReturn(new PaymentStatusResult(
                UUID.randomUUID(), GatewayType.STRIPE, "order_1", "succeeded", true, false));

        mockMvc.perform(post("/api/v1/payments/webhooks/stripe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", "sig")
                        .content(BODY))
                .andExpect(status().isOk());
    }

    @Test
    void processesAValidCashfreeWebhook() throws Exception {
        when(processWebhook.execute(any())).thenReturn(new PaymentStatusResult(
                UUID.randomUUID(), GatewayType.CASHFREE, "order_1", "SUCCESS", true, false));

        mockMvc.perform(post("/api/v1/payments/webhooks/cashfree")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("x-webhook-signature", "sig")
                        .content(BODY))
                .andExpect(status().isOk());
    }

    @Test
    void returnsTheSameSuccessResultForADuplicateWebhook() throws Exception {
        when(processWebhook.execute(any())).thenReturn(new PaymentStatusResult(
                UUID.randomUUID(), GatewayType.RAZORPAY, "order_1", "captured", true, false));

        mockMvc.perform(post("/api/v1/payments/webhooks/razorpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", "sig")
                        .content(BODY))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/payments/webhooks/razorpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", "sig")
                        .content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.successful").value(true));
    }

    @Test
    void rejectsAnInvalidSignatureAsUnauthorized() throws Exception {
        when(processWebhook.execute(any())).thenThrow(new InvalidWebhookSignatureException("bad signature"));

        mockMvc.perform(post("/api/v1/payments/webhooks/razorpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", "bad-sig")
                        .content(BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("invalid-webhook-signature"));
    }

    @Test
    void rejectsAnUnknownProviderOrderAsNotFound() throws Exception {
        when(processWebhook.execute(any())).thenThrow(new GatewayOrderNotFoundException("unknown order"));

        mockMvc.perform(post("/api/v1/payments/webhooks/razorpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", "sig")
                        .content(BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not-found"));
    }
}
