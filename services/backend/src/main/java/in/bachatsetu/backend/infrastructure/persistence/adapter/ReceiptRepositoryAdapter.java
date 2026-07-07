package in.bachatsetu.backend.infrastructure.persistence.adapter;

import in.bachatsetu.backend.infrastructure.persistence.entity.finance.ReceiptJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.mapper.JpaReferenceProvider;
import in.bachatsetu.backend.infrastructure.persistence.mapper.ReceiptJpaMapper;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.ReceiptSpringDataRepository;
import in.bachatsetu.backend.receipt.domain.model.Receipt;
import in.bachatsetu.backend.receipt.domain.model.ReceiptNumber;
import in.bachatsetu.backend.receipt.domain.port.ReceiptPage;
import in.bachatsetu.backend.receipt.domain.port.ReceiptPageRequest;
import in.bachatsetu.backend.receipt.domain.port.ReceiptRepository;
import in.bachatsetu.backend.receipt.domain.port.ReceiptSortField;
import in.bachatsetu.backend.receipt.domain.port.SortDirection;
import in.bachatsetu.backend.shared.domain.AggregateId;
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
public class ReceiptRepositoryAdapter implements ReceiptRepository {

    private final ReceiptSpringDataRepository repository;
    private final ReceiptJpaMapper mapper;
    private final JpaReferenceProvider references;

    public ReceiptRepositoryAdapter(
            ReceiptSpringDataRepository repository,
            ReceiptJpaMapper mapper,
            JpaReferenceProvider references) {
        this.repository = repository;
        this.mapper = mapper;
        this.references = references;
    }

    @Override
    public Optional<Receipt> findById(AggregateId receiptId) {
        return repository.findByIdAndDeletedFalse(receiptId.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<Receipt> findById(AggregateId tenantId, AggregateId receiptId) {
        return repository.findByTenantIdAndIdAndDeletedFalse(tenantId.value(), receiptId.value())
                .map(mapper::toDomain);
    }

    @Override
    public ReceiptPage<Receipt> findPage(AggregateId tenantId, ReceiptPageRequest pageRequest) {
        Pageable pageable = PageRequest.of(pageRequest.page(), pageRequest.size(), toSort(pageRequest));
        Page<ReceiptJpaEntity> page = repository.findAllByTenantIdAndDeletedFalse(tenantId.value(), pageable);
        List<Receipt> content = page.getContent().stream().map(mapper::toDomain).toList();
        return new ReceiptPage<>(content, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    private Sort toSort(ReceiptPageRequest pageRequest) {
        String property = toSortProperty(pageRequest.sortField());
        Sort.Direction direction = pageRequest.direction() == SortDirection.DESC
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return Sort.by(direction, property);
    }

    private String toSortProperty(ReceiptSortField sortField) {
        if (sortField == ReceiptSortField.AMOUNT) {
            return "amountPaise";
        }
        return "createdAt";
    }

    @Override
    public Optional<Receipt> findByNumber(AggregateId tenantId, ReceiptNumber number) {
        return repository.findByTenantIdAndNumberAndDeletedFalse(tenantId.value(), number.value())
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Receipt> findByPaymentId(AggregateId paymentId) {
        return repository.findByPayment_IdAndDeletedFalse(paymentId.value()).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public void save(Receipt receipt) {
        RepositoryOperations.execute(() -> {
            Optional<ReceiptJpaEntity> existing = repository.findById(receipt.id().value());
            ReceiptJpaEntity candidate = mapper.toEntity(receipt, references);
            repository.save(RepositoryOperations.preserveState(candidate, existing));
            return null;
        });
    }
}
