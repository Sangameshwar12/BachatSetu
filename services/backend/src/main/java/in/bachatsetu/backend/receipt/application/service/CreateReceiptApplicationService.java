package in.bachatsetu.backend.receipt.application.service;

import in.bachatsetu.backend.audit.application.command.CreateAuditEntryCommand;
import in.bachatsetu.backend.audit.application.usecase.CreateAuditEntryUseCase;
import in.bachatsetu.backend.audit.domain.model.AuditEventType;
import in.bachatsetu.backend.receipt.application.command.CreateReceiptCommand;
import in.bachatsetu.backend.receipt.application.mapper.ReceiptApplicationMapper;
import in.bachatsetu.backend.receipt.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.receipt.application.port.TransactionPort;
import in.bachatsetu.backend.receipt.application.query.ReceiptResult;
import in.bachatsetu.backend.receipt.application.usecase.CreateReceiptUseCase;
import in.bachatsetu.backend.receipt.domain.factory.ReceiptFactory;
import in.bachatsetu.backend.receipt.domain.model.Receipt;
import in.bachatsetu.backend.receipt.domain.port.ReceiptRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;
import java.util.Optional;

/** Coordinates receipt generation without owning business invariants. */
public final class CreateReceiptApplicationService implements CreateReceiptUseCase {

    private final ReceiptRepository repository;
    private final ReceiptFactory receiptFactory;
    private final TransactionPort transaction;
    private final ReceiptApplicationMapper mapper;
    private final ReceiptApplicationSupport support;
    private final CreateAuditEntryUseCase createAuditEntry;

    public CreateReceiptApplicationService(
            ReceiptRepository repository,
            ReceiptFactory receiptFactory,
            DomainEventPublisherPort eventPublisher,
            TransactionPort transaction,
            ReceiptApplicationMapper mapper,
            CreateAuditEntryUseCase createAuditEntry) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.receiptFactory = Objects.requireNonNull(receiptFactory, "receipt factory must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
        this.support = new ReceiptApplicationSupport(repository, eventPublisher, mapper);
        this.createAuditEntry = Objects.requireNonNull(createAuditEntry, "create audit entry use case must not be null");
    }

    @Override
    public ReceiptResult execute(CreateReceiptCommand command) {
        Objects.requireNonNull(command, "create command must not be null");
        ReceiptResult result = transaction.execute(() -> create(command));
        auditReceiptGenerated(result);
        return result;
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

    /**
     * Best-effort: an audit failure must never fail a receipt generation that has already committed, so any
     * exception is caught and discarded here rather than propagated.
     */
    private void auditReceiptGenerated(ReceiptResult result) {
        try {
            createAuditEntry.execute(new CreateAuditEntryCommand(
                    new AggregateId(result.tenantId()), new AggregateId(result.memberId()),
                    AuditEventType.RECEIPT_GENERATED, "receipt", "Receipt", new AggregateId(result.receiptId()),
                    "RECEIPT_GENERATED", "receipt generated", null, null, null));
        } catch (RuntimeException exception) {
            // Audit is best-effort: never let a recording failure affect an already-committed receipt.
        }
    }
}
