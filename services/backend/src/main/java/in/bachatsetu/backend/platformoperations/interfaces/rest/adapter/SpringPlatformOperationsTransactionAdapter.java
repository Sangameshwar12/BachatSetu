package in.bachatsetu.backend.platformoperations.interfaces.rest.adapter;

import in.bachatsetu.backend.platformoperations.application.port.TransactionPort;
import java.util.Objects;
import java.util.function.Supplier;
import org.springframework.transaction.support.TransactionTemplate;

public final class SpringPlatformOperationsTransactionAdapter implements TransactionPort {

    private final TransactionTemplate transactionTemplate;

    public SpringPlatformOperationsTransactionAdapter(TransactionTemplate transactionTemplate) {
        this.transactionTemplate = Objects.requireNonNull(transactionTemplate, "transaction template must not be null");
    }

    @Override
    public <T> T execute(Supplier<T> operation) {
        Objects.requireNonNull(operation, "operation must not be null");
        return transactionTemplate.execute(status -> operation.get());
    }
}
