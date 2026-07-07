package in.bachatsetu.backend.receipt.interfaces.rest.mapper;

import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.receipt.application.command.CreateReceiptCommand;
import in.bachatsetu.backend.receipt.application.query.ReceiptLineResult;
import in.bachatsetu.backend.receipt.application.query.ReceiptPdfResult;
import in.bachatsetu.backend.receipt.application.query.ReceiptResult;
import in.bachatsetu.backend.receipt.application.query.ReceiptSummary;
import in.bachatsetu.backend.receipt.application.usecase.GetReceiptPdfUseCase;
import in.bachatsetu.backend.receipt.application.usecase.GetReceiptUseCase;
import in.bachatsetu.backend.receipt.application.usecase.ListReceiptsUseCase;
import in.bachatsetu.backend.receipt.domain.model.ReceiptDescription;
import in.bachatsetu.backend.receipt.domain.model.ReceiptLine;
import in.bachatsetu.backend.receipt.domain.model.ReceiptType;
import in.bachatsetu.backend.receipt.domain.port.ReceiptPage;
import in.bachatsetu.backend.receipt.domain.port.ReceiptPageRequest;
import in.bachatsetu.backend.receipt.domain.port.ReceiptSortField;
import in.bachatsetu.backend.receipt.domain.port.SortDirection;
import in.bachatsetu.backend.receipt.interfaces.rest.dto.CreateReceiptRequest;
import in.bachatsetu.backend.receipt.interfaces.rest.dto.PageResponse;
import in.bachatsetu.backend.receipt.interfaces.rest.dto.ReceiptLineRequest;
import in.bachatsetu.backend.receipt.interfaces.rest.dto.ReceiptLineResponse;
import in.bachatsetu.backend.receipt.interfaces.rest.dto.ReceiptResponse;
import in.bachatsetu.backend.receipt.interfaces.rest.dto.ReceiptSummaryResponse;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Money;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

/** Maps validated HTTP contracts to Receipt application commands and safe responses. */
@Component
public class ReceiptApiMapper {

    public CreateReceiptCommand toCreateCommand(CreateReceiptRequest request, AuthenticatedUser currentUser) {
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(currentUser, "current user must not be null");
        List<ReceiptLine> lines = request.lines().stream().map(this::toLine).toList();
        return new CreateReceiptCommand(
                currentUser.tenantId(),
                AggregateId.from(request.paymentId()),
                AggregateId.from(request.memberId()),
                lines,
                currentUser.userId().toAggregateId());
    }

    public ReceiptResult getReceipt(GetReceiptUseCase useCase, AuthenticatedUser currentUser, String receiptId) {
        Objects.requireNonNull(useCase, "use case must not be null");
        Objects.requireNonNull(currentUser, "current user must not be null");
        Objects.requireNonNull(receiptId, "receipt id must not be null");
        return useCase.execute(currentUser.tenantId(), AggregateId.from(receiptId));
    }

    public ReceiptPdfResult getReceiptPdf(GetReceiptPdfUseCase useCase, AuthenticatedUser currentUser, String receiptId) {
        Objects.requireNonNull(useCase, "use case must not be null");
        Objects.requireNonNull(currentUser, "current user must not be null");
        Objects.requireNonNull(receiptId, "receipt id must not be null");
        return useCase.execute(currentUser.tenantId(), AggregateId.from(receiptId));
    }

    public ReceiptResponse toResponse(ReceiptResult result) {
        Objects.requireNonNull(result, "result must not be null");
        List<ReceiptLineResponse> lines = result.lines().stream().map(this::toLineResponse).toList();
        return new ReceiptResponse(
                result.receiptId().toString(),
                result.tenantId().toString(),
                result.paymentId().toString(),
                result.memberId().toString(),
                result.number(),
                lines,
                result.totalAmountPaise(),
                result.currencyCode(),
                result.status(),
                result.cancellationReason(),
                result.generatedAt(),
                result.updatedAt(),
                result.version());
    }

    public ReceiptLineResponse toLineResponse(ReceiptLineResult line) {
        Objects.requireNonNull(line, "line must not be null");
        return new ReceiptLineResponse(
                line.lineId().toString(),
                line.type(),
                line.description(),
                line.amountPaise(),
                line.currencyCode());
    }

    public ReceiptPageRequest toPageRequest(int page, int size, String sort, String direction) {
        return new ReceiptPageRequest(page, size, toSortField(sort), toSortDirection(direction));
    }

    public ReceiptPage<ReceiptSummary> listReceipts(
            ListReceiptsUseCase useCase,
            AuthenticatedUser currentUser,
            ReceiptPageRequest pageRequest) {
        Objects.requireNonNull(useCase, "use case must not be null");
        Objects.requireNonNull(currentUser, "current user must not be null");
        Objects.requireNonNull(pageRequest, "page request must not be null");
        return useCase.execute(currentUser.tenantId(), pageRequest);
    }

    public PageResponse<ReceiptSummaryResponse> listReceipts(
            ListReceiptsUseCase useCase,
            AuthenticatedUser currentUser,
            int page,
            int size,
            String sort,
            String direction) {
        ReceiptPageRequest pageRequest = toPageRequest(page, size, sort, direction);
        return toSummaryPage(listReceipts(useCase, currentUser, pageRequest));
    }

    public ReceiptSummaryResponse toSummaryResponse(ReceiptSummary summary) {
        Objects.requireNonNull(summary, "summary must not be null");
        return new ReceiptSummaryResponse(
                summary.receiptId().toString(),
                summary.number(),
                summary.totalAmountPaise(),
                summary.currencyCode(),
                summary.status(),
                summary.generatedAt());
    }

    public PageResponse<ReceiptSummaryResponse> toSummaryPage(ReceiptPage<ReceiptSummary> page) {
        Objects.requireNonNull(page, "page must not be null");
        List<ReceiptSummaryResponse> content = page.content().stream()
                .map(this::toSummaryResponse)
                .toList();
        return new PageResponse<>(
                content, page.page(), page.size(), page.totalElements(), page.totalPages(),
                page.hasNext(), page.hasPrevious());
    }

    private ReceiptLine toLine(ReceiptLineRequest request) {
        return new ReceiptLine(
                AggregateId.newId(),
                ReceiptType.valueOf(request.type()),
                new ReceiptDescription(request.description()),
                Money.inr(request.amountPaise()));
    }

    private ReceiptSortField toSortField(String sort) {
        return switch (sort) {
            case "amount" -> ReceiptSortField.AMOUNT;
            case "createdAt" -> ReceiptSortField.CREATED_AT;
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
