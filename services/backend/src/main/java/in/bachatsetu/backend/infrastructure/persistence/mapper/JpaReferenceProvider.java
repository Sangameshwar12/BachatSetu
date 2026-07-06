package in.bachatsetu.backend.infrastructure.persistence.mapper;

import in.bachatsetu.backend.infrastructure.persistence.entity.community.DrawJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.community.GroupMemberJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.community.MonthlyCycleJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.community.SavingsGroupJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.finance.PaymentJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.identity.UserJpaEntity;
import in.bachatsetu.backend.shared.domain.AggregateId;

public interface JpaReferenceProvider {

    UserJpaEntity user(AggregateId id);

    SavingsGroupJpaEntity group(AggregateId id);

    GroupMemberJpaEntity member(AggregateId id);

    MonthlyCycleJpaEntity cycle(AggregateId id);

    PaymentJpaEntity payment(AggregateId id);

    DrawJpaEntity draw(AggregateId id);
}
