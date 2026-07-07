package in.bachatsetu.backend.payment.application.service;

import in.bachatsetu.backend.payment.application.command.UpdatePaymentStatusCommand;
import in.bachatsetu.backend.payment.application.mapper.PaymentApplicationMapper;
import in.bachatsetu.backend.payment.application.port.ClockPort;
import in.bachatsetu.backend.payment.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.payment.application.port.TransactionPort;
import in.bachatsetu.backend.payment.application.query.PaymentResult;
import in.bachatsetu.backend.payment.application.usecase.UpdatePaymentStatusUseCase;
import in.bachatsetu.backend.payment.domain.model.Payment;
import in.bachatsetu.backend.payment.domain.port.PaymentRepository;
import java.time.Instant;
import java.util.Objects;

/** Coordinates payment status transitions without owning business invariants. */
public final class UpdatePaymentStatusApplicationService implements UpdatePaymentStatusUseCase {

    private final ClockPort clock;
    private final TransactionPort transaction;
    private final PaymentApplicationSupport support;

    public UpdatePaymentStatusApplicationService(
            PaymentRepository repository,
            DomainEventPublisherPort eventPublisher,
            ClockPort clock,
            TransactionPort transaction,
            PaymentApplicationMapper mapper) {
        Objects.requireNonNull(repository, "repository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
        this.support = new PaymentApplicationSupport(repository, eventPublisher, mapper);
    }

    @Override
    public PaymentResult execute(UpdatePaymentStatusCommand command) {
        Objects.requireNonNull(command, "update command must not be null");
        return transaction.execute(() -> update(command));
    }

    private PaymentResult update(UpdatePaymentStatusCommand command) {
        Payment payment = support.requirePayment(command.tenantId(), command.paymentId());
        Instant now = clock.now();
        switch (command.targetStatus()) {
            case PENDING_PROVIDER -> payment.startAttempt(command.actorId(), now);
            case VERIFIED -> payment.verify(command.providerReference(), command.actorId(), now);
            case FAILED -> payment.fail(command.failureCode(), command.actorId(), now);
            default -> throw new IllegalArgumentException(
                    "unsupported target status: " + command.targetStatus());
        }
        return support.saveAndPublish(payment);
    }
}
