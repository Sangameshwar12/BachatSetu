package in.bachatsetu.backend.payment.application.service;

import in.bachatsetu.backend.group.application.port.SavingsGroupRepository;
import in.bachatsetu.backend.group.domain.model.ContributionSchedule;
import in.bachatsetu.backend.group.domain.model.GroupId;
import in.bachatsetu.backend.group.domain.model.GroupStatus;
import in.bachatsetu.backend.group.domain.model.SavingsGroup;
import in.bachatsetu.backend.payment.application.command.RecordManualPaymentCommand;
import in.bachatsetu.backend.payment.application.exception.CollectionAccessDeniedException;
import in.bachatsetu.backend.payment.application.exception.CollectionGroupNotFoundException;
import in.bachatsetu.backend.payment.application.exception.MemberAlreadyPaidException;
import in.bachatsetu.backend.payment.application.exception.MemberNotInGroupException;
import in.bachatsetu.backend.payment.application.exception.NoActiveCollectionCycleException;
import in.bachatsetu.backend.payment.application.mapper.PaymentApplicationMapper;
import in.bachatsetu.backend.payment.application.port.ClockPort;
import in.bachatsetu.backend.payment.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.payment.application.port.TransactionPort;
import in.bachatsetu.backend.payment.application.query.PaymentResult;
import in.bachatsetu.backend.payment.application.support.ContributionCycleCalculator;
import in.bachatsetu.backend.payment.application.support.CycleWindow;
import in.bachatsetu.backend.payment.application.usecase.RecordManualPaymentUseCase;
import in.bachatsetu.backend.payment.domain.factory.PaymentFactory;
import in.bachatsetu.backend.payment.domain.model.IdempotencyKey;
import in.bachatsetu.backend.payment.domain.model.Payment;
import in.bachatsetu.backend.payment.domain.model.PaymentMethod;
import in.bachatsetu.backend.payment.domain.model.ProviderReference;
import in.bachatsetu.backend.payment.domain.port.PaymentRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Optional;

/** Lets a group's organizer manually record a member's contribution for the current cycle as paid in cash. */
public final class RecordManualPaymentApplicationService implements RecordManualPaymentUseCase {

    private static final String MANUAL_PROVIDER = "MANUAL";

    private final SavingsGroupRepository groupRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentFactory paymentFactory;
    private final ClockPort clock;
    private final TransactionPort transaction;
    private final PaymentApplicationMapper mapper;
    private final PaymentApplicationSupport support;

    public RecordManualPaymentApplicationService(
            SavingsGroupRepository groupRepository,
            PaymentRepository paymentRepository,
            PaymentFactory paymentFactory,
            DomainEventPublisherPort eventPublisher,
            ClockPort clock,
            TransactionPort transaction,
            PaymentApplicationMapper mapper) {
        this.groupRepository = Objects.requireNonNull(groupRepository, "groupRepository must not be null");
        this.paymentRepository = Objects.requireNonNull(paymentRepository, "paymentRepository must not be null");
        this.paymentFactory = Objects.requireNonNull(paymentFactory, "paymentFactory must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
        this.support = new PaymentApplicationSupport(paymentRepository, eventPublisher, mapper);
    }

    @Override
    public PaymentResult execute(RecordManualPaymentCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        return transaction.execute(() -> record(command));
    }

    private PaymentResult record(RecordManualPaymentCommand command) {
        SavingsGroup group = groupRepository.findById(command.tenantId(), new GroupId(command.groupId()))
                .orElseThrow(() -> new CollectionGroupNotFoundException("savings group does not exist"));
        if (!group.ownerId().value().equals(command.actorId())) {
            throw new CollectionAccessDeniedException("only the group owner may record a manual payment");
        }
        boolean isActiveMember = group.members().stream()
                .anyMatch(member -> member.memberId().equals(command.memberId()) && member.isActive());
        if (!isActiveMember) {
            throw new MemberNotInGroupException("member is not an active member of this group");
        }

        ContributionSchedule schedule = group.rule().contributionSchedule();
        LocalDate today = LocalDate.ofInstant(clock.now(), ZoneOffset.UTC);
        Optional<CycleWindow> currentCycle = group.status() == GroupStatus.ACTIVE
                ? ContributionCycleCalculator.currentCycle(schedule, today)
                : Optional.empty();
        CycleWindow cycle = currentCycle.orElseThrow(
                () -> new NoActiveCollectionCycleException("group has no active contribution cycle right now"));

        Instant windowStart = cycle.periodStart().atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant windowEnd = cycle.periodEnd().atStartOfDay(ZoneOffset.UTC).toInstant();
        boolean alreadyPaid = paymentRepository
                .findVerifiedByGroupWithinWindow(command.tenantId(), command.groupId(), windowStart, windowEnd)
                .stream()
                .anyMatch(payment -> payment.memberId().equals(command.memberId()));
        if (alreadyPaid) {
            throw new MemberAlreadyPaidException("member has already paid for the current cycle");
        }

        IdempotencyKey idempotencyKey = new IdempotencyKey("manual-" + command.groupId().value() + "-"
                + command.memberId().value() + "-" + cycle.cycleNumber());
        Optional<Payment> existing = paymentRepository.findByIdempotencyKey(command.tenantId(), idempotencyKey);
        if (existing.isPresent()) {
            return mapper.toResult(existing.get());
        }
        Payment payment = paymentFactory.initiate(
                command.tenantId(), command.groupId(), command.memberId(), idempotencyKey,
                schedule.contributionAmount(), PaymentMethod.CASH, command.actorId());
        payment.verify(
                new ProviderReference(MANUAL_PROVIDER, "organizer-recorded-cycle-" + cycle.cycleNumber()),
                command.actorId(),
                clock.now());
        return support.saveAndPublish(payment);
    }
}
