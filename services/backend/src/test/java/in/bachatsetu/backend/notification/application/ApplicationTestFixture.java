package in.bachatsetu.backend.notification.application;

import in.bachatsetu.backend.notification.application.command.CreateNotificationCommand;
import in.bachatsetu.backend.notification.application.port.TransactionPort;
import in.bachatsetu.backend.notification.domain.model.NotificationCategory;
import in.bachatsetu.backend.notification.domain.model.NotificationChannel;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.Map;
import java.util.function.Supplier;

public final class ApplicationTestFixture {

    public static final Instant NOW = Instant.parse("2026-07-08T08:00:00Z");

    private ApplicationTestFixture() {
    }

    public static CreateNotificationCommand createCommand() {
        return new CreateNotificationCommand(
                AggregateId.newId(),
                AggregateId.newId(),
                "member@example.com",
                NotificationChannel.EMAIL,
                NotificationCategory.VERIFICATION,
                Map.of("memberName", "Asha"),
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
