package in.bachatsetu.backend.payment.application.service;

import in.bachatsetu.backend.group.application.port.SavingsGroupRepository;
import in.bachatsetu.backend.group.domain.model.ContributionSchedule;
import in.bachatsetu.backend.group.domain.model.GroupId;
import in.bachatsetu.backend.group.domain.model.GroupMember;
import in.bachatsetu.backend.group.domain.model.GroupStatus;
import in.bachatsetu.backend.group.domain.model.SavingsGroup;
import in.bachatsetu.backend.payment.application.exception.CollectionGroupNotFoundException;
import in.bachatsetu.backend.payment.application.port.ClockPort;
import in.bachatsetu.backend.payment.application.port.TransactionPort;
import in.bachatsetu.backend.payment.application.query.CollectionSummaryResult;
import in.bachatsetu.backend.payment.application.query.MemberCollectionResult;
import in.bachatsetu.backend.payment.application.support.ContributionCycleCalculator;
import in.bachatsetu.backend.payment.application.support.CycleWindow;
import in.bachatsetu.backend.payment.application.usecase.GetCollectionSummaryUseCase;
import in.bachatsetu.backend.payment.domain.model.Payment;
import in.bachatsetu.backend.payment.domain.port.PaymentRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/** Computes a group's current-cycle contribution collection status from its schedule and verified payments. */
public final class GetCollectionSummaryApplicationService implements GetCollectionSummaryUseCase {

    private final SavingsGroupRepository groupRepository;
    private final PaymentRepository paymentRepository;
    private final ClockPort clock;
    private final TransactionPort transaction;

    public GetCollectionSummaryApplicationService(
            SavingsGroupRepository groupRepository,
            PaymentRepository paymentRepository,
            ClockPort clock,
            TransactionPort transaction) {
        this.groupRepository = Objects.requireNonNull(groupRepository, "groupRepository must not be null");
        this.paymentRepository = Objects.requireNonNull(paymentRepository, "paymentRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
    }

    @Override
    public CollectionSummaryResult execute(AggregateId tenantId, GroupId groupId) {
        Objects.requireNonNull(tenantId, "tenant id must not be null");
        Objects.requireNonNull(groupId, "group id must not be null");
        return transaction.execute(() -> summarize(tenantId, groupId));
    }

    private CollectionSummaryResult summarize(AggregateId tenantId, GroupId groupId) {
        SavingsGroup group = groupRepository.findById(tenantId, groupId)
                .orElseThrow(() -> new CollectionGroupNotFoundException("savings group does not exist"));
        ContributionSchedule schedule = group.rule().contributionSchedule();
        long contributionAmountPaise = schedule.contributionAmount().minorUnits();
        String currencyCode = schedule.contributionAmount().currency().getCurrencyCode();
        List<GroupMember> activeMembers = group.members().stream().filter(GroupMember::isActive).toList();
        int totalMembers = activeMembers.size();

        LocalDate today = LocalDate.ofInstant(clock.now(), ZoneOffset.UTC);
        Optional<CycleWindow> currentCycle = group.status() == GroupStatus.ACTIVE
                ? ContributionCycleCalculator.currentCycle(schedule, today)
                : Optional.empty();

        if (currentCycle.isEmpty()) {
            return new CollectionSummaryResult(
                    groupId.value().value(), false, 0, null, null, null,
                    contributionAmountPaise, currencyCode, totalMembers, 0, 0, 0, 0, 0, 0, List.of());
        }

        CycleWindow cycle = currentCycle.get();
        Instant windowStart = cycle.periodStart().atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant windowEnd = cycle.periodEnd().atStartOfDay(ZoneOffset.UTC).toInstant();
        List<Payment> verifiedThisCycle = paymentRepository.findVerifiedByGroupWithinWindow(
                tenantId, groupId.value(), windowStart, windowEnd);
        Map<AggregateId, List<Payment>> paymentsByMember = verifiedThisCycle.stream()
                .collect(Collectors.groupingBy(Payment::memberId));

        boolean pastDueDate = today.isAfter(cycle.dueDate());
        int paidCount = 0;
        int pendingCount = 0;
        int overdueCount = 0;
        long totalCollectedPaise = 0;
        List<MemberCollectionResult> members = new java.util.ArrayList<>();
        for (GroupMember member : activeMembers) {
            List<Payment> memberPayments = paymentsByMember.getOrDefault(member.memberId(), List.of());
            if (!memberPayments.isEmpty()) {
                long collected = memberPayments.stream().mapToLong(payment -> payment.amount().minorUnits()).sum();
                Instant paidAt = memberPayments.stream()
                        .map(payment -> payment.auditInfo().updatedAt())
                        .min(Instant::compareTo)
                        .orElse(null);
                members.add(new MemberCollectionResult(
                        member.memberId().value(), "PAID", contributionAmountPaise, collected, paidAt,
                        cycle.dueDate()));
                paidCount++;
                totalCollectedPaise += collected;
            } else if (pastDueDate) {
                members.add(new MemberCollectionResult(
                        member.memberId().value(), "OVERDUE", contributionAmountPaise, 0, null, cycle.dueDate()));
                overdueCount++;
            } else {
                members.add(new MemberCollectionResult(
                        member.memberId().value(), "PENDING", contributionAmountPaise, 0, null, cycle.dueDate()));
                pendingCount++;
            }
        }

        long totalExpectedPaise = contributionAmountPaise * totalMembers;
        long totalRemainingPaise = Math.max(0, totalExpectedPaise - totalCollectedPaise);

        return new CollectionSummaryResult(
                groupId.value().value(), true, cycle.cycleNumber(), cycle.periodStart(), cycle.periodEnd(),
                cycle.dueDate(), contributionAmountPaise, currencyCode, totalMembers, paidCount, pendingCount,
                overdueCount, totalExpectedPaise, totalCollectedPaise, totalRemainingPaise, members);
    }
}
