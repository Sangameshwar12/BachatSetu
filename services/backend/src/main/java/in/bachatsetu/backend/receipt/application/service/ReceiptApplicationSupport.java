package in.bachatsetu.backend.receipt.application.service;

import in.bachatsetu.backend.receipt.application.mapper.ReceiptApplicationMapper;
import in.bachatsetu.backend.receipt.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.receipt.application.query.ReceiptResult;
import in.bachatsetu.backend.receipt.domain.model.Receipt;
import in.bachatsetu.backend.receipt.domain.port.ReceiptRepository;
import java.util.Objects;

/** Shared persistence and event-publication mechanics for command services. */
final class ReceiptApplicationSupport {

    private final ReceiptRepository repository;
    private final DomainEventPublisherPort eventPublisher;
    private final ReceiptApplicationMapper mapper;

    ReceiptApplicationSupport(
            ReceiptRepository repository,
            DomainEventPublisherPort eventPublisher,
            ReceiptApplicationMapper mapper) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "event publisher must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    ReceiptResult saveAndPublish(Receipt receipt) {
        repository.save(receipt);
        eventPublisher.publish(receipt.pullDomainEvents());
        return mapper.toResult(receipt);
    }
}
