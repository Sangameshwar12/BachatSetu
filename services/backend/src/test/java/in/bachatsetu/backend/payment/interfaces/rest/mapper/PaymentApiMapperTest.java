package in.bachatsetu.backend.payment.interfaces.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.payment.application.command.CreatePaymentCommand;
import in.bachatsetu.backend.payment.application.command.UpdatePaymentStatusCommand;
import in.bachatsetu.backend.payment.application.query.PaymentAttemptResult;
import in.bachatsetu.backend.payment.application.query.PaymentResult;
import in.bachatsetu.backend.payment.application.query.PaymentSummary;
import in.bachatsetu.backend.payment.application.usecase.GetPaymentUseCase;
import in.bachatsetu.backend.payment.application.usecase.ListPaymentsUseCase;
import in.bachatsetu.backend.payment.domain.port.PaymentPage;
import in.bachatsetu.backend.payment.domain.port.PaymentPageRequest;
import in.bachatsetu.backend.payment.domain.port.PaymentSortField;
import in.bachatsetu.backend.payment.domain.port.SortDirection;
import in.bachatsetu.backend.payment.interfaces.rest.dto.CreatePaymentRequest;
import in.bachatsetu.backend.payment.interfaces.rest.dto.PageResponse;
import in.bachatsetu.backend.payment.interfaces.rest.dto.PaymentResponse;
import in.bachatsetu.backend.payment.interfaces.rest.dto.PaymentSummaryResponse;
import in.bachatsetu.backend.payment.interfaces.rest.dto.UpdatePaymentStatusRequest;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PaymentApiMapperTest {

    private final PaymentApiMapper mapper = new PaymentApiMapper();

    @Test
    void mapsCreateRequestToCommandUsingAuthenticatedIdentity() {
        AuthenticatedUser currentUser = authenticatedUser();
        UUID groupId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        CreatePaymentRequest request = new CreatePaymentRequest(
                groupId.toString(), memberId.toString(), "checkout-attempt-0001", 500_000L, "UPI");

        CreatePaymentCommand command = mapper.toCreateCommand(request, currentUser);

        assertThat(command.tenantId()).isEqualTo(currentUser.tenantId());
        assertThat(command.groupId().value()).isEqualTo(groupId);
        assertThat(command.memberId().value()).isEqualTo(memberId);
        assertThat(command.idempotencyKey().value()).isEqualTo("checkout-attempt-0001");
        assertThat(command.amount().minorUnits()).isEqualTo(500_000L);
        assertThat(command.method().name()).isEqualTo("UPI");
        assertThat(command.actorId()).isEqualTo(currentUser.userId().toAggregateId());
    }

    @Test
    void getPaymentDelegatesToUseCaseWithParsedIdentifiers() {
        AuthenticatedUser currentUser = authenticatedUser();
        UUID paymentId = UUID.randomUUID();
        PaymentResult expected = result(paymentId, "INITIATED");
        GetPaymentUseCase useCase = (tenantId, id) -> {
            assertThat(tenantId).isEqualTo(currentUser.tenantId());
            assertThat(id.value()).isEqualTo(paymentId);
            return expected;
        };

        assertThat(mapper.getPayment(useCase, currentUser, paymentId.toString())).isEqualTo(expected);
    }

    @Test
    void mapsResultToResponse() {
        UUID paymentId = UUID.randomUUID();
        PaymentResult result = result(paymentId, "VERIFIED");

        PaymentResponse response = mapper.toResponse(result);

        assertThat(response.paymentId()).isEqualTo(paymentId.toString());
        assertThat(response.status()).isEqualTo("VERIFIED");
        assertThat(response.attempts()).singleElement()
                .satisfies(attempt -> assertThat(attempt.status()).isEqualTo("SUCCEEDED"));
    }

    @Test
    void buildsPageRequestFromValidatedRestParameters() {
        PaymentPageRequest pageRequest = mapper.toPageRequest(1, 10, "amount", "desc");

        assertThat(pageRequest.page()).isEqualTo(1);
        assertThat(pageRequest.size()).isEqualTo(10);
        assertThat(pageRequest.sortField()).isEqualTo(PaymentSortField.AMOUNT);
        assertThat(pageRequest.direction()).isEqualTo(SortDirection.DESC);
    }

    @Test
    void rejectsUnsupportedSortOrDirectionValues() {
        assertThatThrownBy(() -> mapper.toPageRequest(0, 20, "unsupported", "asc"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> mapper.toPageRequest(0, 20, "amount", "sideways"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void listPaymentsDelegatesToUseCaseWithTenantIdentityAndPageRequest() {
        AuthenticatedUser currentUser = authenticatedUser();
        PaymentPageRequest pageRequest = new PaymentPageRequest(0, 20, PaymentSortField.CREATED_AT, SortDirection.ASC);
        PaymentPage<PaymentSummary> expected = new PaymentPage<>(List.of(summary()), 0, 20, 1);
        ListPaymentsUseCase useCase = (tenantId, request) -> {
            assertThat(tenantId).isEqualTo(currentUser.tenantId());
            assertThat(request).isEqualTo(pageRequest);
            return expected;
        };

        assertThat(mapper.listPayments(useCase, currentUser, pageRequest)).isEqualTo(expected);
    }

    @Test
    void listPaymentsConsolidatesPageRequestAndResponseForTheController() {
        AuthenticatedUser currentUser = authenticatedUser();
        ListPaymentsUseCase useCase = (tenantId, request) -> new PaymentPage<>(List.of(summary()), 0, 20, 1);

        PageResponse<PaymentSummaryResponse> response =
                mapper.listPayments(useCase, currentUser, 0, 20, "createdAt", "asc");

        assertThat(response.content()).hasSize(1);
        assertThat(response.totalElements()).isEqualTo(1);
    }

    @Test
    void mapsSummaryToResponse() {
        PaymentSummaryResponse response = mapper.toSummaryResponse(summary());

        assertThat(response.status()).isEqualTo("INITIATED");
    }

    @Test
    void mapsPaymentPageToPageResponse() {
        PaymentPage<PaymentSummary> page = new PaymentPage<>(List.of(summary(), summary()), 0, 2, 3);

        PageResponse<PaymentSummaryResponse> response = mapper.toSummaryPage(page);

        assertThat(response.content()).hasSize(2);
        assertThat(response.totalPages()).isEqualTo(2);
        assertThat(response.hasNext()).isTrue();
        assertThat(response.hasPrevious()).isFalse();
    }

    @Test
    void mapsUpdateStatusRequestForVerifiedRequiringProviderReference() {
        AuthenticatedUser currentUser = authenticatedUser();
        UUID paymentId = UUID.randomUUID();
        UpdatePaymentStatusRequest request =
                new UpdatePaymentStatusRequest("VERIFIED", "test-provider", "txn-001", null);

        UpdatePaymentStatusCommand command = mapper.toUpdateStatusCommand(paymentId.toString(), request, currentUser);

        assertThat(command.tenantId()).isEqualTo(currentUser.tenantId());
        assertThat(command.paymentId().value()).isEqualTo(paymentId);
        assertThat(command.targetStatus().name()).isEqualTo("VERIFIED");
        assertThat(command.providerReference().provider()).isEqualTo("test-provider");
        assertThat(command.providerReference().transactionId()).isEqualTo("txn-001");
        assertThat(command.actorId()).isEqualTo(currentUser.userId().toAggregateId());
    }

    @Test
    void rejectsVerifiedStatusWithoutProviderReference() {
        AuthenticatedUser currentUser = authenticatedUser();
        UpdatePaymentStatusRequest request = new UpdatePaymentStatusRequest("VERIFIED", null, null, null);

        assertThatThrownBy(() -> mapper.toUpdateStatusCommand(UUID.randomUUID().toString(), request, currentUser))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void mapsUpdateStatusRequestForFailedRequiringFailureCode() {
        AuthenticatedUser currentUser = authenticatedUser();
        UUID paymentId = UUID.randomUUID();
        UpdatePaymentStatusRequest request = new UpdatePaymentStatusRequest("FAILED", null, null, "provider-declined");

        UpdatePaymentStatusCommand command = mapper.toUpdateStatusCommand(paymentId.toString(), request, currentUser);

        assertThat(command.failureCode()).isEqualTo("provider-declined");
        assertThat(command.providerReference()).isNull();
    }

    @Test
    void rejectsFailedStatusWithoutFailureCode() {
        AuthenticatedUser currentUser = authenticatedUser();
        UpdatePaymentStatusRequest request = new UpdatePaymentStatusRequest("FAILED", null, null, null);

        assertThatThrownBy(() -> mapper.toUpdateStatusCommand(UUID.randomUUID().toString(), request, currentUser))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void mapsUpdateStatusRequestForPendingProviderWithoutExtraFields() {
        AuthenticatedUser currentUser = authenticatedUser();
        UpdatePaymentStatusRequest request = new UpdatePaymentStatusRequest("PENDING_PROVIDER", null, null, null);

        UpdatePaymentStatusCommand command =
                mapper.toUpdateStatusCommand(UUID.randomUUID().toString(), request, currentUser);

        assertThat(command.providerReference()).isNull();
        assertThat(command.failureCode()).isNull();
    }

    private AuthenticatedUser authenticatedUser() {
        return new AuthenticatedUser(
                UserId.newId(),
                MobileNumber.of("+919876543210"),
                AggregateId.newId(),
                Set.of("GROUP_MEMBER"),
                Set.of("payment.read"));
    }

    private PaymentResult result(UUID paymentId, String status) {
        Instant now = Instant.parse("2026-07-06T08:00:00Z");
        return new PaymentResult(
                paymentId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "PAY-1A2B3C4D5E6F7A8B",
                500_000L,
                "INR",
                "UPI",
                status,
                "MATCHED",
                List.of(new PaymentAttemptResult(
                        UUID.randomUUID(), 1, now, "SUCCEEDED", "test-provider", "txn-001", null)),
                now,
                now,
                0);
    }

    private PaymentSummary summary() {
        return new PaymentSummary(
                UUID.randomUUID(), "PAY-1A2B3C4D5E6F7A8B", 500_000L, "INR", "UPI", "INITIATED",
                Instant.parse("2026-07-06T08:00:00Z"));
    }
}
