package in.bachatsetu.backend.infrastructure.persistence.adapter;

import in.bachatsetu.backend.infrastructure.persistence.entity.community.DrawJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.community.GroupMemberJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.community.MonthlyCycleJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.community.SavingsGroupJpaEntity;
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
    public SavingsGroupJpaEntity group(AggregateId id) {
        return entityManager.getReference(SavingsGroupJpaEntity.class, id.value());
    }

    @Override
    public GroupMemberJpaEntity member(AggregateId id) {
        return entityManager.getReference(GroupMemberJpaEntity.class, id.value());
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
