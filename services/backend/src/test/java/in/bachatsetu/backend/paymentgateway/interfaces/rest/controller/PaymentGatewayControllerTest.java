package in.bachatsetu.backend.paymentgateway.interfaces.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.auth.application.security.CurrentUserProvider;
import in.bachatsetu.backend.auth.application.security.CurrentUserUnavailableException;
import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.paymentgateway.application.exception.AmountMismatchException;
import in.bachatsetu.backend.paymentgateway.application.exception.RefundNotAllowedException;
import in.bachatsetu.backend.paymentgateway.application.query.PaymentOrderResult;
import in.bachatsetu.backend.paymentgateway.application.query.PaymentStatusResult;
import in.bachatsetu.backend.paymentgateway.application.query.RefundResult;
import in.bachatsetu.backend.paymentgateway.application.usecase.CreatePaymentOrderUseCase;
import in.bachatsetu.backend.paymentgateway.application.usecase.InitiateRefundUseCase;
import in.bachatsetu.backend.paymentgateway.application.usecase.SyncPaymentStatusUseCase;
import in.bachatsetu.backend.paymentgateway.domain.model.GatewayType;
import in.bachatsetu.backend.paymentgateway.interfaces.rest.exception.PaymentGatewayExceptionHandler;
import in.bachatsetu.backend.paymentgateway.interfaces.rest.mapper.PaymentGatewayApiMapper;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Set;
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
// refactor — PaymentGatewayController is itself gated on this property, so tests exercising its
// success paths must explicitly enable it, matching a real "gateway enabled" deployment.
@WebMvcTest(PaymentGatewayController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({PaymentGatewayApiMapper.class, PaymentGatewayExceptionHandler.class})
@TestPropertySource(properties = "bachatsetu.payment.gateway.enabled=true")
class PaymentGatewayControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CreatePaymentOrderUseCase createOrder;

    @MockBean
    private SyncPaymentStatusUseCase syncStatus;

    @MockBean
    private InitiateRefundUseCase initiateRefund;

    @MockBean
    private CurrentUserProvider currentUserProvider;

    @Test
    void createsAGatewayOrder() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        UUID paymentId = UUID.randomUUID();
        when(createOrder.execute(any())).thenReturn(
                new PaymentOrderResult(paymentId, GatewayType.RAZORPAY, "order_1", "https://pay.example/order_1"));

        mockMvc.perform(post("/api/v1/payments/" + paymentId + "/gateway-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amountPaise": 500000, "currencyCode": "INR"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.providerOrderId").value("order_1"))
                .andExpect(jsonPath("$.provider").value("RAZORPAY"));
    }

    @Test
    void reportsAnAmountMismatchAsUnprocessable() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(createOrder.execute(any())).thenThrow(new AmountMismatchException("amount mismatch"));

        mockMvc.perform(post("/api/v1/payments/" + UUID.randomUUID() + "/gateway-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amountPaise": 999000, "currencyCode": "INR"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("payment-gateway-validation-failed"));
    }

    @Test
    void rejectsUnauthenticatedOrderCreation() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenThrow(new CurrentUserUnavailableException());

        mockMvc.perform(post("/api/v1/payments/" + UUID.randomUUID() + "/gateway-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amountPaise": 500000, "currencyCode": "INR"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication-required"));
    }

    @Test
    void syncsPaymentStatus() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        UUID paymentId = UUID.randomUUID();
        when(syncStatus.execute(any())).thenReturn(new PaymentStatusResult(
                paymentId, GatewayType.RAZORPAY, "order_1", "captured", true, false));

        mockMvc.perform(post("/api/v1/payments/" + paymentId + "/gateway-orders/sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.successful").value(true));
    }

    @Test
    void initiatesARefund() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        UUID paymentId = UUID.randomUUID();
        when(initiateRefund.execute(any())).thenReturn(
                new RefundResult(paymentId, GatewayType.RAZORPAY, "rfnd_1", true));

        mockMvc.perform(post("/api/v1/payments/" + paymentId + "/refunds"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providerRefundId").value("rfnd_1"));
    }

    @Test
    void reportsARefundNotAllowedAsUnprocessable() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(initiateRefund.execute(any())).thenThrow(new RefundNotAllowedException("not verified"));

        mockMvc.perform(post("/api/v1/payments/" + UUID.randomUUID() + "/refunds"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("payment-gateway-validation-failed"));
    }

    private AuthenticatedUser authenticatedUser() {
        return new AuthenticatedUser(
                UserId.newId(), MobileNumber.of("+919876543210"), AggregateId.newId(),
                Set.of("GROUP_MEMBER"), Set.of("payment.write"));
    }
}
