package in.bachatsetu.backend.group.domain.factory;

import in.bachatsetu.backend.group.domain.model.ContributionAmount;
import in.bachatsetu.backend.group.domain.model.CreatedAt;
import in.bachatsetu.backend.group.domain.model.GroupDescription;
import in.bachatsetu.backend.group.domain.model.GroupId;
import in.bachatsetu.backend.group.domain.model.GroupName;
import in.bachatsetu.backend.group.domain.model.GroupRule;
import in.bachatsetu.backend.group.domain.model.GroupType;
import in.bachatsetu.backend.group.domain.model.MaximumMembers;
import in.bachatsetu.backend.group.domain.model.OwnerId;
import in.bachatsetu.backend.group.domain.model.SavingsGroup;
import in.bachatsetu.backend.group.domain.service.GroupCodeGenerator;
import in.bachatsetu.backend.group.domain.service.GroupValidationService;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

/** Creates valid savings group aggregates and assigns their identity and group code. */
public final class SavingsGroupFactory {

    private final GroupCodeGenerator codeGenerator;
    private final GroupValidationService validationService;

    public SavingsGroupFactory(
            GroupCodeGenerator codeGenerator,
            GroupValidationService validationService) {
        this.codeGenerator = Objects.requireNonNull(codeGenerator, "code generator must not be null");
        this.validationService = Objects.requireNonNull(validationService, "validation service must not be null");
    }

    public SavingsGroup create(
            AggregateId tenantId,
            OwnerId ownerId,
            GroupName name,
            GroupDescription description,
            GroupType type,
            GroupRule rule,
            CreatedAt createdAt) {
        GroupId groupId = GroupId.newId();
        ContributionAmount contributionAmount = new ContributionAmount(
                Objects.requireNonNull(rule, "group rule must not be null")
                        .contributionSchedule()
                        .contributionAmount());
        MaximumMembers maximumMembers = new MaximumMembers(rule.memberCapacity().maximum());
        validationService.validateCreation(ownerId, contributionAmount, maximumMembers, rule);
        return SavingsGroup.create(
                groupId,
                tenantId,
                ownerId,
                codeGenerator.generate(groupId),
                name,
                description,
                type,
                rule,
                createdAt);
    }
}
