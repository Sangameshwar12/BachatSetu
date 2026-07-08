package in.bachatsetu.backend.audit.interfaces.rest.adapter;

import in.bachatsetu.backend.audit.application.port.TransactionPort;
import java.util.Objects;
import java.util.function.Supplier;
import org.springframework.transaction.support.TransactionTemplate;

/** Executes one Audit use case inside a Spring-managed transaction. */
public final class SpringAuditTransactionAdapter implements TransactionPort {

    private final TransactionTemplate transactionTemplate;

    public SpringAuditTransactionAdapter(TransactionTemplate transactionTemplate) {
        this.transactionTemplate = Objects.requireNonNull(transactionTemplate, "transaction template must not be null");
    }

    @Override
    public <T> T execute(Supplier<T> operation) {
        Objects.requireNonNull(operation, "operation must not be null");
        return transactionTemplate.execute(status -> operation.get());
    }
}
