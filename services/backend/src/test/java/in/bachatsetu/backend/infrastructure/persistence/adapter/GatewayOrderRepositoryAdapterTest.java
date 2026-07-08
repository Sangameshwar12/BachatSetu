package in.bachatsetu.backend.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.infrastructure.persistence.entity.finance.GatewayOrderJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.mapper.GatewayOrderJpaMapper;
import in.bachatsetu.backend.infrastructure.persistence.mapper.JpaReferenceProvider;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.GatewayOrderSpringDataRepository;
import in.bachatsetu.backend.paymentgateway.domain.model.GatewayOrder;
import in.bachatsetu.backend.paymentgateway.domain.model.GatewayType;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GatewayOrderRepositoryAdapterTest {

    private static final Instant NOW = Instant.parse("2026-07-08T08:00:00Z");

    private GatewayOrderSpringDataRepository repository;
    private GatewayOrderJpaMapper mapper;
    private JpaReferenceProvider references;
    private GatewayOrderRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        repository = mock(GatewayOrderSpringDataRepository.class);
        mapper = mock(GatewayOrderJpaMapper.class);
        references = mock(JpaReferenceProvider.class);
        adapter = new GatewayOrderRepositoryAdapter(repository, mapper, references);
    }

    @Test
    void findsByPaymentId() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId paymentId = AggregateId.newId();
        GatewayOrderJpaEntity entity = mock(GatewayOrderJpaEntity.class);
        GatewayOrder order = newOrder(paymentId);
        when(repository.findByTenantIdAndPayment_IdAndDeletedFalse(tenantId.value(), paymentId.value()))
                .thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(order);

        assertThat(adapter.findByPaymentId(tenantId, paymentId)).contains(order);
    }

    @Test
    void reportsNoMatchByPaymentId() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId paymentId = AggregateId.newId();
        when(repository.findByTenantIdAndPayment_IdAndDeletedFalse(tenantId.value(), paymentId.value()))
                .thenReturn(Optional.empty());

        assertThat(adapter.findByPaymentId(tenantId, paymentId)).isEmpty();
    }

    @Test
    void findsByProviderOrderIdAcrossTenants() {
        GatewayOrderJpaEntity entity = mock(GatewayOrderJpaEntity.class);
        GatewayOrder order = newOrder(AggregateId.newId());
        when(repository.findByGatewayTypeAndProviderOrderIdAndDeletedFalse(GatewayType.RAZORPAY, "order_1"))
                .thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(order);

        assertThat(adapter.findByProviderOrderId(GatewayType.RAZORPAY, "order_1")).contains(order);
    }

    @Test
    void savesNewAndUpdatedOrders() {
        GatewayOrder order = newOrder(AggregateId.newId());
        GatewayOrderJpaEntity candidate = mock(GatewayOrderJpaEntity.class);
        when(repository.findById(order.id().value())).thenReturn(Optional.empty());
        when(mapper.toEntity(order, references)).thenReturn(candidate);

        adapter.save(order);

        verify(repository).save(candidate);
    }

    private GatewayOrder newOrder(AggregateId paymentId) {
        return GatewayOrder.create(
                AggregateId.newId(), AggregateId.newId(), paymentId, GatewayType.RAZORPAY, "order_1", "link",
                AggregateId.newId(), NOW);
    }
}
