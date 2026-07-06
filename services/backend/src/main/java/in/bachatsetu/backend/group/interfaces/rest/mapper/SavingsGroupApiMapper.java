package in.bachatsetu.backend.group.interfaces.rest.mapper;

import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.group.application.command.CreateSavingsGroupCommand;
import in.bachatsetu.backend.group.application.query.SavingsGroupResult;
import in.bachatsetu.backend.group.domain.model.ContributionFrequency;
import in.bachatsetu.backend.group.domain.model.ContributionSchedule;
import in.bachatsetu.backend.group.domain.model.GroupDescription;
import in.bachatsetu.backend.group.domain.model.GroupName;
import in.bachatsetu.backend.group.domain.model.GroupRule;
import in.bachatsetu.backend.group.domain.model.GroupType;
import in.bachatsetu.backend.group.domain.model.MemberCapacity;
import in.bachatsetu.backend.group.domain.model.OwnerId;
import in.bachatsetu.backend.group.domain.model.PayoutMethod;
import in.bachatsetu.backend.group.interfaces.rest.dto.ContributionScheduleRequest;
import in.bachatsetu.backend.group.interfaces.rest.dto.CreateSavingsGroupRequest;
import in.bachatsetu.backend.group.interfaces.rest.dto.GroupRuleRequest;
import in.bachatsetu.backend.group.interfaces.rest.dto.MemberCapacityRequest;
import in.bachatsetu.backend.group.interfaces.rest.dto.SavingsGroupResponse;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Money;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Maps validated HTTP contracts to Savings Group application commands and safe responses. */
@Component
public class SavingsGroupApiMapper {

    public CreateSavingsGroupCommand toCommand(CreateSavingsGroupRequest request, AuthenticatedUser currentUser) {
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(currentUser, "current user must not be null");
        OwnerId ownerId = new OwnerId(currentUser.userId().toAggregateId());
        return new CreateSavingsGroupCommand(
                currentUser.tenantId(),
                ownerId,
                new GroupName(request.name()),
                Optional.ofNullable(request.description())
                        .map(GroupDescription::new)
                        .orElseGet(GroupDescription::empty),
                GroupType.valueOf(request.type()),
                toRule(request.rule()));
    }

    public SavingsGroupResponse toResponse(SavingsGroupResult result) {
        Objects.requireNonNull(result, "result must not be null");
        return new SavingsGroupResponse(
                result.groupId().toString(),
                result.tenantId().toString(),
                result.ownerId().toString(),
                result.groupCode(),
                result.name(),
                result.description(),
                result.type(),
                result.status(),
                result.contributionAmountPaise(),
                result.currencyCode(),
                result.maximumMembers(),
                result.activeMemberCount(),
                result.createdAt(),
                result.updatedAt(),
                result.version());
    }

    private GroupRule toRule(GroupRuleRequest request) {
        ContributionScheduleRequest schedule = request.contributionSchedule();
        MemberCapacityRequest capacity = request.memberCapacity();
        return new GroupRule(
                AggregateId.newId(),
                new ContributionSchedule(
                        Money.inr(schedule.contributionAmountPaise()),
                        ContributionFrequency.valueOf(schedule.frequency()),
                        schedule.startDate(),
                        schedule.cycleCount()),
                new MemberCapacity(capacity.minimum(), capacity.maximum()),
                PayoutMethod.valueOf(request.payoutMethod()),
                request.partialPaymentsAllowed());
    }
}
