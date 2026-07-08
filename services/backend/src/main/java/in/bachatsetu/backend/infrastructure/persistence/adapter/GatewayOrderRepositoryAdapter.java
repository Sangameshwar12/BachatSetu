package in.bachatsetu.backend.infrastructure.persistence.adapter;

import in.bachatsetu.backend.infrastructure.persistence.entity.finance.GatewayOrderJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.mapper.GatewayOrderJpaMapper;
import in.bachatsetu.backend.infrastructure.persistence.mapper.JpaReferenceProvider;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.GatewayOrderSpringDataRepository;
import in.bachatsetu.backend.paymentgateway.domain.model.GatewayOrder;
import in.bachatsetu.backend.paymentgateway.domain.model.GatewayType;
import in.bachatsetu.backend.paymentgateway.domain.port.GatewayOrderRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@ConditionalOnPersistenceRepositories
@Transactional(readOnly = true)
public class GatewayOrderRepositoryAdapter implements GatewayOrderRepository {

    private final GatewayOrderSpringDataRepository repository;
    private final GatewayOrderJpaMapper mapper;
    private final JpaReferenceProvider references;

    public GatewayOrderRepositoryAdapter(
            GatewayOrderSpringDataRepository repository,
            GatewayOrderJpaMapper mapper,
            JpaReferenceProvider references) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
        this.references = Objects.requireNonNull(references, "reference provider must not be null");
    }

    @Override
    public Optional<GatewayOrder> findByPaymentId(AggregateId tenantId, AggregateId paymentId) {
        return repository.findByTenantIdAndPayment_IdAndDeletedFalse(tenantId.value(), paymentId.value())
                .map(mapper::toDomain);
    }

    @Override
    public Optional<GatewayOrder> findByProviderOrderId(GatewayType gatewayType, String providerOrderId) {
        return repository.findByGatewayTypeAndProviderOrderIdAndDeletedFalse(gatewayType, providerOrderId)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional
    public void save(GatewayOrder order) {
        RepositoryOperations.execute(() -> {
            Optional<GatewayOrderJpaEntity> existing = repository.findById(order.id().value());
            GatewayOrderJpaEntity candidate = mapper.toEntity(order, references);
            repository.save(RepositoryOperations.preserveState(candidate, existing));
            return null;
        });
    }
}
