package in.bachatsetu.backend.infrastructure.persistence.adapter;

import in.bachatsetu.backend.infrastructure.persistence.entity.finance.PaymentJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.mapper.JpaReferenceProvider;
import in.bachatsetu.backend.infrastructure.persistence.mapper.PaymentJpaMapper;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.PaymentSpringDataRepository;
import in.bachatsetu.backend.payment.domain.model.IdempotencyKey;
import in.bachatsetu.backend.payment.domain.model.Payment;
import in.bachatsetu.backend.payment.domain.model.PaymentReference;
import in.bachatsetu.backend.payment.domain.model.PaymentStatus;
import in.bachatsetu.backend.payment.domain.model.ProviderReference;
import in.bachatsetu.backend.payment.domain.port.PaymentPage;
import in.bachatsetu.backend.payment.domain.port.PaymentPageRequest;
import in.bachatsetu.backend.payment.domain.port.PaymentRepository;
import in.bachatsetu.backend.payment.domain.port.PaymentSortField;
import in.bachatsetu.backend.payment.domain.port.SortDirection;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    public Optional<Payment> findById(AggregateId tenantId, AggregateId paymentId) {
        return repository.findByTenantIdAndIdAndDeletedFalse(tenantId.value(), paymentId.value())
                .map(mapper::toDomain);
    }

    @Override
    public PaymentPage<Payment> findPage(AggregateId tenantId, PaymentPageRequest pageRequest) {
        Pageable pageable = PageRequest.of(pageRequest.page(), pageRequest.size(), toSort(pageRequest));
        Page<PaymentJpaEntity> page = repository.findAllByTenantIdAndDeletedFalse(tenantId.value(), pageable);
        List<Payment> content = page.getContent().stream().map(mapper::toDomain).toList();
        return new PaymentPage<>(content, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    private Sort toSort(PaymentPageRequest pageRequest) {
        String property = toSortProperty(pageRequest.sortField());
        Sort.Direction direction = pageRequest.direction() == SortDirection.DESC
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return Sort.by(direction, property);
    }

    private String toSortProperty(PaymentSortField sortField) {
        if (sortField == PaymentSortField.AMOUNT) {
            return "amountPaise";
        }
        return "createdAt";
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
    public Optional<Payment> findLatestByGroupAndMember(
            AggregateId tenantId, AggregateId groupId, AggregateId memberId) {
        return repository.findFirstByTenantIdAndGroup_IdAndPayer_IdAndDeletedFalseOrderByCreatedAtDesc(
                        tenantId.value(), groupId.value(), memberId.value())
                .map(mapper::toDomain);
    }

    @Override
    public List<Payment> findVerifiedByGroupWithinWindow(
            AggregateId tenantId, AggregateId groupId, Instant from, Instant to) {
        return repository
                .findAllByTenantIdAndGroup_IdAndStatusAndCreatedAtBetweenAndDeletedFalse(
                        tenantId.value(), groupId.value(), PaymentStatus.VERIFIED, from, to)
                .stream()
                .map(mapper::toDomain)
                .toList();
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
