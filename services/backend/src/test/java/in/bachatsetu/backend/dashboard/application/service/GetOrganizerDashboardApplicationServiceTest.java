package in.bachatsetu.backend.dashboard.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.dashboard.application.query.OrganizerDashboardResult;
import in.bachatsetu.backend.dashboard.application.query.OrganizerGroupSummary;
import in.bachatsetu.backend.draw.domain.port.DrawRepository;
import in.bachatsetu.backend.group.application.port.SavingsGroupRepository;
import in.bachatsetu.backend.group.domain.GroupDomainFixtures;
import in.bachatsetu.backend.group.domain.model.SavingsGroup;
import in.bachatsetu.backend.invitation.domain.port.GroupInvitationRepository;
import in.bachatsetu.backend.payment.domain.port.PaymentRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetOrganizerDashboardApplicationServiceTest {

    private static final AggregateId TENANT_ID = AggregateId.newId();
    private static final AggregateId ORGANIZER_ID = AggregateId.newId();

    private SavingsGroupRepository groupRepository;
    private GroupInvitationRepository invitationRepository;
    private PaymentRepository paymentRepository;
    private DrawRepository drawRepository;
    private GetOrganizerDashboardApplicationService service;

    @BeforeEach
    void setUp() {
        groupRepository = mock(SavingsGroupRepository.class);
        invitationRepository = mock(GroupInvitationRepository.class);
        paymentRepository = mock(PaymentRepository.class);
        drawRepository = mock(DrawRepository.class);
        service = new GetOrganizerDashboardApplicationService(
                groupRepository, invitationRepository, paymentRepository, drawRepository);
    }

    @Test
    void composesASummaryForEveryOwnedGroup() {
        SavingsGroup group = GroupDomainFixtures.newGroup(ORGANIZER_ID, 10);
        when(groupRepository.findByOwnerId(TENANT_ID, ORGANIZER_ID)).thenReturn(List.of(group));
        when(invitationRepository.findActiveByGroup(any(), any())).thenReturn(Optional.empty());
        when(drawRepository.findNextScheduledByGroup(any(), any())).thenReturn(Optional.empty());
        when(paymentRepository.findLatestByGroupAndMember(any(), any(), any())).thenReturn(Optional.empty());

        OrganizerDashboardResult result = service.execute(TENANT_ID, ORGANIZER_ID);

        assertThat(result.groups()).hasSize(1);
        OrganizerGroupSummary summary = result.groups().getFirst();
        assertThat(summary.groupId()).isEqualTo(group.groupId().value());
        assertThat(summary.hasActiveInvitation()).isFalse();
        assertThat(summary.nextDraw()).isNull();
        assertThat(summary.contributionProgressPercent()).isZero();
        assertThat(result.quickActions()).extracting("label")
                .containsExactly("Invite Members", "Start Draw", "Record Payment");
    }

    @Test
    void returnsAnEmptyGroupsListWhenTheOrganizerOwnsNoGroups() {
        when(groupRepository.findByOwnerId(TENANT_ID, ORGANIZER_ID)).thenReturn(List.of());

        OrganizerDashboardResult result = service.execute(TENANT_ID, ORGANIZER_ID);

        assertThat(result.groups()).isEmpty();
        assertThat(result.quickActions()).isNotEmpty();
    }
}
