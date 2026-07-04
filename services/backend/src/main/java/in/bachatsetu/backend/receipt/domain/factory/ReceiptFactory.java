package in.bachatsetu.backend.receipt.domain.factory;

import in.bachatsetu.backend.receipt.domain.model.Receipt;
import in.bachatsetu.backend.receipt.domain.model.ReceiptLine;
import in.bachatsetu.backend.receipt.domain.model.ReceiptNumber;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Clock;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class ReceiptFactory {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);

    private final Clock clock;

    public ReceiptFactory(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public Receipt generate(
            AggregateId tenantId,
            AggregateId paymentId,
            AggregateId memberId,
            List<ReceiptLine> lines,
            AggregateId actorId) {
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        ReceiptNumber number = new ReceiptNumber("RCT/" + DATE.format(clock.instant()) + "/" + suffix);
        return Receipt.generate(
                AggregateId.newId(), tenantId, paymentId, memberId, number, lines, actorId, clock.instant());
    }
}
