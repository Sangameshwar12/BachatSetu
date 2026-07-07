package in.bachatsetu.backend.receipt.application.command;

import in.bachatsetu.backend.receipt.domain.model.ReceiptLine;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.List;
import java.util.Objects;

/** Requests generation of a new Receipt aggregate for a verified payment. */
public record CreateReceiptCommand(
        AggregateId tenantId,
        AggregateId paymentId,
        AggregateId memberId,
        List<ReceiptLine> lines,
        AggregateId actorId) {

    public CreateReceiptCommand {
        Objects.requireNonNull(tenantId, "tenant id must not be null");
        Objects.requireNonNull(paymentId, "payment id must not be null");
        Objects.requireNonNull(memberId, "member id must not be null");
        lines = List.copyOf(Objects.requireNonNull(lines, "lines must not be null"));
        Objects.requireNonNull(actorId, "actor id must not be null");
    }
}
