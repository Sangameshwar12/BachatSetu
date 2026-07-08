package in.bachatsetu.backend.notification.interfaces.rest.adapter;

import in.bachatsetu.backend.notification.application.port.TransactionPort;
import java.util.Objects;
import java.util.function.Supplier;
import org.springframework.transaction.support.TransactionTemplate;

/** Executes one Notification use case inside a Spring-managed transaction. */
public final class SpringNotificationTransactionAdapter implements TransactionPort {

    private final TransactionTemplate transactionTemplate;

    public SpringNotificationTransactionAdapter(TransactionTemplate transactionTemplate) {
        this.transactionTemplate = Objects.requireNonNull(
                transactionTemplate, "transaction template must not be null");
    }

    @Override
    public <T> T execute(Supplier<T> operation) {
        Objects.requireNonNull(operation, "operation must not be null");
        return transactionTemplate.execute(status -> operation.get());
    }
}
