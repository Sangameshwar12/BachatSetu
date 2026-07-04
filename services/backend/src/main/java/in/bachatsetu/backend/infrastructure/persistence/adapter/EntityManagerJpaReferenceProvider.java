package in.bachatsetu.backend.infrastructure.persistence.adapter;

import in.bachatsetu.backend.infrastructure.persistence.entity.community.DrawJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.community.GroupJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.community.MemberJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.community.MonthlyCycleJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.finance.PaymentJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.identity.UserJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.mapper.JpaReferenceProvider;
import in.bachatsetu.backend.shared.domain.AggregateId;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnPersistenceRepositories
public class EntityManagerJpaReferenceProvider implements JpaReferenceProvider {

    private final EntityManager entityManager;

    public EntityManagerJpaReferenceProvider(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public UserJpaEntity user(AggregateId id) {
        return entityManager.getReference(UserJpaEntity.class, id.value());
    }

    @Override
    public GroupJpaEntity group(AggregateId id) {
        return entityManager.getReference(GroupJpaEntity.class, id.value());
    }

    @Override
    public MemberJpaEntity member(AggregateId id) {
        return entityManager.getReference(MemberJpaEntity.class, id.value());
    }

    @Override
    public MonthlyCycleJpaEntity cycle(AggregateId id) {
        return entityManager.getReference(MonthlyCycleJpaEntity.class, id.value());
    }

    @Override
    public PaymentJpaEntity payment(AggregateId id) {
        return entityManager.getReference(PaymentJpaEntity.class, id.value());
    }

    @Override
    public DrawJpaEntity draw(AggregateId id) {
        return entityManager.getReference(DrawJpaEntity.class, id.value());
    }
}
