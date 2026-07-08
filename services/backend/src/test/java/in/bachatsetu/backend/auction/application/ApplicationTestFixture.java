package in.bachatsetu.backend.auction.application;

import in.bachatsetu.backend.auction.application.command.CreateAuctionCommand;
import in.bachatsetu.backend.auction.application.port.TransactionPort;
import in.bachatsetu.backend.draw.domain.model.DrawNumber;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.function.Supplier;

public final class ApplicationTestFixture {

    public static final Instant NOW = Instant.parse("2026-07-08T08:00:00Z");

    private ApplicationTestFixture() {
    }

    public static CreateAuctionCommand createCommand() {
        return new CreateAuctionCommand(
                AggregateId.newId(),
                AggregateId.newId(),
                AggregateId.newId(),
                new DrawNumber(1),
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
