package in.bachatsetu.backend.receipt.application;

import in.bachatsetu.backend.receipt.application.command.CreateReceiptCommand;
import in.bachatsetu.backend.receipt.application.port.TransactionPort;
import in.bachatsetu.backend.receipt.domain.model.ReceiptDescription;
import in.bachatsetu.backend.receipt.domain.model.ReceiptLine;
import in.bachatsetu.backend.receipt.domain.model.ReceiptType;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Money;
import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;

public final class ApplicationTestFixture {

    public static final Instant NOW = Instant.parse("2026-07-07T08:00:00Z");

    private ApplicationTestFixture() {
    }

    public static CreateReceiptCommand createCommand() {
        return new CreateReceiptCommand(
                AggregateId.newId(),
                AggregateId.newId(),
                AggregateId.newId(),
                List.of(new ReceiptLine(
                        AggregateId.newId(), ReceiptType.CONTRIBUTION,
                        new ReceiptDescription("Monthly contribution"), Money.inr(500_000))),
                AggregateId.newId());
    }

    public static TransactionPort directTransaction() {
        return new TransactionPort() {
            @Override
            public <T> T execute(Supplier<T> operation) {
                return operation.get();
            }
        };
    }
}
