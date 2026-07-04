package in.bachatsetu.backend.infrastructure.persistence.adapter;

import in.bachatsetu.backend.infrastructure.persistence.entity.finance.PaymentJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.mapper.JpaReferenceProvider;
import in.bachatsetu.backend.infrastructure.persistence.mapper.PaymentJpaMapper;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.PaymentSpringDataRepository;
import in.bachatsetu.backend.payment.domain.model.IdempotencyKey;
import in.bachatsetu.backend.payment.domain.model.Payment;
import in.bachatsetu.backend.payment.domain.model.PaymentReference;
import in.bachatsetu.backend.payment.domain.model.ProviderReference;
import in.bachatsetu.backend.payment.domain.port.PaymentRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@ConditionalOnPersistenceRepositories
@Transactional(readOnly = true)
public class PaymentRepositoryAdapter implements PaymentRepository {

    private final PaymentSpringDataRepository repository;
    private final PaymentJpaMapper mapper;
    private final JpaReferenceProvider references;

    public PaymentRepositoryAdapter(
            PaymentSpringDataRepository repository,
            PaymentJpaMapper mapper,
            JpaReferenceProvider references) {
        this.repository = repository;
        this.mapper = mapper;
        this.references = references;
    }

    @Override
    public Optional<Payment> findById(AggregateId paymentId) {
        return repository.findByIdAndDeletedFalse(paymentId.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<Payment> findByReference(AggregateId tenantId, PaymentReference reference) {
        return repository.findByTenantIdAndReferenceAndDeletedFalse(tenantId.value(), reference.value())
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Payment> findByIdempotencyKey(AggregateId tenantId, IdempotencyKey idempotencyKey) {
        return repository
                .findByTenantIdAndIdempotencyKeyHashAndDeletedFalse(tenantId.value(), idempotencyKey.value())
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Payment> findByProviderReference(ProviderReference providerReference) {
        return repository.findByProviderNameAndProviderPaymentReferenceAndDeletedFalse(
                        providerReference.provider(), providerReference.transactionId())
                .map(mapper::toDomain);
    }

    @Override
    @Transactional
    public void save(Payment payment) {
        RepositoryOperations.execute(() -> {
            Optional<PaymentJpaEntity> existing = repository.findById(payment.id().value());
            PaymentJpaEntity candidate = mapper.toEntity(payment, references);
            repository.save(RepositoryOperations.preserveState(candidate, existing));
            return null;
        });
    }
}
