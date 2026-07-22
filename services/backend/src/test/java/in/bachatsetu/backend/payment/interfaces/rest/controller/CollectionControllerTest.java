package in.bachatsetu.backend.payment.interfaces.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.auth.application.security.CurrentUserProvider;
import in.bachatsetu.backend.auth.application.security.CurrentUserUnavailableException;
import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.payment.application.exception.CollectionAccessDeniedException;
import in.bachatsetu.backend.payment.application.exception.CollectionGroupNotFoundException;
import in.bachatsetu.backend.payment.application.exception.MemberAlreadyPaidException;
import in.bachatsetu.backend.payment.application.exception.NoActiveCollectionCycleException;
import in.bachatsetu.backend.payment.application.query.CollectionSummaryResult;
import in.bachatsetu.backend.payment.application.query.MemberCollectionResult;
import in.bachatsetu.backend.payment.application.usecase.GetCollectionSummaryUseCase;
import in.bachatsetu.backend.payment.application.usecase.RecordManualPaymentUseCase;
import in.bachatsetu.backend.payment.domain.exception.PaymentConflictException;
import in.bachatsetu.backend.payment.interfaces.rest.exception.PaymentExceptionHandler;
import in.bachatsetu.backend.payment.interfaces.rest.mapper.CollectionApiMapper;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CollectionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({CollectionApiMapper.class, PaymentExceptionHandler.class})
class CollectionControllerTest {

    private static final AggregateId TENANT_ID = AggregateId.newId();

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GetCollectionSummaryUseCase getCollectionSummary;

    @MockBean
    private RecordManualPaymentUseCase recordManualPayment;

    @MockBean
    private CurrentUserProvider currentUserProvider;

    @Test
    void getsCollectionSummary() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        UUID groupId = UUID.randomUUID();
        when(getCollectionSummary.execute(eq(TENANT_ID), any())).thenReturn(summary(groupId));

        mockMvc.perform(get("/api/v1/groups/" + groupId + "/collection"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupId").value(groupId.toString()))
                .andExpect(jsonPath("$.cycleActive").value(true))
                .andExpect(jsonPath("$.members.length()").value(1));
    }

    @Test
    void reportsMissingGroupAsNotFound() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        when(getCollectionSummary.execute(eq(TENANT_ID), any()))
                .thenThrow(new CollectionGroupNotFoundException("savings group does not exist"));

        mockMvc.perform(get("/api/v1/groups/" + UUID.randomUUID() + "/collection"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("group-not-found"));
    }

    @Test
    void rejectsUnauthenticatedGetRequest() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenThrow(new CurrentUserUnavailableException());

        mockMvc.perform(get("/api/v1/groups/" + UUID.randomUUID() + "/collection"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication-required"));
    }

    @Test
    void marksAMemberAsPaid() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());

        mockMvc.perform(post("/api/v1/groups/" + UUID.randomUUID() + "/collection/members/"
                        + UUID.randomUUID() + "/mark-paid"))
                .andExpect(status().isNoContent());

        verify(recordManualPayment).execute(any());
    }

    @Test
    void reportsNonOwnerMarkPaidAsForbidden() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        org.mockito.Mockito.doThrow(new CollectionAccessDeniedException("only the group owner may perform this operation"))
                .when(recordManualPayment).execute(any());

        mockMvc.perform(post("/api/v1/groups/" + UUID.randomUUID() + "/collection/members/"
                        + UUID.randomUUID() + "/mark-paid"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("access-denied"));
    }

    @Test
    void reportsAlreadyPaidMemberAsConflict() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        org.mockito.Mockito.doThrow(new MemberAlreadyPaidException("member has already paid for the current cycle"))
                .when(recordManualPayment).execute(any());

        mockMvc.perform(post("/api/v1/groups/" + UUID.randomUUID() + "/collection/members/"
                        + UUID.randomUUID() + "/mark-paid"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("member-already-paid"));
    }

    @Test
    void reportsAConcurrentMarkPaidRaceAsConflictNotServerError() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        org.mockito.Mockito.doThrow(new PaymentConflictException(
                        "a conflicting payment write already completed", new RuntimeException("cause")))
                .when(recordManualPayment).execute(any());

        mockMvc.perform(post("/api/v1/groups/" + UUID.randomUUID() + "/collection/members/"
                        + UUID.randomUUID() + "/mark-paid"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("concurrent-request-conflict"));
    }

    @Test
    void reportsNoActiveCycleAsUnprocessable() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(authenticatedUser());
        org.mockito.Mockito.doThrow(new NoActiveCollectionCycleException("group has no active contribution cycle right now"))
                .when(recordManualPayment).execute(any());

        mockMvc.perform(post("/api/v1/groups/" + UUID.randomUUID() + "/collection/members/"
                        + UUID.randomUUID() + "/mark-paid"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("no-active-collection-cycle"));
    }

    @Test
    void rejectsUnauthenticatedMarkPaidRequest() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenThrow(new CurrentUserUnavailableException());

        mockMvc.perform(post("/api/v1/groups/" + UUID.randomUUID() + "/collection/members/"
                        + UUID.randomUUID() + "/mark-paid"))
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

    private CollectionSummaryResult summary(UUID groupId) {
        LocalDate today = LocalDate.of(2026, 8, 1);
        Instant now = Instant.parse("2026-08-01T00:00:00Z");
        return new CollectionSummaryResult(
                groupId, true, 1, today, today.plusMonths(1), today, 100_000, "INR", 1, 1, 0, 0,
                100_000, 100_000, 0,
                List.of(new MemberCollectionResult(
                        UUID.randomUUID(), "QA Tester", "PAID", 100_000, 100_000, now, today)));
    }
}
