package in.bachatsetu.backend.infrastructure.persistence.mapper;

import in.bachatsetu.backend.group.domain.model.ContributionSchedule;
import in.bachatsetu.backend.group.domain.model.GroupCode;
import in.bachatsetu.backend.group.domain.model.GroupName;
import in.bachatsetu.backend.group.domain.model.GroupRule;
import in.bachatsetu.backend.group.domain.model.MemberCapacity;
import in.bachatsetu.backend.group.domain.model.SavingsGroup;
import in.bachatsetu.backend.infrastructure.persistence.entity.community.GroupJpaEntity;
import in.bachatsetu.backend.shared.domain.Money;
import org.mapstruct.Context;
import org.mapstruct.Mapper;

@Mapper(config = PersistenceMapperConfiguration.class)
public interface GroupJpaMapper {

    default SavingsGroup toDomain(GroupJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        GroupRule rule = new GroupRule(
                JpaMappingSupport.id(entity.getId()),
                new ContributionSchedule(
                        new Money(
                                entity.getContributionAmountPaise(),
                                JpaMappingSupport.currency(entity.getCurrencyCode())),
                        entity.getFrequency(),
                        entity.getStartDate(),
                        entity.getDurationCycles()),
                new MemberCapacity(entity.getMinimumMembers(), entity.getMaximumMembers()),
                entity.getPayoutMethod(),
                entity.isPartialPaymentAllowed());
        return new SavingsGroup(
                JpaMappingSupport.id(entity.getId()),
                JpaMappingSupport.id(entity.getTenantId()),
                JpaMappingSupport.id(entity.getOrganizer().getId()),
                new GroupCode(entity.getCode()),
                new GroupName(entity.getName()),
                entity.getType(),
                rule,
                entity.getStatus(),
                JpaMappingSupport.auditInfo(entity),
                entity.getVersion());
    }

    default GroupJpaEntity toEntity(SavingsGroup domain, @Context JpaReferenceProvider references) {
        if (domain == null) {
            return null;
        }
        ContributionSchedule schedule = domain.rule().contributionSchedule();
        MemberCapacity capacity = domain.rule().memberCapacity();
        return new GroupJpaEntity(
                domain.id().value(),
                domain.tenantId().value(),
                references.user(domain.organizerId()),
                domain.code().value(),
                domain.name().value(),
                domain.type(),
                domain.status(),
                schedule.contributionAmount().minorUnits(),
                schedule.contributionAmount().currency().getCurrencyCode(),
                schedule.frequency(),
                schedule.startDate(),
                schedule.cycleCount(),
                capacity.minimum(),
                capacity.maximum(),
                domain.rule().payoutMethod(),
                domain.rule().partialPaymentsAllowed());
    }
}
