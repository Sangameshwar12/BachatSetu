package in.bachatsetu.backend.dashboard.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.dashboard.application.exception.NoActiveGroupException;
import in.bachatsetu.backend.dashboard.application.query.MemberDashboardResult;
import in.bachatsetu.backend.draw.domain.port.DrawRepository;
import in.bachatsetu.backend.group.application.port.SavingsGroupRepository;
import in.bachatsetu.backend.group.domain.GroupDomainFixtures;
import in.bachatsetu.backend.group.domain.model.GroupId;
import in.bachatsetu.backend.group.domain.model.SavingsGroup;
import in.bachatsetu.backend.member.domain.model.GroupParticipation;
import in.bachatsetu.backend.member.domain.model.GroupRole;
import in.bachatsetu.backend.member.domain.model.MemberNumber;
import in.bachatsetu.backend.member.domain.model.MemberProfile;
import in.bachatsetu.backend.member.domain.model.MemberStatus;
import in.bachatsetu.backend.member.domain.model.ParticipationStatus;
import in.bachatsetu.backend.member.domain.port.MemberRepository;
import in.bachatsetu.backend.notification.domain.port.NotificationRepository;
import in.bachatsetu.backend.payment.domain.port.PaymentRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.AuditInfo;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetMemberDashboardApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-10T06:00:00Z");
    private static final AggregateId TENANT_ID = AggregateId.newId();
    private static final AggregateId USER_ID = AggregateId.newId();

    private MemberRepository memberRepository;
    private SavingsGroupRepository groupRepository;
    private PaymentRepository paymentRepository;
    private DrawRepository drawRepository;
    private NotificationRepository notificationRepository;
    private GetMemberDashboardApplicationService service;

    @BeforeEach
    void setUp() {
        memberRepository = mock(MemberRepository.class);
        groupRepository = mock(SavingsGroupRepository.class);
        paymentRepository = mock(PaymentRepository.class);
        drawRepository = mock(DrawRepository.class);
        notificationRepository = mock(NotificationRepository.class);
        service = new GetMemberDashboardApplicationService(
                memberRepository, groupRepository, paymentRepository, drawRepository, notificationRepository);
    }

    @Test
    void composesTheDashboardFromTheMembersActiveGroup() {
        SavingsGroup group = GroupDomainFixtures.newGroup(10);
        AggregateId groupId = group.groupId().value();
        MemberProfile profile = activeProfile(groupId);
        when(memberRepository.findByUserId(TENANT_ID, USER_ID)).thenReturn(Optional.of(profile));
        when(groupRepository.findById(TENANT_ID, new GroupId(groupId))).thenReturn(Optional.of(group));
        when(drawRepository.findNextScheduledByGroup(TENANT_ID, groupId)).thenReturn(Optional.empty());
        when(paymentRepository.findLatestByGroupAndMember(TENANT_ID, groupId, USER_ID)).thenReturn(Optional.empty());
        when(notificationRepository.findRecentForRecipient(TENANT_ID, USER_ID)).thenReturn(List.of());

        MemberDashboardResult result = service.execute(TENANT_ID, USER_ID);

        assertThat(result.currentGroup().groupId()).isEqualTo(groupId);
        assertThat(result.nextDraw()).isNull();
        assertThat(result.latestPaymentStatus()).isNull();
        assertThat(result.recentNotifications()).isEmpty();
    }

    @Test
    void selectsTheNewestActiveGroupOverAnOlderClosedOne() {
        SavingsGroup closedGroup = GroupDomainFixtures.newGroup(10);
        closedGroup.close(USER_ID, NOW);
        SavingsGroup activeGroup = GroupDomainFixtures.newGroup(10);
        activeGroup.activate(USER_ID, NOW);

        AggregateId closedGroupId = closedGroup.groupId().value();
        AggregateId activeGroupId = activeGroup.groupId().value();
        GroupParticipation olderParticipation = new GroupParticipation(
                AggregateId.newId(), closedGroupId, GroupRole.MEMBER, NOW, ParticipationStatus.ACTIVE, null);
        GroupParticipation newerParticipation = new GroupParticipation(
                AggregateId.newId(), activeGroupId, GroupRole.MEMBER, NOW.plusSeconds(60), ParticipationStatus.ACTIVE, null);
        MemberProfile profile = new MemberProfile(
                USER_ID, TENANT_ID, USER_ID, new MemberNumber("MEM-0001"), MemberStatus.ACTIVE,
                List.of(olderParticipation, newerParticipation), List.of(), AuditInfo.createdBy(USER_ID, NOW), 0);

        when(memberRepository.findByUserId(TENANT_ID, USER_ID)).thenReturn(Optional.of(profile));
        when(groupRepository.findById(TENANT_ID, new GroupId(closedGroupId))).thenReturn(Optional.of(closedGroup));
        when(groupRepository.findById(TENANT_ID, new GroupId(activeGroupId))).thenReturn(Optional.of(activeGroup));
        when(drawRepository.findNextScheduledByGroup(TENANT_ID, activeGroupId)).thenReturn(Optional.empty());
        when(paymentRepository.findLatestByGroupAndMember(TENANT_ID, activeGroupId, USER_ID)).thenReturn(Optional.empty());
        when(notificationRepository.findRecentForRecipient(TENANT_ID, USER_ID)).thenReturn(List.of());

        MemberDashboardResult result = service.execute(TENANT_ID, USER_ID);

        assertThat(result.currentGroup().groupId()).isEqualTo(activeGroupId);
    }

    @Test
    void fallsBackToTheNewestParticipationWhenNoneOfTheGroupsAreActive() {
        SavingsGroup closedGroup = GroupDomainFixtures.newGroup(10);
        closedGroup.close(USER_ID, NOW);
        SavingsGroup inactiveGroup = GroupDomainFixtures.newGroup(10);

        AggregateId closedGroupId = closedGroup.groupId().value();
        AggregateId inactiveGroupId = inactiveGroup.groupId().value();
        GroupParticipation olderParticipation = new GroupParticipation(
                AggregateId.newId(), closedGroupId, GroupRole.MEMBER, NOW, ParticipationStatus.ACTIVE, null);
        GroupParticipation newerParticipation = new GroupParticipation(
                AggregateId.newId(), inactiveGroupId, GroupRole.MEMBER, NOW.plusSeconds(60), ParticipationStatus.ACTIVE, null);
        MemberProfile profile = new MemberProfile(
                USER_ID, TENANT_ID, USER_ID, new MemberNumber("MEM-0001"), MemberStatus.ACTIVE,
                List.of(olderParticipation, newerParticipation), List.of(), AuditInfo.createdBy(USER_ID, NOW), 0);

        when(memberRepository.findByUserId(TENANT_ID, USER_ID)).thenReturn(Optional.of(profile));
        when(groupRepository.findById(TENANT_ID, new GroupId(closedGroupId))).thenReturn(Optional.of(closedGroup));
        when(groupRepository.findById(TENANT_ID, new GroupId(inactiveGroupId))).thenReturn(Optional.of(inactiveGroup));
        when(drawRepository.findNextScheduledByGroup(TENANT_ID, inactiveGroupId)).thenReturn(Optional.empty());
        when(paymentRepository.findLatestByGroupAndMember(TENANT_ID, inactiveGroupId, USER_ID)).thenReturn(Optional.empty());
        when(notificationRepository.findRecentForRecipient(TENANT_ID, USER_ID)).thenReturn(List.of());

        MemberDashboardResult result = service.execute(TENANT_ID, USER_ID);

        assertThat(result.currentGroup().groupId()).isEqualTo(inactiveGroupId);
    }

    @Test
    void rejectsWhenNoMemberProfileExists() {
        when(memberRepository.findByUserId(TENANT_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(TENANT_ID, USER_ID)).isInstanceOf(NoActiveGroupException.class);
    }

    @Test
    void rejectsWhenTheMemberHasNoActiveParticipation() {
        MemberProfile profile = new MemberProfile(
                USER_ID, TENANT_ID, USER_ID, new MemberNumber("MEM-0001"), MemberStatus.ACTIVE, List.of(), List.of(),
                AuditInfo.createdBy(USER_ID, NOW), 0);
        when(memberRepository.findByUserId(TENANT_ID, USER_ID)).thenReturn(Optional.of(profile));

        assertThatThrownBy(() -> service.execute(TENANT_ID, USER_ID)).isInstanceOf(NoActiveGroupException.class);
    }

    private MemberProfile activeProfile(AggregateId groupId) {
        GroupParticipation participation =
                new GroupParticipation(AggregateId.newId(), groupId, GroupRole.MEMBER, NOW, ParticipationStatus.ACTIVE, null);
        return new MemberProfile(
                USER_ID, TENANT_ID, USER_ID, new MemberNumber("MEM-0001"), MemberStatus.ACTIVE,
                List.of(participation), List.of(), AuditInfo.createdBy(USER_ID, NOW), 0);
    }
}
