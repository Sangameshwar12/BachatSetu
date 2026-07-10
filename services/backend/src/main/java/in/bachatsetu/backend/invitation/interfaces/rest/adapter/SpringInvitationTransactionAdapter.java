package in.bachatsetu.backend.invitation.interfaces.rest.adapter;

import in.bachatsetu.backend.invitation.application.port.TransactionPort;
import java.util.Objects;
import java.util.function.Supplier;
import org.springframework.transaction.support.TransactionTemplate;

public final class SpringInvitationTransactionAdapter implements TransactionPort {

    private final TransactionTemplate transactionTemplate;

    public SpringInvitationTransactionAdapter(TransactionTemplate transactionTemplate) {
        this.transactionTemplate = Objects.requireNonNull(transactionTemplate, "transaction template must not be null");
    }

    @Override
    public <T> T execute(Supplier<T> operation) {
        Objects.requireNonNull(operation, "operation must not be null");
        return transactionTemplate.execute(status -> operation.get());
    }
}
