package in.bachatsetu.backend.dashboard.application.service;

import in.bachatsetu.backend.dashboard.application.exception.NoActiveGroupException;
import in.bachatsetu.backend.dashboard.application.query.CurrentGroupSummary;
import in.bachatsetu.backend.dashboard.application.query.MemberDashboardResult;
import in.bachatsetu.backend.dashboard.application.query.NextDrawSummary;
import in.bachatsetu.backend.dashboard.application.query.RecentNotificationSummary;
import in.bachatsetu.backend.dashboard.application.usecase.GetMemberDashboardUseCase;
import in.bachatsetu.backend.draw.domain.model.Draw;
import in.bachatsetu.backend.draw.domain.port.DrawRepository;
import in.bachatsetu.backend.group.application.port.SavingsGroupRepository;
import in.bachatsetu.backend.group.domain.model.GroupId;
import in.bachatsetu.backend.group.domain.model.GroupStatus;
import in.bachatsetu.backend.group.domain.model.SavingsGroup;
import in.bachatsetu.backend.member.domain.model.GroupParticipation;
import in.bachatsetu.backend.member.domain.model.MemberProfile;
import in.bachatsetu.backend.member.domain.model.ParticipationStatus;
import in.bachatsetu.backend.member.domain.port.MemberRepository;
import in.bachatsetu.backend.notification.domain.model.Notification;
import in.bachatsetu.backend.notification.domain.port.NotificationRepository;
import in.bachatsetu.backend.payment.domain.model.Payment;
import in.bachatsetu.backend.payment.domain.port.PaymentRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Composes the member Welcome/Home dashboard from existing group, payment, draw, and notification data. */
public final class GetMemberDashboardApplicationService implements GetMemberDashboardUseCase {

    private final MemberRepository memberRepository;
    private final SavingsGroupRepository groupRepository;
    private final PaymentRepository paymentRepository;
    private final DrawRepository drawRepository;
    private final NotificationRepository notificationRepository;

    public GetMemberDashboardApplicationService(
            MemberRepository memberRepository,
            SavingsGroupRepository groupRepository,
            PaymentRepository paymentRepository,
            DrawRepository drawRepository,
            NotificationRepository notificationRepository) {
        this.memberRepository = Objects.requireNonNull(memberRepository, "memberRepository must not be null");
        this.groupRepository = Objects.requireNonNull(groupRepository, "groupRepository must not be null");
        this.paymentRepository = Objects.requireNonNull(paymentRepository, "paymentRepository must not be null");
        this.drawRepository = Objects.requireNonNull(drawRepository, "drawRepository must not be null");
        this.notificationRepository =
                Objects.requireNonNull(notificationRepository, "notificationRepository must not be null");
    }

    @Override
    public MemberDashboardResult execute(AggregateId tenantId, AggregateId userId) {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");

        MemberProfile profile = memberRepository.findByUserId(tenantId, userId)
                .orElseThrow(() -> new NoActiveGroupException("no member profile exists for this user"));
        List<GroupParticipation> activeParticipations = profile.participations().stream()
                .filter(participation -> participation.status() == ParticipationStatus.ACTIVE)
                .sorted(Comparator.comparing(GroupParticipation::joinedAt).reversed())
                .toList();
        if (activeParticipations.isEmpty()) {
            throw new NoActiveGroupException("this user has not yet joined or created a group");
        }

        AggregateId groupId = null;
        SavingsGroup group = null;
        for (GroupParticipation participation : activeParticipations) {
            Optional<SavingsGroup> candidate = groupRepository.findById(tenantId, new GroupId(participation.groupId()));
            if (candidate.isPresent() && candidate.get().status() == GroupStatus.ACTIVE) {
                groupId = participation.groupId();
                group = candidate.get();
                break;
            }
        }
        if (group == null) {
            groupId = activeParticipations.getFirst().groupId();
            group = groupRepository.findById(tenantId, new GroupId(groupId))
                    .orElseThrow(() -> new NoActiveGroupException("the joined group no longer exists"));
        }

        CurrentGroupSummary currentGroup = new CurrentGroupSummary(
                group.groupId().value(), group.code().value(), group.name().value(),
                group.contributionAmount().value().minorUnits(), group.contributionAmount().value().currency().getCurrencyCode(),
                group.rule().contributionSchedule().frequency().name(), group.members().size(),
                group.maximumMembers().value());

        NextDrawSummary nextDraw = drawRepository.findNextScheduledByGroup(tenantId, groupId)
                .map(this::toNextDrawSummary)
                .orElse(null);

        String latestPaymentStatus = paymentRepository.findLatestByGroupAndMember(tenantId, groupId, userId)
                .map(Payment::status)
                .map(Enum::name)
                .orElse(null);

        List<RecentNotificationSummary> notifications = notificationRepository
                .findRecentForRecipient(tenantId, userId)
                .stream()
                .map(this::toNotificationSummary)
                .toList();

        return new MemberDashboardResult(currentGroup, nextDraw, latestPaymentStatus, notifications);
    }

    private NextDrawSummary toNextDrawSummary(Draw draw) {
        return new NextDrawSummary(draw.id(), draw.scheduledAt(), draw.status().name());
    }

    private RecentNotificationSummary toNotificationSummary(Notification notification) {
        return new RecentNotificationSummary(
                notification.id(), notification.category().name(), notification.status().name(),
                notification.auditInfo().createdAt());
    }
}
