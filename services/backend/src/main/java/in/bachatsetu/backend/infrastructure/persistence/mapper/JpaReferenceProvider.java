package in.bachatsetu.backend.infrastructure.persistence.mapper;

import in.bachatsetu.backend.infrastructure.persistence.entity.community.DrawJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.community.GroupJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.community.MemberJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.community.MonthlyCycleJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.finance.PaymentJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.identity.UserJpaEntity;
import in.bachatsetu.backend.shared.domain.AggregateId;

public interface JpaReferenceProvider {

    UserJpaEntity user(AggregateId id);

    GroupJpaEntity group(AggregateId id);

    MemberJpaEntity member(AggregateId id);

    MonthlyCycleJpaEntity cycle(AggregateId id);

    PaymentJpaEntity payment(AggregateId id);

    DrawJpaEntity draw(AggregateId id);
}
