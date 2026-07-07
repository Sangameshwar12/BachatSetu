package in.bachatsetu.backend.payment.interfaces.rest.mapper;

import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.payment.application.command.CreatePaymentCommand;
import in.bachatsetu.backend.payment.application.command.UpdatePaymentStatusCommand;
import in.bachatsetu.backend.payment.application.query.PaymentAttemptResult;
import in.bachatsetu.backend.payment.application.query.PaymentResult;
import in.bachatsetu.backend.payment.application.query.PaymentSummary;
import in.bachatsetu.backend.payment.application.usecase.GetPaymentUseCase;
import in.bachatsetu.backend.payment.application.usecase.ListPaymentsUseCase;
import in.bachatsetu.backend.payment.domain.model.IdempotencyKey;
import in.bachatsetu.backend.payment.domain.model.PaymentMethod;
import in.bachatsetu.backend.payment.domain.model.PaymentStatus;
import in.bachatsetu.backend.payment.domain.model.ProviderReference;
import in.bachatsetu.backend.payment.domain.port.PaymentPage;
import in.bachatsetu.backend.payment.domain.port.PaymentPageRequest;
import in.bachatsetu.backend.payment.domain.port.PaymentSortField;
import in.bachatsetu.backend.payment.domain.port.SortDirection;
import in.bachatsetu.backend.payment.interfaces.rest.dto.CreatePaymentRequest;
import in.bachatsetu.backend.payment.interfaces.rest.dto.PageResponse;
import in.bachatsetu.backend.payment.interfaces.rest.dto.PaymentAttemptResponse;
import in.bachatsetu.backend.payment.interfaces.rest.dto.PaymentResponse;
import in.bachatsetu.backend.payment.interfaces.rest.dto.PaymentSummaryResponse;
import in.bachatsetu.backend.payment.interfaces.rest.dto.UpdatePaymentStatusRequest;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Money;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

/** Maps validated HTTP contracts to Payment application commands and safe responses. */
@Component
public class PaymentApiMapper {

    public CreatePaymentCommand toCreateCommand(CreatePaymentRequest request, AuthenticatedUser currentUser) {
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(currentUser, "current user must not be null");
        return new CreatePaymentCommand(
                currentUser.tenantId(),
                AggregateId.from(request.groupId()),
                AggregateId.from(request.memberId()),
                new IdempotencyKey(request.idempotencyKey()),
                Money.inr(request.amountPaise()),
                PaymentMethod.valueOf(request.method()),
                currentUser.userId().toAggregateId());
    }

    public PaymentResult getPayment(GetPaymentUseCase useCase, AuthenticatedUser currentUser, String paymentId) {
        Objects.requireNonNull(useCase, "use case must not be null");
        Objects.requireNonNull(currentUser, "current user must not be null");
        Objects.requireNonNull(paymentId, "payment id must not be null");
        return useCase.execute(currentUser.tenantId(), AggregateId.from(paymentId));
    }

    public PaymentResponse toResponse(PaymentResult result) {
        Objects.requireNonNull(result, "result must not be null");
        List<PaymentAttemptResponse> attempts = result.attempts().stream()
                .map(this::toAttemptResponse)
                .toList();
        return new PaymentResponse(
                result.paymentId().toString(),
                result.tenantId().toString(),
                result.groupId().toString(),
                result.memberId().toString(),
                result.reference(),
                result.amountPaise(),
                result.currencyCode(),
                result.method(),
                result.status(),
                result.reconciliationStatus(),
                attempts,
                result.createdAt(),
                result.updatedAt(),
                result.version());
    }

    public PaymentAttemptResponse toAttemptResponse(PaymentAttemptResult attempt) {
        Objects.requireNonNull(attempt, "attempt must not be null");
        return new PaymentAttemptResponse(
                attempt.attemptId().toString(),
                attempt.sequence(),
                attempt.initiatedAt(),
                attempt.status(),
                attempt.provider(),
                attempt.transactionId(),
                attempt.failureCode());
    }

    public PaymentPageRequest toPageRequest(int page, int size, String sort, String direction) {
        return new PaymentPageRequest(page, size, toSortField(sort), toSortDirection(direction));
    }

    public PaymentPage<PaymentSummary> listPayments(
            ListPaymentsUseCase useCase,
            AuthenticatedUser currentUser,
            PaymentPageRequest pageRequest) {
        Objects.requireNonNull(useCase, "use case must not be null");
        Objects.requireNonNull(currentUser, "current user must not be null");
        Objects.requireNonNull(pageRequest, "page request must not be null");
        return useCase.execute(currentUser.tenantId(), pageRequest);
    }

    public PageResponse<PaymentSummaryResponse> listPayments(
            ListPaymentsUseCase useCase,
            AuthenticatedUser currentUser,
            int page,
            int size,
            String sort,
            String direction) {
        PaymentPageRequest pageRequest = toPageRequest(page, size, sort, direction);
        return toSummaryPage(listPayments(useCase, currentUser, pageRequest));
    }

    public PaymentSummaryResponse toSummaryResponse(PaymentSummary summary) {
        Objects.requireNonNull(summary, "summary must not be null");
        return new PaymentSummaryResponse(
                summary.paymentId().toString(),
                summary.reference(),
                summary.amountPaise(),
                summary.currencyCode(),
                summary.method(),
                summary.status(),
                summary.createdAt());
    }

    public PageResponse<PaymentSummaryResponse> toSummaryPage(PaymentPage<PaymentSummary> page) {
        Objects.requireNonNull(page, "page must not be null");
        List<PaymentSummaryResponse> content = page.content().stream()
                .map(this::toSummaryResponse)
                .toList();
        return new PageResponse<>(
                content, page.page(), page.size(), page.totalElements(), page.totalPages(),
                page.hasNext(), page.hasPrevious());
    }

    public UpdatePaymentStatusCommand toUpdateStatusCommand(
            String paymentId,
            UpdatePaymentStatusRequest request,
            AuthenticatedUser currentUser) {
        Objects.requireNonNull(paymentId, "payment id must not be null");
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(currentUser, "current user must not be null");
        PaymentStatus targetStatus = PaymentStatus.valueOf(request.status());
        return new UpdatePaymentStatusCommand(
                currentUser.tenantId(),
                AggregateId.from(paymentId),
                targetStatus,
                toProviderReference(targetStatus, request),
                toFailureCode(targetStatus, request),
                currentUser.userId().toAggregateId());
    }

    private ProviderReference toProviderReference(PaymentStatus targetStatus, UpdatePaymentStatusRequest request) {
        if (targetStatus != PaymentStatus.VERIFIED) {
            return null;
        }
        if (request.provider() == null || request.transactionId() == null) {
            throw new IllegalArgumentException("provider and transactionId are required when status is VERIFIED");
        }
        return new ProviderReference(request.provider(), request.transactionId());
    }

    private String toFailureCode(PaymentStatus targetStatus, UpdatePaymentStatusRequest request) {
        if (targetStatus != PaymentStatus.FAILED) {
            return null;
        }
        if (request.failureCode() == null) {
            throw new IllegalArgumentException("failureCode is required when status is FAILED");
        }
        return request.failureCode();
    }

    private PaymentSortField toSortField(String sort) {
        return switch (sort) {
            case "amount" -> PaymentSortField.AMOUNT;
            case "createdAt" -> PaymentSortField.CREATED_AT;
            default -> throw new IllegalArgumentException("unsupported sort field: " + sort);
        };
    }

    private SortDirection toSortDirection(String direction) {
        return switch (direction) {
            case "asc" -> SortDirection.ASC;
            case "desc" -> SortDirection.DESC;
            default -> throw new IllegalArgumentException("unsupported sort direction: " + direction);
        };
    }
}
