package in.bachatsetu.backend.receipt.application.service;

import in.bachatsetu.backend.receipt.application.command.CreateReceiptCommand;
import in.bachatsetu.backend.receipt.application.mapper.ReceiptApplicationMapper;
import in.bachatsetu.backend.receipt.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.receipt.application.port.TransactionPort;
import in.bachatsetu.backend.receipt.application.query.ReceiptResult;
import in.bachatsetu.backend.receipt.application.usecase.CreateReceiptUseCase;
import in.bachatsetu.backend.receipt.domain.factory.ReceiptFactory;
import in.bachatsetu.backend.receipt.domain.model.Receipt;
import in.bachatsetu.backend.receipt.domain.port.ReceiptRepository;
import java.util.Objects;
import java.util.Optional;

/** Coordinates receipt generation without owning business invariants. */
public final class CreateReceiptApplicationService implements CreateReceiptUseCase {

    private final ReceiptRepository repository;
    private final ReceiptFactory receiptFactory;
    private final TransactionPort transaction;
    private final ReceiptApplicationMapper mapper;
    private final ReceiptApplicationSupport support;

    public CreateReceiptApplicationService(
            ReceiptRepository repository,
            ReceiptFactory receiptFactory,
            DomainEventPublisherPort eventPublisher,
            TransactionPort transaction,
            ReceiptApplicationMapper mapper) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.receiptFactory = Objects.requireNonNull(receiptFactory, "receipt factory must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
        this.support = new ReceiptApplicationSupport(repository, eventPublisher, mapper);
    }

    @Override
    public ReceiptResult execute(CreateReceiptCommand command) {
        Objects.requireNonNull(command, "create command must not be null");
        return transaction.execute(() -> create(command));
    }

    private ReceiptResult create(CreateReceiptCommand command) {
        Optional<Receipt> existing = repository.findByPaymentId(command.paymentId());
        if (existing.isPresent()) {
            return mapper.toResult(existing.get());
        }
        Receipt receipt = receiptFactory.generate(
                command.tenantId(),
                command.paymentId(),
                command.memberId(),
                command.lines(),
                command.actorId());
        return support.saveAndPublish(receipt);
    }
}
