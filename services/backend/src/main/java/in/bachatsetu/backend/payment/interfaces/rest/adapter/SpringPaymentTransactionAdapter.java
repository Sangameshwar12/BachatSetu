package in.bachatsetu.backend.payment.interfaces.rest.adapter;

import in.bachatsetu.backend.payment.application.port.TransactionPort;
import java.util.Objects;
import java.util.function.Supplier;
import org.springframework.transaction.support.TransactionTemplate;

/** Executes one Payment use case inside a Spring-managed transaction. */
public final class SpringPaymentTransactionAdapter implements TransactionPort {

    private final TransactionTemplate transactionTemplate;

    public SpringPaymentTransactionAdapter(TransactionTemplate transactionTemplate) {
        this.transactionTemplate = Objects.requireNonNull(
                transactionTemplate, "transaction template must not be null");
    }

    @Override
    public <T> T execute(Supplier<T> operation) {
        Objects.requireNonNull(operation, "operation must not be null");
        return transactionTemplate.execute(status -> operation.get());
    }
}
