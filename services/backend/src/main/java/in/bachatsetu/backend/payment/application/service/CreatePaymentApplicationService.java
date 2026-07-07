package in.bachatsetu.backend.payment.application.service;

import in.bachatsetu.backend.payment.application.command.CreatePaymentCommand;
import in.bachatsetu.backend.payment.application.mapper.PaymentApplicationMapper;
import in.bachatsetu.backend.payment.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.payment.application.port.TransactionPort;
import in.bachatsetu.backend.payment.application.query.PaymentResult;
import in.bachatsetu.backend.payment.application.usecase.CreatePaymentUseCase;
import in.bachatsetu.backend.payment.domain.factory.PaymentFactory;
import in.bachatsetu.backend.payment.domain.model.Payment;
import in.bachatsetu.backend.payment.domain.port.PaymentRepository;
import java.util.Objects;
import java.util.Optional;

/** Coordinates payment initiation without owning business invariants. */
public final class CreatePaymentApplicationService implements CreatePaymentUseCase {

    private final PaymentRepository repository;
    private final PaymentFactory paymentFactory;
    private final TransactionPort transaction;
    private final PaymentApplicationMapper mapper;
    private final PaymentApplicationSupport support;

    public CreatePaymentApplicationService(
            PaymentRepository repository,
            PaymentFactory paymentFactory,
            DomainEventPublisherPort eventPublisher,
            TransactionPort transaction,
            PaymentApplicationMapper mapper) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.paymentFactory = Objects.requireNonNull(paymentFactory, "payment factory must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
        this.support = new PaymentApplicationSupport(repository, eventPublisher, mapper);
    }

    @Override
    public PaymentResult execute(CreatePaymentCommand command) {
        Objects.requireNonNull(command, "create command must not be null");
        return transaction.execute(() -> create(command));
    }

    private PaymentResult create(CreatePaymentCommand command) {
        Optional<Payment> existing = repository.findByIdempotencyKey(command.tenantId(), command.idempotencyKey());
        if (existing.isPresent()) {
            return mapper.toResult(existing.get());
        }
        Payment payment = paymentFactory.initiate(
                command.tenantId(),
                command.groupId(),
                command.memberId(),
                command.idempotencyKey(),
                command.amount(),
                command.method(),
                command.actorId());
        return support.saveAndPublish(payment);
    }
}
