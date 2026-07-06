package in.bachatsetu.backend.group.interfaces.rest.mapper;

import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.group.application.command.ActivateGroupCommand;
import in.bachatsetu.backend.group.application.command.CloseGroupCommand;
import in.bachatsetu.backend.group.application.command.CreateSavingsGroupCommand;
import in.bachatsetu.backend.group.application.command.JoinGroupCommand;
import in.bachatsetu.backend.group.application.command.RemoveMemberCommand;
import in.bachatsetu.backend.group.application.command.SuspendGroupCommand;
import in.bachatsetu.backend.group.application.query.GroupMemberResult;
import in.bachatsetu.backend.group.application.port.GroupPage;
import in.bachatsetu.backend.group.application.port.GroupPageRequest;
import in.bachatsetu.backend.group.application.port.GroupSortField;
import in.bachatsetu.backend.group.application.query.SavingsGroupResult;
import in.bachatsetu.backend.group.application.query.SavingsGroupSummary;
import in.bachatsetu.backend.group.application.port.SortDirection;
import in.bachatsetu.backend.group.application.usecase.GetSavingsGroupUseCase;
import in.bachatsetu.backend.group.application.usecase.ListSavingsGroupsUseCase;
import in.bachatsetu.backend.group.domain.model.ContributionFrequency;
import in.bachatsetu.backend.group.domain.model.ContributionSchedule;
import in.bachatsetu.backend.group.domain.model.GroupDescription;
import in.bachatsetu.backend.group.domain.model.GroupId;
import in.bachatsetu.backend.group.domain.model.GroupName;
import in.bachatsetu.backend.group.domain.model.GroupRule;
import in.bachatsetu.backend.group.domain.model.GroupStatus;
import in.bachatsetu.backend.group.domain.model.GroupType;
import in.bachatsetu.backend.group.domain.model.MemberCapacity;
import in.bachatsetu.backend.group.domain.model.OwnerId;
import in.bachatsetu.backend.group.domain.model.PayoutMethod;
import in.bachatsetu.backend.group.interfaces.rest.dto.AddGroupMemberRequest;
import in.bachatsetu.backend.group.interfaces.rest.dto.ContributionScheduleRequest;
import in.bachatsetu.backend.group.interfaces.rest.dto.CreateSavingsGroupRequest;
import in.bachatsetu.backend.group.interfaces.rest.dto.GroupMemberResponse;
import in.bachatsetu.backend.group.interfaces.rest.dto.GroupRuleRequest;
import in.bachatsetu.backend.group.interfaces.rest.dto.MemberCapacityRequest;
import in.bachatsetu.backend.group.interfaces.rest.dto.PageResponse;
import in.bachatsetu.backend.group.interfaces.rest.dto.SavingsGroupResponse;
import in.bachatsetu.backend.group.interfaces.rest.dto.SavingsGroupSummaryResponse;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Money;
import java.util.List;
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

    public SavingsGroupResult getGroup(GetSavingsGroupUseCase useCase, AuthenticatedUser currentUser, String groupId) {
        Objects.requireNonNull(useCase, "use case must not be null");
        Objects.requireNonNull(currentUser, "current user must not be null");
        Objects.requireNonNull(groupId, "group id must not be null");
        return useCase.execute(currentUser.tenantId(), GroupId.from(groupId));
    }

    public GroupPage<SavingsGroupSummary> listGroups(
            ListSavingsGroupsUseCase useCase,
            AuthenticatedUser currentUser,
            GroupPageRequest pageRequest) {
        Objects.requireNonNull(useCase, "use case must not be null");
        Objects.requireNonNull(currentUser, "current user must not be null");
        Objects.requireNonNull(pageRequest, "page request must not be null");
        return useCase.execute(currentUser.tenantId(), pageRequest);
    }

    public GroupPageRequest toPageRequest(int page, int size, String sort, String direction, String status) {
        return new GroupPageRequest(
                page,
                size,
                toSortField(sort),
                toSortDirection(direction),
                status == null ? null : GroupStatus.valueOf(status));
    }

    public ActivateGroupCommand toActivateCommand(String groupId, AuthenticatedUser currentUser) {
        Objects.requireNonNull(groupId, "group id must not be null");
        Objects.requireNonNull(currentUser, "current user must not be null");
        return new ActivateGroupCommand(
                currentUser.tenantId(), GroupId.from(groupId), currentUser.userId().toAggregateId());
    }

    public SuspendGroupCommand toSuspendCommand(String groupId, AuthenticatedUser currentUser) {
        Objects.requireNonNull(groupId, "group id must not be null");
        Objects.requireNonNull(currentUser, "current user must not be null");
        return new SuspendGroupCommand(
                currentUser.tenantId(), GroupId.from(groupId), currentUser.userId().toAggregateId());
    }

    public CloseGroupCommand toCloseCommand(String groupId, AuthenticatedUser currentUser) {
        Objects.requireNonNull(groupId, "group id must not be null");
        Objects.requireNonNull(currentUser, "current user must not be null");
        return new CloseGroupCommand(
                currentUser.tenantId(), GroupId.from(groupId), currentUser.userId().toAggregateId());
    }

    public JoinGroupCommand toJoinCommand(
            String groupId,
            AddGroupMemberRequest request,
            AuthenticatedUser currentUser) {
        Objects.requireNonNull(groupId, "group id must not be null");
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(currentUser, "current user must not be null");
        return new JoinGroupCommand(
                currentUser.tenantId(),
                GroupId.from(groupId),
                AggregateId.from(request.memberId()),
                currentUser.userId().toAggregateId());
    }

    public RemoveMemberCommand toRemoveMemberCommand(
            String groupId,
            String memberId,
            AuthenticatedUser currentUser) {
        Objects.requireNonNull(groupId, "group id must not be null");
        Objects.requireNonNull(memberId, "member id must not be null");
        Objects.requireNonNull(currentUser, "current user must not be null");
        return new RemoveMemberCommand(
                currentUser.tenantId(),
                GroupId.from(groupId),
                AggregateId.from(memberId),
                currentUser.userId().toAggregateId());
    }

    public GroupMemberResponse toMemberResponse(SavingsGroupResult result, String memberId) {
        Objects.requireNonNull(result, "result must not be null");
        Objects.requireNonNull(memberId, "member id must not be null");
        return result.members().stream()
                .filter(member -> member.memberId().toString().equals(memberId))
                .findFirst()
                .map(this::toMemberResponse)
                .orElseThrow(() -> new IllegalStateException("member is missing from the returned group result"));
    }

    public GroupMemberResponse toMemberResponse(GroupMemberResult member) {
        Objects.requireNonNull(member, "member must not be null");
        return new GroupMemberResponse(
                member.memberId().toString(), member.joinedAt(), member.removedAt(), member.active());
    }

    public SavingsGroupSummaryResponse toSummaryResponse(SavingsGroupSummary summary) {
        Objects.requireNonNull(summary, "summary must not be null");
        return new SavingsGroupSummaryResponse(
                summary.groupId().toString(),
                summary.groupCode(),
                summary.name(),
                summary.status(),
                summary.contributionAmountPaise(),
                summary.currencyCode(),
                summary.maximumMembers(),
                summary.activeMemberCount());
    }

    public PageResponse<SavingsGroupSummaryResponse> toSummaryPage(GroupPage<SavingsGroupSummary> page) {
        Objects.requireNonNull(page, "page must not be null");
        List<SavingsGroupSummaryResponse> content = page.content().stream()
                .map(this::toSummaryResponse)
                .toList();
        return new PageResponse<>(
                content, page.page(), page.size(), page.totalElements(), page.totalPages(),
                page.hasNext(), page.hasPrevious());
    }

    private GroupSortField toSortField(String sort) {
        return switch (sort) {
            case "name" -> GroupSortField.NAME;
            case "createdAt" -> GroupSortField.CREATED_AT;
            default -> throw new IllegalArgumentException("unsupported sort field: " + sort);
        };
    }

    private SortDirection toSortDirection(String direction) {
        return switch (direction) {
            case "asc" -> SortDirection.ASC;
            case "desc" -> SortDirection.DESC;
            default -> throw new IllegalArgumentException("unsupported sort direction: " + direction);
        };
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
