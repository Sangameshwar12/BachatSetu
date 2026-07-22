package in.bachatsetu.backend.payment.application.service;

import static in.bachatsetu.backend.group.domain.GroupDomainFixtures.NOW;
import static in.bachatsetu.backend.group.domain.GroupDomainFixtures.newGroup;
import static in.bachatsetu.backend.payment.application.ApplicationTestFixture.directTransaction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.group.application.port.SavingsGroupRepository;
import in.bachatsetu.backend.group.domain.model.GroupId;
import in.bachatsetu.backend.group.domain.model.SavingsGroup;
import in.bachatsetu.backend.payment.application.exception.CollectionGroupNotFoundException;
import in.bachatsetu.backend.payment.application.port.ClockPort;
import in.bachatsetu.backend.payment.application.port.TransactionPort;
import in.bachatsetu.backend.payment.application.query.CollectionSummaryResult;
import in.bachatsetu.backend.payment.domain.model.IdempotencyKey;
import in.bachatsetu.backend.payment.domain.model.Payment;
import in.bachatsetu.backend.payment.domain.model.PaymentMethod;
import in.bachatsetu.backend.payment.domain.model.PaymentReference;
import in.bachatsetu.backend.payment.domain.model.ProviderReference;
import in.bachatsetu.backend.payment.domain.port.PaymentRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.AuditInfo;
import in.bachatsetu.backend.shared.domain.Money;
import in.bachatsetu.backend.user.domain.model.PersonName;
import in.bachatsetu.backend.user.domain.model.UserProfile;
import in.bachatsetu.backend.user.domain.model.UserStatus;
import in.bachatsetu.backend.user.domain.port.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetCollectionSummaryApplicationServiceTest {

    private static final LocalDate CYCLE_START = LocalDate.of(2026, 8, 1);

    private SavingsGroupRepository groupRepository;
    private PaymentRepository paymentRepository;
    private UserRepository userRepository;
    private ClockPort clock;
    private TransactionPort transaction;
    private GetCollectionSummaryApplicationService service;

    @BeforeEach
    void setUp() {
        groupRepository = mock(SavingsGroupRepository.class);
        paymentRepository = mock(PaymentRepository.class);
        userRepository = mock(UserRepository.class);
        clock = () -> CYCLE_START.atStartOfDay(ZoneOffset.UTC).toInstant();
        transaction = directTransaction();
        service = new GetCollectionSummaryApplicationService(
                groupRepository, paymentRepository, userRepository, clock, transaction);
        when(paymentRepository.findVerifiedByGroupWithinWindow(any(), any(), any(), any())).thenReturn(List.of());
        when(userRepository.findById(any())).thenReturn(Optional.empty());
    }

    @Test
    void reportsMissingGroupsAsNotFound() {
        AggregateId tenantId = AggregateId.newId();
        GroupId groupId = GroupId.newId();
        when(groupRepository.findById(tenantId, groupId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(tenantId, groupId))
                .isInstanceOf(CollectionGroupNotFoundException.class);
    }

    @Test
    void reportsNoActiveCycleForAnInactiveGroup() {
        SavingsGroup group = newGroup(2);
        when(groupRepository.findById(group.tenantId(), group.groupId())).thenReturn(Optional.of(group));

        CollectionSummaryResult result = service.execute(group.tenantId(), group.groupId());

        assertThat(result.cycleActive()).isFalse();
        assertThat(result.cycleNumber()).isZero();
        assertThat(result.members()).isEmpty();
        assertThat(result.contributionAmountPaise()).isEqualTo(100_000);
    }

    @Test
    void reportsEveryMemberPendingOnTheDueDateWhenNobodyHasPaid() {
        AggregateId ownerId = AggregateId.newId();
        SavingsGroup group = newGroup(ownerId, 5);
        group.activate(ownerId, NOW);
        AggregateId memberId = AggregateId.newId();
        group.joinMember(memberId, ownerId, NOW.plusSeconds(1));
        when(groupRepository.findById(group.tenantId(), group.groupId())).thenReturn(Optional.of(group));

        CollectionSummaryResult result = service.execute(group.tenantId(), group.groupId());

        assertThat(result.cycleActive()).isTrue();
        assertThat(result.cycleNumber()).isEqualTo(1);
        assertThat(result.totalMembers()).isEqualTo(2);
        assertThat(result.pendingCount()).isEqualTo(2);
        assertThat(result.paidCount()).isZero();
        assertThat(result.overdueCount()).isZero();
        assertThat(result.totalExpectedPaise()).isEqualTo(200_000);
        assertThat(result.totalCollectedPaise()).isZero();
        assertThat(result.members()).allMatch(member -> "PENDING".equals(member.status()));
    }

    @Test
    void reportsUnpaidMembersAsOverdueAfterTheDueDateHasPassed() {
        clock = () -> CYCLE_START.plusDays(5).atStartOfDay(ZoneOffset.UTC).toInstant();
        service = new GetCollectionSummaryApplicationService(
                groupRepository, paymentRepository, userRepository, clock, transaction);
        AggregateId ownerId = AggregateId.newId();
        SavingsGroup group = newGroup(ownerId, 5);
        group.activate(ownerId, NOW);
        when(groupRepository.findById(group.tenantId(), group.groupId())).thenReturn(Optional.of(group));

        CollectionSummaryResult result = service.execute(group.tenantId(), group.groupId());

        assertThat(result.overdueCount()).isEqualTo(1);
        assertThat(result.pendingCount()).isZero();
        assertThat(result.members()).allMatch(member -> "OVERDUE".equals(member.status()));
    }

    @Test
    void reportsAVerifiedPaymentAsPaidAndSumsCollectedAmounts() {
        AggregateId ownerId = AggregateId.newId();
        SavingsGroup group = newGroup(ownerId, 5);
        group.activate(ownerId, NOW);
        when(groupRepository.findById(group.tenantId(), group.groupId())).thenReturn(Optional.of(group));
        Payment payment = verifiedPayment(group.tenantId(), group.groupId().value(), ownerId,
                CYCLE_START.atStartOfDay(ZoneOffset.UTC).toInstant());
        when(paymentRepository.findVerifiedByGroupWithinWindow(any(), any(), any(), any()))
                .thenReturn(List.of(payment));

        CollectionSummaryResult result = service.execute(group.tenantId(), group.groupId());

        assertThat(result.paidCount()).isEqualTo(1);
        assertThat(result.pendingCount()).isZero();
        assertThat(result.totalCollectedPaise()).isEqualTo(100_000);
        assertThat(result.totalRemainingPaise()).isZero();
        assertThat(result.members()).singleElement()
                .satisfies(member -> {
                    assertThat(member.status()).isEqualTo("PAID");
                    assertThat(member.collectedAmountPaise()).isEqualTo(100_000);
                });
    }

    @Test
    void resolvesEachMembersDisplayNameTheSameWayTheOrganizerNameIsResolvedElsewhere() {
        AggregateId ownerId = AggregateId.newId();
        SavingsGroup group = newGroup(ownerId, 5);
        group.activate(ownerId, NOW);
        when(groupRepository.findById(group.tenantId(), group.groupId())).thenReturn(Optional.of(group));
        UserProfile ownerProfile = new UserProfile(
                ownerId, new PersonName("QA", "Tester"),
                new in.bachatsetu.backend.user.domain.model.UserContact(
                        null, new in.bachatsetu.backend.shared.domain.PhoneNumber("+919876543210")),
                null, in.bachatsetu.backend.user.domain.model.PreferredLanguage.ENGLISH, UserStatus.ACTIVE,
                List.of(), AuditInfo.createdBy(ownerId, NOW), 0);
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(ownerProfile));

        CollectionSummaryResult result = service.execute(group.tenantId(), group.groupId());

        assertThat(result.members()).singleElement()
                .satisfies(member -> assertThat(member.memberName()).isEqualTo("QA Tester"));
    }

    @Test
    void fallsBackToANullNameWhenNoProfileCanBeResolved() {
        AggregateId ownerId = AggregateId.newId();
        SavingsGroup group = newGroup(ownerId, 5);
        group.activate(ownerId, NOW);
        when(groupRepository.findById(group.tenantId(), group.groupId())).thenReturn(Optional.of(group));

        CollectionSummaryResult result = service.execute(group.tenantId(), group.groupId());

        assertThat(result.members()).singleElement()
                .satisfies(member -> assertThat(member.memberName()).isNull());
    }

    @Test
    void rejectsNullInputs() {
        AggregateId tenantId = AggregateId.newId();
        GroupId groupId = GroupId.newId();
        assertThatThrownBy(() -> service.execute(null, groupId)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> service.execute(tenantId, null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void validatesRequiredServiceDependencies() {
        assertThatThrownBy(() -> new GetCollectionSummaryApplicationService(
                null, paymentRepository, userRepository, clock, transaction))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new GetCollectionSummaryApplicationService(
                groupRepository, null, userRepository, clock, transaction))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new GetCollectionSummaryApplicationService(
                groupRepository, paymentRepository, null, clock, transaction))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new GetCollectionSummaryApplicationService(
                groupRepository, paymentRepository, userRepository, null, transaction))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new GetCollectionSummaryApplicationService(
                groupRepository, paymentRepository, userRepository, clock, null))
                .isInstanceOf(NullPointerException.class);
    }

    private Payment verifiedPayment(AggregateId tenantId, AggregateId groupId, AggregateId memberId, Instant at) {
        Payment payment = Payment.initiate(
                AggregateId.newId(), tenantId, groupId, memberId,
                new PaymentReference("PAY-" + AggregateId.newId().value().toString().replace("-", "")),
                new IdempotencyKey("collection-test-payment-01"),
                Money.inr(100_000),
                PaymentMethod.CASH, memberId, at);
        payment.verify(new ProviderReference("MANUAL", "test"), memberId, at);
        payment.pullDomainEvents();
        return payment;
    }
}
