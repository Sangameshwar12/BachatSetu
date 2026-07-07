package in.bachatsetu.backend.payment.application.service;

import in.bachatsetu.backend.payment.application.exception.PaymentNotFoundException;
import in.bachatsetu.backend.payment.application.mapper.PaymentApplicationMapper;
import in.bachatsetu.backend.payment.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.payment.application.query.PaymentResult;
import in.bachatsetu.backend.payment.domain.model.Payment;
import in.bachatsetu.backend.payment.domain.port.PaymentRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

/** Shared persistence and event-publication mechanics for command services. */
final class PaymentApplicationSupport {

    private final PaymentRepository repository;
    private final DomainEventPublisherPort eventPublisher;
    private final PaymentApplicationMapper mapper;

    PaymentApplicationSupport(
            PaymentRepository repository,
            DomainEventPublisherPort eventPublisher,
            PaymentApplicationMapper mapper) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "event publisher must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    Payment requirePayment(AggregateId tenantId, AggregateId paymentId) {
        return repository.findById(tenantId, paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("payment does not exist"));
    }

    PaymentResult saveAndPublish(Payment payment) {
        repository.save(payment);
        eventPublisher.publish(payment.pullDomainEvents());
        return mapper.toResult(payment);
    }
}
