package in.bachatsetu.backend.dashboard.application.service;

import in.bachatsetu.backend.dashboard.application.query.NextDrawSummary;
import in.bachatsetu.backend.dashboard.application.query.OrganizerDashboardResult;
import in.bachatsetu.backend.dashboard.application.query.OrganizerGroupSummary;
import in.bachatsetu.backend.dashboard.application.query.QuickAction;
import in.bachatsetu.backend.dashboard.application.usecase.GetOrganizerDashboardUseCase;
import in.bachatsetu.backend.draw.domain.model.Draw;
import in.bachatsetu.backend.draw.domain.port.DrawRepository;
import in.bachatsetu.backend.group.application.port.SavingsGroupRepository;
import in.bachatsetu.backend.group.domain.model.GroupMember;
import in.bachatsetu.backend.group.domain.model.SavingsGroup;
import in.bachatsetu.backend.invitation.domain.model.InvitationStatus;
import in.bachatsetu.backend.invitation.domain.port.GroupInvitationRepository;
import in.bachatsetu.backend.payment.domain.model.Payment;
import in.bachatsetu.backend.payment.domain.model.PaymentStatus;
import in.bachatsetu.backend.payment.domain.port.PaymentRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.List;
import java.util.Objects;

/** Composes the organizer dashboard: every group they own, plus real quick-action routes. */
public final class GetOrganizerDashboardApplicationService implements GetOrganizerDashboardUseCase {

    private final SavingsGroupRepository groupRepository;
    private final GroupInvitationRepository invitationRepository;
    private final PaymentRepository paymentRepository;
    private final DrawRepository drawRepository;

    public GetOrganizerDashboardApplicationService(
            SavingsGroupRepository groupRepository,
            GroupInvitationRepository invitationRepository,
            PaymentRepository paymentRepository,
            DrawRepository drawRepository) {
        this.groupRepository = Objects.requireNonNull(groupRepository, "groupRepository must not be null");
        this.invitationRepository = Objects.requireNonNull(invitationRepository, "invitationRepository must not be null");
        this.paymentRepository = Objects.requireNonNull(paymentRepository, "paymentRepository must not be null");
        this.drawRepository = Objects.requireNonNull(drawRepository, "drawRepository must not be null");
    }

    @Override
    public OrganizerDashboardResult execute(AggregateId tenantId, AggregateId organizerId) {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(organizerId, "organizerId must not be null");

        List<OrganizerGroupSummary> groups = groupRepository.findByOwnerId(tenantId, organizerId).stream()
                .map(group -> toSummary(tenantId, group))
                .toList();

        return new OrganizerDashboardResult(groups, quickActions());
    }

    private OrganizerGroupSummary toSummary(AggregateId tenantId, SavingsGroup group) {
        AggregateId groupId = group.groupId().value();
        boolean hasActiveInvitation = invitationRepository.findActiveByGroup(tenantId, groupId)
                .map(invitation -> invitation.status() == InvitationStatus.ACTIVE)
                .orElse(false);
        NextDrawSummary nextDraw = drawRepository.findNextScheduledByGroup(tenantId, groupId)
                .map(this::toNextDrawSummary)
                .orElse(null);
        int progress = contributionProgressPercent(tenantId, group);
        return new OrganizerGroupSummary(
                groupId, group.code().value(), group.name().value(), group.members().size(),
                group.maximumMembers().value(), hasActiveInvitation, nextDraw, progress);
    }

    private int contributionProgressPercent(AggregateId tenantId, SavingsGroup group) {
        List<GroupMember> members = group.members();
        if (members.isEmpty()) {
            return 0;
        }
        long paidCount = members.stream()
                .filter(member -> paymentRepository
                        .findLatestByGroupAndMember(tenantId, group.groupId().value(), member.memberId())
                        .map(Payment::status)
                        .filter(status -> status == PaymentStatus.VERIFIED)
                        .isPresent())
                .count();
        return Math.toIntExact(Math.round(paidCount * 100.0 / members.size()));
    }

    private NextDrawSummary toNextDrawSummary(Draw draw) {
        return new NextDrawSummary(draw.id(), draw.scheduledAt(), draw.status().name());
    }

    private List<QuickAction> quickActions() {
        return List.of(
                new QuickAction("Invite Members", "POST", "/api/v1/groups/{groupId}/invite"),
                new QuickAction("Start Draw", "POST", "/api/v1/draws"),
                new QuickAction("Record Payment", "POST", "/api/v1/payments"));
    }
}
