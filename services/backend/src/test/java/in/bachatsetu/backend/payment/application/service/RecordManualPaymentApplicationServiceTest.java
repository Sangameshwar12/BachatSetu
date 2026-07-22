package in.bachatsetu.backend.payment.application.service;

import static in.bachatsetu.backend.group.domain.GroupDomainFixtures.NOW;
import static in.bachatsetu.backend.group.domain.GroupDomainFixtures.newGroup;
import static in.bachatsetu.backend.payment.application.ApplicationTestFixture.directTransaction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.group.application.port.SavingsGroupRepository;
import in.bachatsetu.backend.group.domain.model.GroupId;
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
import in.bachatsetu.backend.payment.domain.factory.PaymentFactory;
import in.bachatsetu.backend.payment.domain.model.IdempotencyKey;
import in.bachatsetu.backend.payment.domain.model.Payment;
import in.bachatsetu.backend.payment.domain.model.PaymentMethod;
import in.bachatsetu.backend.payment.domain.model.PaymentReference;
import in.bachatsetu.backend.payment.domain.model.ProviderReference;
import in.bachatsetu.backend.payment.domain.port.PaymentRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Money;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RecordManualPaymentApplicationServiceTest {

    private static final LocalDate CYCLE_START = LocalDate.of(2026, 8, 1);

    private SavingsGroupRepository groupRepository;
    private PaymentRepository paymentRepository;
    private PaymentFactory paymentFactory;
    private DomainEventPublisherPort eventPublisher;
    private ClockPort clock;
    private TransactionPort transaction;
    private PaymentApplicationMapper mapper;
    private RecordManualPaymentApplicationService service;

    @BeforeEach
    void setUp() {
        groupRepository = mock(SavingsGroupRepository.class);
        paymentRepository = mock(PaymentRepository.class);
        paymentFactory = new PaymentFactory(Clock.fixed(
                CYCLE_START.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC));
        eventPublisher = mock(DomainEventPublisherPort.class);
        clock = () -> CYCLE_START.atStartOfDay(ZoneOffset.UTC).toInstant();
        transaction = directTransaction();
        mapper = new PaymentApplicationMapper();
        service = new RecordManualPaymentApplicationService(
                groupRepository, paymentRepository, paymentFactory, eventPublisher, clock, transaction, mapper);
        when(paymentRepository.findVerifiedByGroupWithinWindow(any(), any(), any(), any())).thenReturn(List.of());
        when(paymentRepository.findByIdempotencyKey(any(), any())).thenReturn(Optional.empty());
    }

    @Test
    void recordsAVerifiedCashPaymentForTheCurrentCycle() {
        AggregateId ownerId = AggregateId.newId();
        SavingsGroup group = newGroup(ownerId, 5);
        group.activate(ownerId, NOW);
        AggregateId memberId = AggregateId.newId();
        group.joinMember(memberId, ownerId, NOW.plusSeconds(1));
        when(groupRepository.findById(group.tenantId(), group.groupId())).thenReturn(Optional.of(group));

        PaymentResult result = service.execute(
                new RecordManualPaymentCommand(group.tenantId(), group.groupId().value(), memberId, ownerId));

        assertThat(result.status()).isEqualTo("VERIFIED");
        assertThat(result.method()).isEqualTo("CASH");
        assertThat(result.amountPaise()).isEqualTo(100_000);
        assertThat(result.memberId()).isEqualTo(memberId.value());
        verify(paymentRepository).save(any(Payment.class));
        verify(eventPublisher).publish(any());
    }

    @Test
    void rejectsANonOwnerActor() {
        AggregateId ownerId = AggregateId.newId();
        SavingsGroup group = newGroup(ownerId, 5);
        group.activate(ownerId, NOW);
        when(groupRepository.findById(group.tenantId(), group.groupId())).thenReturn(Optional.of(group));
        AggregateId nonOwner = AggregateId.newId();

        assertThatThrownBy(() -> service.execute(
                        new RecordManualPaymentCommand(group.tenantId(), group.groupId().value(), ownerId, nonOwner)))
                .isInstanceOf(CollectionAccessDeniedException.class);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void rejectsAMemberWhoIsNotInTheGroup() {
        AggregateId ownerId = AggregateId.newId();
        SavingsGroup group = newGroup(ownerId, 5);
        group.activate(ownerId, NOW);
        when(groupRepository.findById(group.tenantId(), group.groupId())).thenReturn(Optional.of(group));
        AggregateId strangerId = AggregateId.newId();

        assertThatThrownBy(() -> service.execute(
                        new RecordManualPaymentCommand(group.tenantId(), group.groupId().value(), strangerId, ownerId)))
                .isInstanceOf(MemberNotInGroupException.class);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void rejectsRecordingWhenTheGroupHasNoActiveCycle() {
        AggregateId ownerId = AggregateId.newId();
        SavingsGroup group = newGroup(ownerId, 5);
        when(groupRepository.findById(group.tenantId(), group.groupId())).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> service.execute(
                        new RecordManualPaymentCommand(group.tenantId(), group.groupId().value(), ownerId, ownerId)))
                .isInstanceOf(NoActiveCollectionCycleException.class);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void rejectsRecordingForAMemberWhoHasAlreadyPaidThisCycle() {
        AggregateId ownerId = AggregateId.newId();
        SavingsGroup group = newGroup(ownerId, 5);
        group.activate(ownerId, NOW);
        when(groupRepository.findById(group.tenantId(), group.groupId())).thenReturn(Optional.of(group));
        Payment existingPayment = Payment.initiate(
                AggregateId.newId(), group.tenantId(), group.groupId().value(), ownerId,
                new PaymentReference("PAY-EXISTING"),
                new IdempotencyKey("existing-cycle-payment-01"),
                Money.inr(100_000), PaymentMethod.CASH, ownerId,
                CYCLE_START.atStartOfDay(ZoneOffset.UTC).toInstant());
        existingPayment.verify(new ProviderReference("MANUAL", "test"), ownerId,
                CYCLE_START.atStartOfDay(ZoneOffset.UTC).toInstant());
        when(paymentRepository.findVerifiedByGroupWithinWindow(any(), any(), any(), any()))
                .thenReturn(List.of(existingPayment));

        assertThatThrownBy(() -> service.execute(
                        new RecordManualPaymentCommand(group.tenantId(), group.groupId().value(), ownerId, ownerId)))
                .isInstanceOf(MemberAlreadyPaidException.class);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void returnsTheExistingPaymentInsteadOfDuplicatingItWhenTheIdempotencyKeyAlreadyExists() {
        AggregateId ownerId = AggregateId.newId();
        SavingsGroup group = newGroup(ownerId, 5);
        group.activate(ownerId, NOW);
        AggregateId memberId = AggregateId.newId();
        group.joinMember(memberId, ownerId, NOW.plusSeconds(1));
        when(groupRepository.findById(group.tenantId(), group.groupId())).thenReturn(Optional.of(group));
        Payment existingPayment = Payment.initiate(
                AggregateId.newId(), group.tenantId(), group.groupId().value(), memberId,
                new PaymentReference("PAY-RETRY"),
                new IdempotencyKey("manual-" + group.groupId().value().value() + "-" + memberId.value() + "-1"),
                Money.inr(100_000), PaymentMethod.CASH, ownerId,
                CYCLE_START.atStartOfDay(ZoneOffset.UTC).toInstant());
        existingPayment.verify(new ProviderReference("MANUAL", "organizer-recorded-cycle-1"), ownerId,
                CYCLE_START.atStartOfDay(ZoneOffset.UTC).toInstant());
        when(paymentRepository.findByIdempotencyKey(any(), any())).thenReturn(Optional.of(existingPayment));

        PaymentResult result = service.execute(
                new RecordManualPaymentCommand(group.tenantId(), group.groupId().value(), memberId, ownerId));

        assertThat(result.memberId()).isEqualTo(memberId.value());
        assertThat(result.status()).isEqualTo("VERIFIED");
        verify(paymentRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void reportsMissingGroupsAsNotFound() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId groupId = AggregateId.newId();
        when(groupRepository.findById(tenantId, new GroupId(groupId))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(
                        new RecordManualPaymentCommand(tenantId, groupId, AggregateId.newId(), AggregateId.newId())))
                .isInstanceOf(CollectionGroupNotFoundException.class);
    }

    @Test
    void rejectsNullCommand() {
        assertThatThrownBy(() -> service.execute(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void validatesRequiredServiceDependencies() {
        assertThatThrownBy(() -> new RecordManualPaymentApplicationService(
                        null, paymentRepository, paymentFactory, eventPublisher, clock, transaction, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new RecordManualPaymentApplicationService(
                        groupRepository, null, paymentFactory, eventPublisher, clock, transaction, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new RecordManualPaymentApplicationService(
                        groupRepository, paymentRepository, null, eventPublisher, clock, transaction, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new RecordManualPaymentApplicationService(
                        groupRepository, paymentRepository, paymentFactory, null, clock, transaction, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new RecordManualPaymentApplicationService(
                        groupRepository, paymentRepository, paymentFactory, eventPublisher, null, transaction, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new RecordManualPaymentApplicationService(
                        groupRepository, paymentRepository, paymentFactory, eventPublisher, clock, null, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new RecordManualPaymentApplicationService(
                        groupRepository, paymentRepository, paymentFactory, eventPublisher, clock, transaction, null))
                .isInstanceOf(NullPointerException.class);
    }
}
