package in.bachatsetu.backend.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.infrastructure.persistence.entity.finance.PaymentJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.mapper.JpaReferenceProvider;
import in.bachatsetu.backend.infrastructure.persistence.mapper.PaymentJpaMapper;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.PaymentSpringDataRepository;
import in.bachatsetu.backend.payment.domain.model.IdempotencyKey;
import in.bachatsetu.backend.payment.domain.model.Payment;
import in.bachatsetu.backend.payment.domain.model.PaymentMethod;
import in.bachatsetu.backend.payment.domain.model.PaymentReference;
import in.bachatsetu.backend.payment.domain.model.ProviderReference;
import in.bachatsetu.backend.payment.domain.port.PaymentPage;
import in.bachatsetu.backend.payment.domain.port.PaymentPageRequest;
import in.bachatsetu.backend.payment.domain.port.PaymentSortField;
import in.bachatsetu.backend.payment.domain.port.SortDirection;
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

class PaymentRepositoryAdapterTest {

    private static final Instant NOW = Instant.parse("2026-07-06T08:00:00Z");

    private PaymentSpringDataRepository repository;
    private PaymentJpaMapper mapper;
    private JpaReferenceProvider references;
    private PaymentRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        repository = mock(PaymentSpringDataRepository.class);
        mapper = mock(PaymentJpaMapper.class);
        references = mock(JpaReferenceProvider.class);
        adapter = new PaymentRepositoryAdapter(repository, mapper, references);
    }

    @Test
    void findsByLegacyIdentifier() {
        AggregateId paymentId = AggregateId.newId();
        PaymentJpaEntity entity = mock(PaymentJpaEntity.class);
        Payment payment = newPayment(paymentId);
        when(repository.findByIdAndDeletedFalse(paymentId.value())).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(payment);

        assertThat(adapter.findById(paymentId)).contains(payment);
    }

    @Test
    void findsByTenantScopedIdentifier() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId paymentId = AggregateId.newId();
        PaymentJpaEntity entity = mock(PaymentJpaEntity.class);
        Payment payment = newPayment(paymentId);
        when(repository.findByTenantIdAndIdAndDeletedFalse(tenantId.value(), paymentId.value()))
                .thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(payment);

        assertThat(adapter.findById(tenantId, paymentId)).contains(payment);
    }

    @Test
    void reportsNoTenantScopedMatch() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId paymentId = AggregateId.newId();
        when(repository.findByTenantIdAndIdAndDeletedFalse(tenantId.value(), paymentId.value()))
                .thenReturn(Optional.empty());

        assertThat(adapter.findById(tenantId, paymentId)).isEmpty();
    }

    @Test
    void findsByReference() {
        AggregateId tenantId = AggregateId.newId();
        PaymentReference reference = new PaymentReference("PAY-12345678");
        PaymentJpaEntity entity = mock(PaymentJpaEntity.class);
        Payment payment = newPayment(AggregateId.newId());
        when(repository.findByTenantIdAndReferenceAndDeletedFalse(tenantId.value(), reference.value()))
                .thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(payment);

        assertThat(adapter.findByReference(tenantId, reference)).contains(payment);
    }

    @Test
    void findsByIdempotencyKey() {
        AggregateId tenantId = AggregateId.newId();
        IdempotencyKey idempotencyKey = new IdempotencyKey("checkout-attempt-0001");
        PaymentJpaEntity entity = mock(PaymentJpaEntity.class);
        Payment payment = newPayment(AggregateId.newId());
        when(repository.findByTenantIdAndIdempotencyKeyHashAndDeletedFalse(tenantId.value(), idempotencyKey.value()))
                .thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(payment);

        assertThat(adapter.findByIdempotencyKey(tenantId, idempotencyKey)).contains(payment);
    }

    @Test
    void findsByProviderReference() {
        ProviderReference providerReference = new ProviderReference("test-provider", "txn-001");
        PaymentJpaEntity entity = mock(PaymentJpaEntity.class);
        Payment payment = newPayment(AggregateId.newId());
        when(repository.findByProviderNameAndProviderPaymentReferenceAndDeletedFalse("test-provider", "txn-001"))
                .thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(payment);

        assertThat(adapter.findByProviderReference(providerReference)).contains(payment);
    }

    @Test
    void findsPageAndAppliesRequestedSort() {
        AggregateId tenantId = AggregateId.newId();
        PaymentJpaEntity entity = mock(PaymentJpaEntity.class);
        Payment payment = newPayment(AggregateId.newId());
        PaymentPageRequest pageRequest = new PaymentPageRequest(0, 20, PaymentSortField.AMOUNT, SortDirection.DESC);
        when(repository.findAllByTenantIdAndDeletedFalse(eq(tenantId.value()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity), PageRequest.of(0, 20), 1));
        when(mapper.toDomain(entity)).thenReturn(payment);

        PaymentPage<Payment> page = adapter.findPage(tenantId, pageRequest);

        assertThat(page.content()).containsExactly(payment);
        assertThat(page.totalElements()).isEqualTo(1);
    }

    @Test
    void findsPageWithDefaultCreatedAtSort() {
        AggregateId tenantId = AggregateId.newId();
        PaymentPageRequest pageRequest = new PaymentPageRequest(1, 5, PaymentSortField.CREATED_AT, SortDirection.ASC);
        Page<PaymentJpaEntity> emptyPage = new PageImpl<>(List.of(), PageRequest.of(1, 5), 6);
        when(repository.findAllByTenantIdAndDeletedFalse(eq(tenantId.value()), any(Pageable.class)))
                .thenReturn(emptyPage);

        PaymentPage<Payment> page = adapter.findPage(tenantId, pageRequest);

        assertThat(page.page()).isEqualTo(1);
        assertThat(page.size()).isEqualTo(5);
        assertThat(page.totalElements()).isEqualTo(6);
    }

    @Test
    void savesNewAndUpdatedPayments() {
        Payment payment = newPayment(AggregateId.newId());
        PaymentJpaEntity candidate = mock(PaymentJpaEntity.class);
        when(repository.findById(payment.id().value())).thenReturn(Optional.empty());
        when(mapper.toEntity(payment, references)).thenReturn(candidate);

        adapter.save(payment);

        verify(repository).save(candidate);
    }

    private Payment newPayment(AggregateId paymentId) {
        return Payment.initiate(
                paymentId,
                AggregateId.newId(),
                AggregateId.newId(),
                AggregateId.newId(),
                new PaymentReference("PAY-12345678"),
                new IdempotencyKey("checkout-attempt-0001"),
                Money.inr(100_000),
                PaymentMethod.UPI,
                AggregateId.newId(),
                NOW);
    }
}
