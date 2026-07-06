package in.bachatsetu.backend.group.interfaces.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.group.application.command.ActivateGroupCommand;
import in.bachatsetu.backend.group.application.command.CreateSavingsGroupCommand;
import in.bachatsetu.backend.group.application.command.JoinGroupCommand;
import in.bachatsetu.backend.group.application.command.RemoveMemberCommand;
import in.bachatsetu.backend.group.application.query.GroupMemberResult;
import in.bachatsetu.backend.group.application.port.GroupPage;
import in.bachatsetu.backend.group.application.port.GroupPageRequest;
import in.bachatsetu.backend.group.application.port.GroupSortField;
import in.bachatsetu.backend.group.application.query.SavingsGroupResult;
import in.bachatsetu.backend.group.application.query.SavingsGroupSummary;
import in.bachatsetu.backend.group.application.port.SortDirection;
import in.bachatsetu.backend.group.application.usecase.GetSavingsGroupUseCase;
import in.bachatsetu.backend.group.application.usecase.ListSavingsGroupsUseCase;
import in.bachatsetu.backend.group.domain.model.GroupId;
import in.bachatsetu.backend.group.domain.model.GroupStatus;
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
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SavingsGroupApiMapperTest {

    private final SavingsGroupApiMapper mapper = new SavingsGroupApiMapper();

    @Test
    void mapsRequestToCommandUsingAuthenticatedIdentityForTenantAndOwner() {
        AuthenticatedUser currentUser = new AuthenticatedUser(
                UserId.newId(),
                MobileNumber.of("+919876543210"),
                AggregateId.newId(),
                Set.of("GROUP_MEMBER"),
                Set.of("group.read"));
        CreateSavingsGroupRequest request = new CreateSavingsGroupRequest(
                "Sunrise Bhishi Circle",
                "Monthly society savings",
                "BHISHI",
                new GroupRuleRequest(
                        new ContributionScheduleRequest(500_000L, "MONTHLY", LocalDate.of(2026, 8, 1), 12),
                        new MemberCapacityRequest(2, 10),
                        "RANDOM_DRAW",
                        false));

        CreateSavingsGroupCommand command = mapper.toCommand(request, currentUser);

        assertThat(command.tenantId()).isEqualTo(currentUser.tenantId());
        assertThat(command.ownerId().value()).isEqualTo(currentUser.userId().toAggregateId());
        assertThat(command.name().value()).isEqualTo("Sunrise Bhishi Circle");
        assertThat(command.description().value()).isEqualTo("Monthly society savings");
        assertThat(command.type().name()).isEqualTo("BHISHI");
        assertThat(command.rule().contributionSchedule().contributionAmount().minorUnits()).isEqualTo(500_000L);
        assertThat(command.rule().memberCapacity().minimum()).isEqualTo(2);
        assertThat(command.rule().memberCapacity().maximum()).isEqualTo(10);
        assertThat(command.rule().payoutMethod().name()).isEqualTo("RANDOM_DRAW");
    }

    @Test
    void defaultsMissingDescriptionToEmpty() {
        AuthenticatedUser currentUser = new AuthenticatedUser(
                UserId.newId(),
                MobileNumber.of("+919876543210"),
                AggregateId.newId(),
                Set.of(),
                Set.of());
        CreateSavingsGroupRequest request = new CreateSavingsGroupRequest(
                "Sunrise Bhishi Circle",
                null,
                "BHISHI",
                new GroupRuleRequest(
                        new ContributionScheduleRequest(500_000L, "MONTHLY", LocalDate.of(2026, 8, 1), 12),
                        new MemberCapacityRequest(2, 10),
                        "RANDOM_DRAW",
                        false));

        CreateSavingsGroupCommand command = mapper.toCommand(request, currentUser);

        assertThat(command.description().value()).isEmpty();
    }

    @Test
    void mapsResultToResponse() {
        SavingsGroupResult result = new SavingsGroupResult(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "BS-1A2B3C4D5E6F7A8B",
                "Sunrise Bhishi Circle",
                "Monthly society savings",
                "BHISHI",
                "INACTIVE",
                500_000L,
                "INR",
                10,
                1,
                Instant.parse("2026-07-06T08:00:00Z"),
                Instant.parse("2026-07-06T08:00:00Z"),
                0,
                List.of(new GroupMemberResult(UUID.randomUUID(), Instant.parse("2026-07-06T08:00:00Z"), null, true)));

        SavingsGroupResponse response = mapper.toResponse(result);

        assertThat(response.groupId()).isEqualTo(result.groupId().toString());
        assertThat(response.groupCode()).isEqualTo("BS-1A2B3C4D5E6F7A8B");
        assertThat(response.contributionAmountPaise()).isEqualTo(500_000L);
        assertThat(response.currencyCode()).isEqualTo("INR");
        assertThat(response.status()).isEqualTo("INACTIVE");
        assertThat(response.version()).isZero();
    }

    @Test
    void mapsLifecycleCommandsUsingAuthenticatedIdentity() {
        AuthenticatedUser currentUser = authenticatedUser();
        GroupId groupId = GroupId.newId();

        ActivateGroupCommand activate = mapper.toActivateCommand(groupId.value().toString(), currentUser);
        assertThat(activate.tenantId()).isEqualTo(currentUser.tenantId());
        assertThat(activate.groupId()).isEqualTo(groupId);
        assertThat(activate.actorId()).isEqualTo(currentUser.userId().toAggregateId());

        assertThat(mapper.toSuspendCommand(groupId.value().toString(), currentUser).tenantId())
                .isEqualTo(currentUser.tenantId());
        assertThat(mapper.toCloseCommand(groupId.value().toString(), currentUser).tenantId())
                .isEqualTo(currentUser.tenantId());
    }

    @Test
    void mapsJoinAndRemoveMemberCommands() {
        AuthenticatedUser currentUser = authenticatedUser();
        GroupId groupId = GroupId.newId();
        AggregateId memberId = AggregateId.newId();

        JoinGroupCommand join = mapper.toJoinCommand(
                groupId.value().toString(), new AddGroupMemberRequest(memberId.toString()), currentUser);
        assertThat(join.tenantId()).isEqualTo(currentUser.tenantId());
        assertThat(join.groupId()).isEqualTo(groupId);
        assertThat(join.memberId()).isEqualTo(memberId);
        assertThat(join.actorId()).isEqualTo(currentUser.userId().toAggregateId());

        RemoveMemberCommand remove = mapper.toRemoveMemberCommand(
                groupId.value().toString(), memberId.toString(), currentUser);
        assertThat(remove.memberId()).isEqualTo(memberId);
        assertThat(remove.actorId()).isEqualTo(currentUser.userId().toAggregateId());
    }

    @Test
    void findsMemberWithinGroupResult() {
        UUID memberId = UUID.randomUUID();
        Instant now = Instant.parse("2026-07-06T08:00:00Z");
        SavingsGroupResult result = new SavingsGroupResult(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "BS-1A2B3C4D5E6F7A8B",
                "Sunrise Bhishi Circle", "Monthly society savings", "BHISHI", "ACTIVE", 500_000L, "INR", 10, 1,
                now, now, 0, List.of(new GroupMemberResult(memberId, now, null, true)));

        GroupMemberResponse response = mapper.toMemberResponse(result, memberId.toString());

        assertThat(response.memberId()).isEqualTo(memberId.toString());
        assertThat(response.active()).isTrue();
        assertThat(response.removedAt()).isNull();
    }

    @Test
    void rejectsMemberMissingFromGroupResult() {
        Instant now = Instant.parse("2026-07-06T08:00:00Z");
        SavingsGroupResult result = new SavingsGroupResult(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "BS-1A2B3C4D5E6F7A8B",
                "Sunrise Bhishi Circle", "Monthly society savings", "BHISHI", "ACTIVE", 500_000L, "INR", 10, 1,
                now, now, 0, List.of(new GroupMemberResult(UUID.randomUUID(), now, null, true)));
        String missingMemberId = UUID.randomUUID().toString();

        assertThat(catchThrowable(() -> mapper.toMemberResponse(result, missingMemberId)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getGroupDelegatesToUseCaseWithParsedIdentifiers() {
        AuthenticatedUser currentUser = authenticatedUser();
        GroupId groupId = GroupId.newId();
        Instant now = Instant.parse("2026-07-06T08:00:00Z");
        SavingsGroupResult expected = new SavingsGroupResult(
                groupId.value().value(), currentUser.tenantId().value(), UUID.randomUUID(), "BS-1A2B3C4D5E6F7A8B",
                "Sunrise Bhishi Circle", "Monthly society savings", "BHISHI", "ACTIVE", 500_000L, "INR", 10, 1,
                now, now, 0, List.of());
        GetSavingsGroupUseCase useCase =
                (tenantId, id) -> {
                    assertThat(tenantId).isEqualTo(currentUser.tenantId());
                    assertThat(id).isEqualTo(groupId);
                    return expected;
                };

        assertThat(mapper.getGroup(useCase, currentUser, groupId.value().toString())).isEqualTo(expected);
    }

    @Test
    void listGroupsDelegatesToUseCaseWithTenantIdentityAndPageRequest() {
        AuthenticatedUser currentUser = authenticatedUser();
        GroupPageRequest pageRequest = new GroupPageRequest(0, 20, GroupSortField.CREATED_AT, SortDirection.ASC, null);
        GroupPage<SavingsGroupSummary> expected = new GroupPage<>(List.of(summary()), 0, 20, 1);
        ListSavingsGroupsUseCase useCase = (tenantId, request) -> {
            assertThat(tenantId).isEqualTo(currentUser.tenantId());
            assertThat(request).isEqualTo(pageRequest);
            return expected;
        };

        assertThat(mapper.listGroups(useCase, currentUser, pageRequest)).isEqualTo(expected);
    }

    @Test
    void mapsSummaryToResponse() {
        SavingsGroupSummary summary = new SavingsGroupSummary(
                UUID.randomUUID(), "BS-1A2B3C4D5E6F7A8B", "Sunrise Bhishi Circle", "ACTIVE", 500_000L, "INR", 10, 3);

        SavingsGroupSummaryResponse response = mapper.toSummaryResponse(summary);

        assertThat(response.groupId()).isEqualTo(summary.groupId().toString());
        assertThat(response.activeMemberCount()).isEqualTo(3);
    }

    @Test
    void mapsGroupPageToPageResponse() {
        GroupPage<SavingsGroupSummary> page = new GroupPage<>(List.of(summary(), summary()), 0, 2, 3);

        PageResponse<SavingsGroupSummaryResponse> response = mapper.toSummaryPage(page);

        assertThat(response.content()).hasSize(2);
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.totalElements()).isEqualTo(3);
        assertThat(response.totalPages()).isEqualTo(2);
        assertThat(response.hasNext()).isTrue();
        assertThat(response.hasPrevious()).isFalse();
    }

    @Test
    void buildsPageRequestFromValidatedRestParameters() {
        GroupPageRequest pageRequest = mapper.toPageRequest(1, 10, "name", "desc", "ACTIVE");

        assertThat(pageRequest.page()).isEqualTo(1);
        assertThat(pageRequest.size()).isEqualTo(10);
        assertThat(pageRequest.sortField()).isEqualTo(GroupSortField.NAME);
        assertThat(pageRequest.direction()).isEqualTo(SortDirection.DESC);
        assertThat(pageRequest.statusFilter()).isEqualTo(GroupStatus.ACTIVE);
    }

    @Test
    void buildsPageRequestWithoutStatusFilterWhenAbsent() {
        GroupPageRequest pageRequest = mapper.toPageRequest(0, 20, "createdAt", "asc", null);

        assertThat(pageRequest.statusFilter()).isNull();
    }

    @Test
    void rejectsUnsupportedSortOrDirectionValues() {
        assertThatThrownBy(() -> mapper.toPageRequest(0, 20, "unsupported", "asc", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> mapper.toPageRequest(0, 20, "name", "sideways", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private AuthenticatedUser authenticatedUser() {
        return new AuthenticatedUser(
                UserId.newId(),
                MobileNumber.of("+919876543210"),
                AggregateId.newId(),
                Set.of("GROUP_MEMBER"),
                Set.of("group.read"));
    }

    private SavingsGroupSummary summary() {
        return new SavingsGroupSummary(
                UUID.randomUUID(), "BS-1A2B3C4D5E6F7A8B", "Sunrise Bhishi Circle", "ACTIVE", 500_000L, "INR", 10, 1);
    }
}
