package in.bachatsetu.backend.payment.application.service;

import in.bachatsetu.backend.payment.application.exception.PaymentNotFoundException;
import in.bachatsetu.backend.payment.application.mapper.PaymentApplicationMapper;
import in.bachatsetu.backend.payment.application.port.TransactionPort;
import in.bachatsetu.backend.payment.application.query.PaymentResult;
import in.bachatsetu.backend.payment.application.usecase.GetPaymentUseCase;
import in.bachatsetu.backend.payment.domain.model.Payment;
import in.bachatsetu.backend.payment.domain.port.PaymentRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

/** Retrieves and maps a tenant-scoped Payment aggregate. */
public final class GetPaymentApplicationService implements GetPaymentUseCase {

    private final PaymentRepository repository;
    private final TransactionPort transaction;
    private final PaymentApplicationMapper mapper;

    public GetPaymentApplicationService(
            PaymentRepository repository,
            TransactionPort transaction,
            PaymentApplicationMapper mapper) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public PaymentResult execute(AggregateId tenantId, AggregateId paymentId) {
        Objects.requireNonNull(tenantId, "tenant id must not be null");
        Objects.requireNonNull(paymentId, "payment id must not be null");
        return transaction.execute(() -> {
            Payment payment = repository.findById(tenantId, paymentId)
                    .orElseThrow(() -> new PaymentNotFoundException("payment does not exist"));
            return mapper.toResult(payment);
        });
    }
}
