package in.bachatsetu.backend.payment.application.service;

import in.bachatsetu.backend.payment.application.mapper.PaymentApplicationMapper;
import in.bachatsetu.backend.payment.application.port.TransactionPort;
import in.bachatsetu.backend.payment.application.query.PaymentSummary;
import in.bachatsetu.backend.payment.application.usecase.ListPaymentsUseCase;
import in.bachatsetu.backend.payment.domain.model.Payment;
import in.bachatsetu.backend.payment.domain.port.PaymentPage;
import in.bachatsetu.backend.payment.domain.port.PaymentPageRequest;
import in.bachatsetu.backend.payment.domain.port.PaymentRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.List;
import java.util.Objects;

/** Lists tenant-scoped payments as compact immutable query models, paginated by the repository. */
public final class ListPaymentsApplicationService implements ListPaymentsUseCase {

    private final PaymentRepository repository;
    private final TransactionPort transaction;
    private final PaymentApplicationMapper mapper;

    public ListPaymentsApplicationService(
            PaymentRepository repository,
            TransactionPort transaction,
            PaymentApplicationMapper mapper) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public PaymentPage<PaymentSummary> execute(AggregateId tenantId, PaymentPageRequest pageRequest) {
        Objects.requireNonNull(tenantId, "tenant id must not be null");
        Objects.requireNonNull(pageRequest, "page request must not be null");
        return transaction.execute(() -> {
            PaymentPage<Payment> page = repository.findPage(tenantId, pageRequest);
            List<PaymentSummary> summaries = page.content().stream().map(mapper::toSummary).toList();
            return new PaymentPage<>(summaries, page.page(), page.size(), page.totalElements());
        });
    }
}
