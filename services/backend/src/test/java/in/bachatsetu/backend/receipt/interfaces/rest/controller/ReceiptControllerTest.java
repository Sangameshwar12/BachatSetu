package in.bachatsetu.backend.receipt.interfaces.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.auth.application.security.CurrentUserProvider;
import in.bachatsetu.backend.auth.application.security.CurrentUserUnavailableException;
import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.receipt.application.exception.ReceiptNotFoundException;
import in.bachatsetu.backend.receipt.application.query.ReceiptLineResult;
import in.bachatsetu.backend.receipt.application.query.ReceiptResult;
import in.bachatsetu.backend.receipt.application.query.ReceiptSummary;
import in.bachatsetu.backend.receipt.application.usecase.CreateReceiptUseCase;
import in.bachatsetu.backend.receipt.application.usecase.GetReceiptUseCase;
import in.bachatsetu.backend.receipt.application.usecase.ListReceiptsUseCase;
import in.bachatsetu.backend.receipt.domain.port.ReceiptPage;
import in.bachatsetu.backend.receipt.domain.port.ReceiptPageRequest;
import in.bachatsetu.backend.receipt.domain.port.ReceiptSortField;
import in.bachatsetu.backend.receipt.domain.port.SortDirection;
import in.bachatsetu.backend.receipt.interfaces.rest.exception.ReceiptExceptionHandler;
import in.bachatsetu.backend.receipt.interfaces.rest.mapper.ReceiptApiMapper;
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

@WebMvcTest(ReceiptController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({ReceiptApiMapper.class, ReceiptExceptionHandler.class})
class ReceiptControllerTest {

    private static final AggregateId TENANT_ID = AggregateId.newId();

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CreateReceiptUseCase createReceipt;

    @MockBean
    private GetReceiptUseCase getReceipt;

    @MockBean
    private ListReceiptsUseCase listReceipts;

    @MockBean
    private CurrentUserProvider currentUserProvider;

    @Test
    void createsReceipt() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        UUID receiptId = UUID.randomUUID();
        when(createReceipt.execute(any())).thenReturn(result(receiptId, "GENERATED"));

        mockMvc.perform(post("/api/v1/receipts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/receipts/" + receiptId))
                .andExpect(jsonPath("$.receiptId").value(receiptId.toString()))
                .andExpect(jsonPath("$.status").value("GENERATED"));
    }

    @Test
    void rejectsInvalidPaymentIdFormat() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());

        mockMvc.perform(post("/api/v1/receipts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"paymentId": "not-a-uuid", "memberId": "%s",
                                 "lines": [{"type": "CONTRIBUTION", "description": "Monthly", "amountPaise": 500000}]}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation-error"));
    }

    @Test
    void rejectsAnEmptyLineList() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());

        mockMvc.perform(post("/api/v1/receipts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"paymentId": "%s", "memberId": "%s", "lines": []}
                                """.formatted(UUID.randomUUID(), UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation-error"));
    }

    @Test
    void rejectsUnauthenticatedCreateRequest() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenThrow(new CurrentUserUnavailableException());

        mockMvc.perform(post("/api/v1/receipts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication-required"));
    }

    @Test
    void getsReceipt() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        UUID receiptId = UUID.randomUUID();
        when(getReceipt.execute(eq(TENANT_ID), any())).thenReturn(result(receiptId, "GENERATED"));

        mockMvc.perform(get("/api/v1/receipts/" + receiptId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receiptId").value(receiptId.toString()))
                .andExpect(jsonPath("$.status").value("GENERATED"));
    }

    @Test
    void reportsMissingReceiptAsNotFound() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(getReceipt.execute(eq(TENANT_ID), any()))
                .thenThrow(new ReceiptNotFoundException("receipt does not exist"));

        mockMvc.perform(get("/api/v1/receipts/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("receipt-not-found"));
    }

    @Test
    void rejectsUnauthenticatedGetRequest() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenThrow(new CurrentUserUnavailableException());

        mockMvc.perform(get("/api/v1/receipts/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication-required"));
    }

    @Test
    void listsReceiptsWithDefaultPagination() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        ReceiptPageRequest expectedRequest =
                new ReceiptPageRequest(0, 20, ReceiptSortField.CREATED_AT, SortDirection.ASC);
        when(listReceipts.execute(TENANT_ID, expectedRequest))
                .thenReturn(new ReceiptPage<>(List.of(summary(), summary()), 0, 20, 2));

        mockMvc.perform(get("/api/v1/receipts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.hasPrevious").value(false));
    }

    @Test
    void listsReceiptsWithExplicitPaginationAndSort() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        ReceiptPageRequest expectedRequest =
                new ReceiptPageRequest(1, 2, ReceiptSortField.AMOUNT, SortDirection.DESC);
        when(listReceipts.execute(TENANT_ID, expectedRequest))
                .thenReturn(new ReceiptPage<>(List.of(summary()), 1, 2, 3));

        mockMvc.perform(get("/api/v1/receipts")
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

        mockMvc.perform(get("/api/v1/receipts").param("sort", "unknown"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation-error"));

        mockMvc.perform(get("/api/v1/receipts").param("direction", "sideways"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation-error"));

        mockMvc.perform(get("/api/v1/receipts").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation-error"));

        mockMvc.perform(get("/api/v1/receipts").param("size", "500"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation-error"));
    }

    @Test
    void rejectsUnauthenticatedListRequest() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenThrow(new CurrentUserUnavailableException());

        mockMvc.perform(get("/api/v1/receipts"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication-required"));
    }

    private AuthenticatedUser authenticatedUser() {
        return new AuthenticatedUser(
                UserId.newId(),
                MobileNumber.of("+919876543210"),
                TENANT_ID,
                Set.of("GROUP_MEMBER"),
                Set.of("receipt.read"));
    }

    private ReceiptResult result(UUID receiptId, String status) {
        Instant now = Instant.parse("2026-07-07T08:00:00Z");
        return new ReceiptResult(
                receiptId,
                TENANT_ID.value(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "RCT/20260707/1A2B3C4D",
                List.of(new ReceiptLineResult(UUID.randomUUID(), "CONTRIBUTION", "Monthly contribution", 500_000L, "INR")),
                500_000L,
                "INR",
                status,
                null,
                now,
                now,
                0);
    }

    private ReceiptSummary summary() {
        return new ReceiptSummary(
                UUID.randomUUID(), "RCT/20260707/1A2B3C4D", 500_000L, "INR", "GENERATED",
                Instant.parse("2026-07-07T08:00:00Z"));
    }

    private String validRequestBody() {
        return """
                {
                  "paymentId": "%s",
                  "memberId": "%s",
                  "lines": [
                    {"type": "CONTRIBUTION", "description": "Monthly contribution", "amountPaise": 500000}
                  ]
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID());
    }
}
