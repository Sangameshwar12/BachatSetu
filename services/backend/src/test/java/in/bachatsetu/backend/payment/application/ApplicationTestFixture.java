package in.bachatsetu.backend.payment.application;

import in.bachatsetu.backend.payment.application.command.CreatePaymentCommand;
import in.bachatsetu.backend.payment.application.port.TransactionPort;
import in.bachatsetu.backend.payment.domain.model.IdempotencyKey;
import in.bachatsetu.backend.payment.domain.model.PaymentMethod;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Money;
import java.util.function.Supplier;

public final class ApplicationTestFixture {

    private ApplicationTestFixture() {
    }

    public static CreatePaymentCommand createCommand() {
        return new CreatePaymentCommand(
                AggregateId.newId(),
                AggregateId.newId(),
                AggregateId.newId(),
                new IdempotencyKey("checkout-attempt-0001"),
                Money.inr(100_000),
                PaymentMethod.UPI,
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
