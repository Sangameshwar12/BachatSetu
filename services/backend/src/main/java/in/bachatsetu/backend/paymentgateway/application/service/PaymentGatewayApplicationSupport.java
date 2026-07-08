package in.bachatsetu.backend.paymentgateway.application.service;

import in.bachatsetu.backend.paymentgateway.application.exception.GatewayOrderNotFoundException;
import in.bachatsetu.backend.paymentgateway.application.mapper.PaymentGatewayApplicationMapper;
import in.bachatsetu.backend.paymentgateway.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.paymentgateway.application.query.PaymentOrderResult;
import in.bachatsetu.backend.paymentgateway.domain.model.GatewayOrder;
import in.bachatsetu.backend.paymentgateway.domain.port.GatewayOrderRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

/** Shared persistence and lookup mechanics for the Payment Gateway command services. */
final class PaymentGatewayApplicationSupport {

    private final GatewayOrderRepository repository;
    private final DomainEventPublisherPort eventPublisher;
    private final PaymentGatewayApplicationMapper mapper;

    PaymentGatewayApplicationSupport(
            GatewayOrderRepository repository,
            DomainEventPublisherPort eventPublisher,
            PaymentGatewayApplicationMapper mapper) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "event publisher must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    GatewayOrder requireOrderByPayment(AggregateId tenantId, AggregateId paymentId) {
        return repository.findByPaymentId(tenantId, paymentId)
                .orElseThrow(() -> new GatewayOrderNotFoundException(
                        "no gateway order exists for payment " + paymentId));
    }

    PaymentOrderResult saveAndPublish(GatewayOrder order) {
        repository.save(order);
        eventPublisher.publish(order.pullDomainEvents());
        return mapper.toOrderResult(order);
    }
}
