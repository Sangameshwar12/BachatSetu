package in.bachatsetu.backend.draw.application;

import in.bachatsetu.backend.draw.application.command.CreateDrawCommand;
import in.bachatsetu.backend.draw.application.port.TransactionPort;
import in.bachatsetu.backend.draw.domain.model.DrawNumber;
import in.bachatsetu.backend.draw.domain.model.DrawType;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.function.Supplier;

public final class ApplicationTestFixture {

    public static final Instant NOW = Instant.parse("2026-07-07T08:00:00Z");

    private ApplicationTestFixture() {
    }

    public static CreateDrawCommand createCommand() {
        return new CreateDrawCommand(
                AggregateId.newId(),
                AggregateId.newId(),
                AggregateId.newId(),
                new DrawNumber(1),
                DrawType.AUCTION,
                NOW.plusSeconds(3600),
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
