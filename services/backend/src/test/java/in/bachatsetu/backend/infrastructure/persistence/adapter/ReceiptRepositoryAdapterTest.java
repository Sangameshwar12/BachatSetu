package in.bachatsetu.backend.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.infrastructure.persistence.entity.finance.ReceiptJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.mapper.JpaReferenceProvider;
import in.bachatsetu.backend.infrastructure.persistence.mapper.ReceiptJpaMapper;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.ReceiptSpringDataRepository;
import in.bachatsetu.backend.receipt.domain.model.Receipt;
import in.bachatsetu.backend.receipt.domain.model.ReceiptDescription;
import in.bachatsetu.backend.receipt.domain.model.ReceiptLine;
import in.bachatsetu.backend.receipt.domain.model.ReceiptNumber;
import in.bachatsetu.backend.receipt.domain.model.ReceiptType;
import in.bachatsetu.backend.receipt.domain.port.ReceiptPage;
import in.bachatsetu.backend.receipt.domain.port.ReceiptPageRequest;
import in.bachatsetu.backend.receipt.domain.port.ReceiptSortField;
import in.bachatsetu.backend.receipt.domain.port.SortDirection;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Money;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

class ReceiptRepositoryAdapterTest {

    private static final Instant NOW = Instant.parse("2026-07-07T08:00:00Z");

    private ReceiptSpringDataRepository repository;
    private ReceiptJpaMapper mapper;
    private JpaReferenceProvider references;
    private ReceiptRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        repository = mock(ReceiptSpringDataRepository.class);
        mapper = mock(ReceiptJpaMapper.class);
        references = mock(JpaReferenceProvider.class);
        adapter = new ReceiptRepositoryAdapter(repository, mapper, references);
    }

    @Test
    void findsByLegacyIdentifier() {
        AggregateId receiptId = AggregateId.newId();
        ReceiptJpaEntity entity = mock(ReceiptJpaEntity.class);
        Receipt receipt = newReceipt(receiptId);
        when(repository.findByIdAndDeletedFalse(receiptId.value())).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(receipt);

        assertThat(adapter.findById(receiptId)).contains(receipt);
    }

    @Test
    void findsByTenantScopedIdentifier() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId receiptId = AggregateId.newId();
        ReceiptJpaEntity entity = mock(ReceiptJpaEntity.class);
        Receipt receipt = newReceipt(receiptId);
        when(repository.findByTenantIdAndIdAndDeletedFalse(tenantId.value(), receiptId.value()))
                .thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(receipt);

        assertThat(adapter.findById(tenantId, receiptId)).contains(receipt);
    }

    @Test
    void reportsNoTenantScopedMatch() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId receiptId = AggregateId.newId();
        when(repository.findByTenantIdAndIdAndDeletedFalse(tenantId.value(), receiptId.value()))
                .thenReturn(Optional.empty());

        assertThat(adapter.findById(tenantId, receiptId)).isEmpty();
    }

    @Test
    void findsByNumber() {
        AggregateId tenantId = AggregateId.newId();
        ReceiptNumber number = new ReceiptNumber("RCT/20260707/00000001");
        ReceiptJpaEntity entity = mock(ReceiptJpaEntity.class);
        Receipt receipt = newReceipt(AggregateId.newId());
        when(repository.findByTenantIdAndNumberAndDeletedFalse(tenantId.value(), number.value()))
                .thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(receipt);

        assertThat(adapter.findByNumber(tenantId, number)).contains(receipt);
    }

    @Test
    void findsByPaymentId() {
        AggregateId paymentId = AggregateId.newId();
        ReceiptJpaEntity entity = mock(ReceiptJpaEntity.class);
        Receipt receipt = newReceipt(AggregateId.newId());
        when(repository.findByPayment_IdAndDeletedFalse(paymentId.value())).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(receipt);

        assertThat(adapter.findByPaymentId(paymentId)).contains(receipt);
    }

    @Test
    void findsPageAndAppliesRequestedSort() {
        AggregateId tenantId = AggregateId.newId();
        ReceiptJpaEntity entity = mock(ReceiptJpaEntity.class);
        Receipt receipt = newReceipt(AggregateId.newId());
        ReceiptPageRequest pageRequest = new ReceiptPageRequest(0, 20, ReceiptSortField.AMOUNT, SortDirection.DESC);
        when(repository.findAllByTenantIdAndDeletedFalse(eq(tenantId.value()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity), PageRequest.of(0, 20), 1));
        when(mapper.toDomain(entity)).thenReturn(receipt);

        ReceiptPage<Receipt> page = adapter.findPage(tenantId, pageRequest);

        assertThat(page.content()).containsExactly(receipt);
        assertThat(page.totalElements()).isEqualTo(1);
    }

    @Test
    void findsPageWithDefaultCreatedAtSort() {
        AggregateId tenantId = AggregateId.newId();
        ReceiptPageRequest pageRequest = new ReceiptPageRequest(1, 5, ReceiptSortField.CREATED_AT, SortDirection.ASC);
        Page<ReceiptJpaEntity> emptyPage = new PageImpl<>(List.of(), PageRequest.of(1, 5), 6);
        when(repository.findAllByTenantIdAndDeletedFalse(eq(tenantId.value()), any(Pageable.class)))
                .thenReturn(emptyPage);

        ReceiptPage<Receipt> page = adapter.findPage(tenantId, pageRequest);

        assertThat(page.page()).isEqualTo(1);
        assertThat(page.size()).isEqualTo(5);
        assertThat(page.totalElements()).isEqualTo(6);
    }

    @Test
    void savesNewAndUpdatedReceipts() {
        Receipt receipt = newReceipt(AggregateId.newId());
        ReceiptJpaEntity candidate = mock(ReceiptJpaEntity.class);
        when(repository.findById(receipt.id().value())).thenReturn(Optional.empty());
        when(mapper.toEntity(receipt, references)).thenReturn(candidate);

        adapter.save(receipt);

        verify(repository).save(candidate);
    }

    private Receipt newReceipt(AggregateId receiptId) {
        List<ReceiptLine> lines = List.of(new ReceiptLine(
                AggregateId.newId(), ReceiptType.CONTRIBUTION,
                new ReceiptDescription("Monthly contribution"), Money.inr(500_000)));
        return Receipt.generate(
                receiptId, AggregateId.newId(), AggregateId.newId(), AggregateId.newId(),
                new ReceiptNumber("RCT/20260707/00000001"), lines, AggregateId.newId(), NOW);
    }
}
