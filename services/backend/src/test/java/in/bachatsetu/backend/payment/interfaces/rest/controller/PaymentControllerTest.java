package in.bachatsetu.backend.payment.interfaces.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.auth.application.security.CurrentUserProvider;
import in.bachatsetu.backend.auth.application.security.CurrentUserUnavailableException;
import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.payment.application.exception.PaymentNotFoundException;
import in.bachatsetu.backend.payment.application.query.PaymentAttemptResult;
import in.bachatsetu.backend.payment.application.query.PaymentResult;
import in.bachatsetu.backend.payment.application.query.PaymentSummary;
import in.bachatsetu.backend.payment.application.usecase.CreatePaymentUseCase;
import in.bachatsetu.backend.payment.application.usecase.GetPaymentUseCase;
import in.bachatsetu.backend.payment.application.usecase.ListPaymentsUseCase;
import in.bachatsetu.backend.payment.application.usecase.UpdatePaymentStatusUseCase;
import in.bachatsetu.backend.payment.domain.exception.InvalidPaymentStateException;
import in.bachatsetu.backend.payment.domain.port.PaymentPage;
import in.bachatsetu.backend.payment.domain.port.PaymentPageRequest;
import in.bachatsetu.backend.payment.domain.port.PaymentSortField;
import in.bachatsetu.backend.payment.domain.port.SortDirection;
import in.bachatsetu.backend.payment.interfaces.rest.exception.PaymentExceptionHandler;
import in.bachatsetu.backend.payment.interfaces.rest.mapper.PaymentApiMapper;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({PaymentApiMapper.class, PaymentExceptionHandler.class})
class PaymentControllerTest {

    private static final AggregateId TENANT_ID = AggregateId.newId();

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CreatePaymentUseCase createPayment;

    @MockBean
    private GetPaymentUseCase getPayment;

    @MockBean
    private ListPaymentsUseCase listPayments;

    @MockBean
    private UpdatePaymentStatusUseCase updatePaymentStatus;

    @MockBean
    private CurrentUserProvider currentUserProvider;

    @Test
    void createsPayment() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        UUID paymentId = UUID.randomUUID();
        when(createPayment.execute(any())).thenReturn(result(paymentId, "INITIATED"));

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/payments/" + paymentId))
                .andExpect(jsonPath("$.paymentId").value(paymentId.toString()))
                .andExpect(jsonPath("$.status").value("INITIATED"));
    }

    @Test
    void rejectsInvalidGroupIdFormat() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"groupId": "not-a-uuid", "memberId": "%s", "idempotencyKey": "checkout-attempt-0001",
                                 "amountPaise": 500000, "method": "UPI"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation-error"));
    }

    @Test
    void rejectsUnauthenticatedCreateRequest() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenThrow(new CurrentUserUnavailableException());

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication-required"));
    }

    @Test
    void getsPayment() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        UUID paymentId = UUID.randomUUID();
        when(getPayment.execute(eq(TENANT_ID), any())).thenReturn(result(paymentId, "VERIFIED"));

        mockMvc.perform(get("/api/v1/payments/" + paymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(paymentId.toString()))
                .andExpect(jsonPath("$.status").value("VERIFIED"));
    }

    @Test
    void reportsMissingPaymentAsNotFound() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(getPayment.execute(eq(TENANT_ID), any()))
                .thenThrow(new PaymentNotFoundException("payment does not exist"));

        mockMvc.perform(get("/api/v1/payments/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("payment-not-found"));
    }

    @Test
    void rejectsUnauthenticatedGetRequest() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenThrow(new CurrentUserUnavailableException());

        mockMvc.perform(get("/api/v1/payments/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication-required"));
    }

    @Test
    void listsPaymentsWithDefaultPagination() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        PaymentPageRequest expectedRequest = new PaymentPageRequest(0, 20, PaymentSortField.CREATED_AT, SortDirection.ASC);
        when(listPayments.execute(TENANT_ID, expectedRequest))
                .thenReturn(new PaymentPage<>(List.of(summary(), summary()), 0, 20, 2));

        mockMvc.perform(get("/api/v1/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.hasPrevious").value(false));
    }

    @Test
    void listsPaymentsWithExplicitPaginationAndSort() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        PaymentPageRequest expectedRequest = new PaymentPageRequest(1, 2, PaymentSortField.AMOUNT, SortDirection.DESC);
        when(listPayments.execute(TENANT_ID, expectedRequest))
                .thenReturn(new PaymentPage<>(List.of(summary()), 1, 2, 3));

        mockMvc.perform(get("/api/v1/payments")
                        .param("page", "1")
                        .param("size", "2")
                        .param("sort", "amount")
                        .param("direction", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.hasPrevious").value(true));
    }

    @Test
    void rejectsInvalidSortAndPaginationParameters() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());

        mockMvc.perform(get("/api/v1/payments").param("sort", "unknown"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation-error"));

        mockMvc.perform(get("/api/v1/payments").param("direction", "sideways"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation-error"));

        mockMvc.perform(get("/api/v1/payments").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation-error"));

        mockMvc.perform(get("/api/v1/payments").param("size", "500"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation-error"));
    }

    @Test
    void rejectsUnauthenticatedListRequest() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenThrow(new CurrentUserUnavailableException());

        mockMvc.perform(get("/api/v1/payments"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication-required"));
    }

    @Test
    void updatesPaymentStatusToVerified() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        UUID paymentId = UUID.randomUUID();
        when(updatePaymentStatus.execute(any())).thenReturn(result(paymentId, "VERIFIED"));

        mockMvc.perform(patch("/api/v1/payments/" + paymentId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "VERIFIED", "provider": "test-provider", "transactionId": "txn-001"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VERIFIED"));
    }

    @Test
    void rejectsInvalidStatusValue() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());

        mockMvc.perform(patch("/api/v1/payments/" + UUID.randomUUID() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "CANCELLED"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation-error"));
    }

    @Test
    void reportsMissingProviderReferenceAsBadRequest() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());

        mockMvc.perform(patch("/api/v1/payments/" + UUID.randomUUID() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "VERIFIED"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid-request"));
    }

    @Test
    void reportsInvalidStatusTransitionAsUnprocessable() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(updatePaymentStatus.execute(any()))
                .thenThrow(new InvalidPaymentStateException("payment state does not permit this operation"));

        mockMvc.perform(patch("/api/v1/payments/" + UUID.randomUUID() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "FAILED", "failureCode": "provider-declined"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("payment-validation-failed"));
    }

    @Test
    void reportsMissingPaymentOnUpdateAsNotFound() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(updatePaymentStatus.execute(any()))
                .thenThrow(new PaymentNotFoundException("payment does not exist"));

        mockMvc.perform(patch("/api/v1/payments/" + UUID.randomUUID() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "FAILED", "failureCode": "provider-declined"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("payment-not-found"));
    }

    @Test
    void rejectsUnauthenticatedUpdateRequest() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenThrow(new CurrentUserUnavailableException());

        mockMvc.perform(patch("/api/v1/payments/" + UUID.randomUUID() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "PENDING_PROVIDER"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication-required"));
    }

    private AuthenticatedUser authenticatedUser() {
        return new AuthenticatedUser(
                UserId.newId(),
                MobileNumber.of("+919876543210"),
                TENANT_ID,
                Set.of("GROUP_MEMBER"),
                Set.of("payment.read"));
    }

    private PaymentResult result(UUID paymentId, String status) {
        Instant now = Instant.parse("2026-07-06T08:00:00Z");
        return new PaymentResult(
                paymentId,
                TENANT_ID.value(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "PAY-1A2B3C4D5E6F7A8B",
                500_000L,
                "INR",
                "UPI",
                status,
                "NOT_REQUIRED",
                List.of(new PaymentAttemptResult(UUID.randomUUID(), 1, now, "CREATED", null, null, null)),
                now,
                now,
                0);
    }

    private PaymentSummary summary() {
        return new PaymentSummary(
                UUID.randomUUID(), "PAY-1A2B3C4D5E6F7A8B", 500_000L, "INR", "UPI", "INITIATED",
                Instant.parse("2026-07-06T08:00:00Z"));
    }

    private String validRequestBody() {
        return """
                {
                  "groupId": "%s",
                  "memberId": "%s",
                  "idempotencyKey": "checkout-attempt-0001",
                  "amountPaise": 500000,
                  "method": "UPI"
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID());
    }
}
