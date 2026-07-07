package in.bachatsetu.backend.receipt.application.mapper;

import in.bachatsetu.backend.receipt.application.query.ReceiptLineResult;
import in.bachatsetu.backend.receipt.application.query.ReceiptResult;
import in.bachatsetu.backend.receipt.application.query.ReceiptSummary;
import in.bachatsetu.backend.receipt.domain.model.Receipt;
import in.bachatsetu.backend.receipt.domain.model.ReceiptLine;
import java.util.Objects;

/** Maps the Receipt domain aggregate to immutable application query models. */
public final class ReceiptApplicationMapper {

    public ReceiptResult toResult(Receipt receipt) {
        Objects.requireNonNull(receipt, "receipt must not be null");
        return new ReceiptResult(
                receipt.id().value(),
                receipt.tenantId().value(),
                receipt.paymentId().value(),
                receipt.memberId().value(),
                receipt.number().value(),
                receipt.lines().stream().map(this::toLineResult).toList(),
                receipt.total().minorUnits(),
                receipt.total().currency().getCurrencyCode(),
                receipt.status().name(),
                receipt.cancellationReason(),
                receipt.auditInfo().createdAt(),
                receipt.auditInfo().updatedAt(),
                receipt.version());
    }

    public ReceiptSummary toSummary(Receipt receipt) {
        Objects.requireNonNull(receipt, "receipt must not be null");
        return new ReceiptSummary(
                receipt.id().value(),
                receipt.number().value(),
                receipt.total().minorUnits(),
                receipt.total().currency().getCurrencyCode(),
                receipt.status().name(),
                receipt.auditInfo().createdAt());
    }

    public ReceiptLineResult toLineResult(ReceiptLine line) {
        Objects.requireNonNull(line, "line must not be null");
        return new ReceiptLineResult(
                line.id().value(),
                line.type().name(),
                line.description().value(),
                line.amount().minorUnits(),
                line.amount().currency().getCurrencyCode());
    }
}
