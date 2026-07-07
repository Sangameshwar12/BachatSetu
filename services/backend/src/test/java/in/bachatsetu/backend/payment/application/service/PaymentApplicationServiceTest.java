package in.bachatsetu.backend.payment.application.service;

import static in.bachatsetu.backend.payment.application.ApplicationTestFixture.createCommand;
import static in.bachatsetu.backend.payment.application.ApplicationTestFixture.directTransaction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.payment.application.command.CreatePaymentCommand;
import in.bachatsetu.backend.payment.application.command.UpdatePaymentStatusCommand;
import in.bachatsetu.backend.payment.application.exception.PaymentNotFoundException;
import in.bachatsetu.backend.payment.application.mapper.PaymentApplicationMapper;
import in.bachatsetu.backend.payment.application.port.ClockPort;
import in.bachatsetu.backend.payment.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.payment.application.port.TransactionPort;
import in.bachatsetu.backend.payment.application.query.PaymentResult;
import in.bachatsetu.backend.payment.application.query.PaymentSummary;
import in.bachatsetu.backend.payment.application.usecase.CreatePaymentUseCase;
import in.bachatsetu.backend.payment.application.usecase.GetPaymentUseCase;
import in.bachatsetu.backend.payment.application.usecase.ListPaymentsUseCase;
import in.bachatsetu.backend.payment.application.usecase.UpdatePaymentStatusUseCase;
import in.bachatsetu.backend.payment.domain.event.PaymentInitiated;
import in.bachatsetu.backend.payment.domain.event.PaymentStatusChanged;
import in.bachatsetu.backend.payment.domain.exception.InvalidPaymentStateException;
import in.bachatsetu.backend.payment.domain.factory.PaymentFactory;
import in.bachatsetu.backend.payment.domain.model.IdempotencyKey;
import in.bachatsetu.backend.payment.domain.model.Payment;
import in.bachatsetu.backend.payment.domain.model.PaymentMethod;
import in.bachatsetu.backend.payment.domain.model.PaymentReference;
import in.bachatsetu.backend.payment.domain.model.PaymentStatus;
import in.bachatsetu.backend.payment.domain.model.ProviderReference;
import in.bachatsetu.backend.payment.domain.port.PaymentPage;
import in.bachatsetu.backend.payment.domain.port.PaymentPageRequest;
import in.bachatsetu.backend.payment.domain.port.PaymentRepository;
import in.bachatsetu.backend.payment.domain.port.PaymentSortField;
import in.bachatsetu.backend.payment.domain.port.SortDirection;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.DomainEvent;
import in.bachatsetu.backend.shared.domain.Money;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PaymentApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-06T08:00:00Z");

    private PaymentRepository repository;
    private PaymentFactory paymentFactory;
    private DomainEventPublisherPort publisher;
    private ClockPort clock;
    private TransactionPort transaction;
    private PaymentApplicationMapper mapper;

    @BeforeEach
    void setUp() {
        repository = mock(PaymentRepository.class);
        paymentFactory = new PaymentFactory(Clock.fixed(NOW.plusSeconds(10), ZoneOffset.UTC));
        publisher = mock(DomainEventPublisherPort.class);
        clock = () -> NOW.plusSeconds(10);
        transaction = directTransaction();
        mapper = new PaymentApplicationMapper();
    }

    @Test
    void createsSavesPublishesAndMapsPayment() {
        CreatePaymentCommand command = createCommand();
        when(repository.findByIdempotencyKey(command.tenantId(), command.idempotencyKey()))
                .thenReturn(Optional.empty());
        CreatePaymentUseCase service = new CreatePaymentApplicationService(
                repository, paymentFactory, publisher, transaction, mapper);

        PaymentResult result = service.execute(command);

        assertThat(result.status()).isEqualTo("INITIATED");
        assertThat(result.amountPaise()).isEqualTo(command.amount().minorUnits());
        verify(repository).save(any(Payment.class));
        assertPublishedEvents(PaymentInitiated.class);
    }

    @Test
    void returnsExistingPaymentForARepeatedIdempotencyKey() {
        CreatePaymentCommand command = createCommand();
        Payment existing = newPayment(command.actorId());
        when(repository.findByIdempotencyKey(command.tenantId(), command.idempotencyKey()))
                .thenReturn(Optional.of(existing));
        CreatePaymentUseCase service = new CreatePaymentApplicationService(
                repository, paymentFactory, publisher, transaction, mapper);

        PaymentResult result = service.execute(command);

        assertThat(result.paymentId()).isEqualTo(existing.id().value());
        verify(repository, never()).save(any());
        verify(publisher, never()).publish(any());
    }

    @Test
    void retrievesTenantScopedPayment() {
        AggregateId tenantId = AggregateId.newId();
        Payment payment = newPayment(AggregateId.newId());
        when(repository.findById(tenantId, payment.id())).thenReturn(Optional.of(payment));
        GetPaymentUseCase service = new GetPaymentApplicationService(repository, transaction, mapper);

        PaymentResult result = service.execute(tenantId, payment.id());

        assertThat(result.paymentId()).isEqualTo(payment.id().value());
    }

    @Test
    void tenantScopedLookupHidesPaymentsFromOtherTenants() {
        AggregateId tenantId = AggregateId.newId();
        Payment payment = newPayment(AggregateId.newId());
        when(repository.findById(tenantId, payment.id())).thenReturn(Optional.empty());
        GetPaymentUseCase service = new GetPaymentApplicationService(repository, transaction, mapper);

        assertThatThrownBy(() -> service.execute(tenantId, payment.id()))
                .isInstanceOf(PaymentNotFoundException.class);
    }

    @Test
    void listsTenantScopedPaymentSummaries() {
        AggregateId tenantId = AggregateId.newId();
        Payment first = newPayment(AggregateId.newId());
        Payment second = newPayment(AggregateId.newId());
        PaymentPageRequest pageRequest = new PaymentPageRequest(0, 20, PaymentSortField.CREATED_AT, SortDirection.ASC);
        when(repository.findPage(tenantId, pageRequest))
                .thenReturn(new PaymentPage<>(List.of(first, second), 0, 20, 2));
        ListPaymentsUseCase service = new ListPaymentsApplicationService(repository, transaction, mapper);

        PaymentPage<PaymentSummary> page = service.execute(tenantId, pageRequest);

        assertThat(page.content()).hasSize(2);
        assertThat(page.totalElements()).isEqualTo(2);
        assertThatThrownBy(() -> page.content().add(mapper.toSummary(first)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void startsAnAttemptAndPublishesStatusChange() {
        AggregateId tenantId = AggregateId.newId();
        Payment payment = newPayment(AggregateId.newId());
        when(repository.findById(tenantId, payment.id())).thenReturn(Optional.of(payment));
        UpdatePaymentStatusUseCase service = new UpdatePaymentStatusApplicationService(
                repository, publisher, clock, transaction, mapper);

        PaymentResult result = service.execute(new UpdatePaymentStatusCommand(
                tenantId, payment.id(), PaymentStatus.PENDING_PROVIDER, null, null, payment.tenantId()));

        assertThat(result.status()).isEqualTo("PENDING_PROVIDER");
        verify(repository).save(payment);
        assertPublishedEvents(PaymentStatusChanged.class);
    }

    @Test
    void verifiesAPaymentWithAProviderReference() {
        AggregateId tenantId = AggregateId.newId();
        Payment payment = newPayment(AggregateId.newId());
        when(repository.findById(tenantId, payment.id())).thenReturn(Optional.of(payment));
        UpdatePaymentStatusUseCase service = new UpdatePaymentStatusApplicationService(
                repository, publisher, clock, transaction, mapper);
        ProviderReference reference = new ProviderReference("test-provider", "txn-001");

        PaymentResult result = service.execute(new UpdatePaymentStatusCommand(
                tenantId, payment.id(), PaymentStatus.VERIFIED, reference, null, payment.tenantId()));

        assertThat(result.status()).isEqualTo("VERIFIED");
        assertThat(result.reconciliationStatus()).isEqualTo("MATCHED");
        assertPublishedEvents(PaymentStatusChanged.class);
    }

    @Test
    void failsAPaymentWithAFailureCode() {
        AggregateId tenantId = AggregateId.newId();
        Payment payment = newPayment(AggregateId.newId());
        when(repository.findById(tenantId, payment.id())).thenReturn(Optional.of(payment));
        UpdatePaymentStatusUseCase service = new UpdatePaymentStatusApplicationService(
                repository, publisher, clock, transaction, mapper);

        PaymentResult result = service.execute(new UpdatePaymentStatusCommand(
                tenantId, payment.id(), PaymentStatus.FAILED, null, "provider-declined", payment.tenantId()));

        assertThat(result.status()).isEqualTo("FAILED");
        assertPublishedEvents(PaymentStatusChanged.class);
    }

    @Test
    void rejectsAnUnsupportedTargetStatus() {
        AggregateId tenantId = AggregateId.newId();
        Payment payment = newPayment(AggregateId.newId());
        when(repository.findById(tenantId, payment.id())).thenReturn(Optional.of(payment));
        UpdatePaymentStatusUseCase service = new UpdatePaymentStatusApplicationService(
                repository, publisher, clock, transaction, mapper);

        assertThatThrownBy(() -> service.execute(new UpdatePaymentStatusCommand(
                        tenantId, payment.id(), PaymentStatus.CANCELLED, null, null, payment.tenantId())))
                .isInstanceOf(IllegalArgumentException.class);
        verify(repository, never()).save(any());
        verify(publisher, never()).publish(any());
    }

    @Test
    void rejectsAnInvalidStatusTransitionWithoutSavingOrPublishing() {
        AggregateId tenantId = AggregateId.newId();
        Payment payment = newPayment(AggregateId.newId());
        payment.startAttempt(payment.tenantId(), NOW.plusSeconds(1));
        payment.verify(new ProviderReference("test-provider", "txn-001"), payment.tenantId(), NOW.plusSeconds(2));
        payment.pullDomainEvents();
        when(repository.findById(tenantId, payment.id())).thenReturn(Optional.of(payment));
        UpdatePaymentStatusUseCase service = new UpdatePaymentStatusApplicationService(
                repository, publisher, clock, transaction, mapper);

        assertThatThrownBy(() -> service.execute(new UpdatePaymentStatusCommand(
                        tenantId, payment.id(), PaymentStatus.FAILED, null, "code", payment.tenantId())))
                .isInstanceOf(InvalidPaymentStateException.class);
        verify(repository, never()).save(any());
        verify(publisher, never()).publish(any());
    }

    @Test
    void reportsMissingPaymentOnUpdateWithoutSavingOrPublishing() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId paymentId = AggregateId.newId();
        when(repository.findById(tenantId, paymentId)).thenReturn(Optional.empty());
        UpdatePaymentStatusUseCase service = new UpdatePaymentStatusApplicationService(
                repository, publisher, clock, transaction, mapper);

        assertThatThrownBy(() -> service.execute(new UpdatePaymentStatusCommand(
                        tenantId, paymentId, PaymentStatus.VERIFIED, new ProviderReference("p", "t"), null,
                        AggregateId.newId())))
                .isInstanceOf(PaymentNotFoundException.class);
        verify(repository, never()).save(any());
        verify(publisher, never()).publish(any());
    }

    @Test
    void rejectsNullUseCaseInputs() {
        assertThatThrownBy(() -> new CreatePaymentApplicationService(
                        repository, paymentFactory, publisher, transaction, mapper)
                .execute(null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new GetPaymentApplicationService(repository, transaction, mapper)
                        .execute(null, AggregateId.newId()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new GetPaymentApplicationService(repository, transaction, mapper)
                        .execute(AggregateId.newId(), null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ListPaymentsApplicationService(repository, transaction, mapper)
                        .execute(null, new PaymentPageRequest(0, 20, PaymentSortField.CREATED_AT, SortDirection.ASC)))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ListPaymentsApplicationService(repository, transaction, mapper)
                        .execute(AggregateId.newId(), null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new UpdatePaymentStatusApplicationService(
                        repository, publisher, clock, transaction, mapper)
                .execute(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void validatesRequiredServiceDependencies() {
        assertThatThrownBy(() -> new CreatePaymentApplicationService(
                        null, paymentFactory, publisher, transaction, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CreatePaymentApplicationService(
                        repository, null, publisher, transaction, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CreatePaymentApplicationService(
                        repository, paymentFactory, publisher, null, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CreatePaymentApplicationService(
                        repository, paymentFactory, publisher, transaction, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new GetPaymentApplicationService(null, transaction, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new GetPaymentApplicationService(repository, null, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new GetPaymentApplicationService(repository, transaction, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ListPaymentsApplicationService(null, transaction, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ListPaymentsApplicationService(repository, null, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ListPaymentsApplicationService(repository, transaction, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new UpdatePaymentStatusApplicationService(
                        null, publisher, clock, transaction, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new UpdatePaymentStatusApplicationService(
                        repository, null, clock, transaction, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new UpdatePaymentStatusApplicationService(
                        repository, publisher, null, transaction, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new UpdatePaymentStatusApplicationService(
                        repository, publisher, clock, null, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new UpdatePaymentStatusApplicationService(
                        repository, publisher, clock, transaction, null))
                .isInstanceOf(NullPointerException.class);
    }

    private Payment newPayment(AggregateId actorId) {
        Payment payment = Payment.initiate(
                AggregateId.newId(),
                AggregateId.newId(),
                AggregateId.newId(),
                AggregateId.newId(),
                new PaymentReference("PAY-" + AggregateId.newId().value().toString().replace("-", "")
                        .toUpperCase(Locale.ROOT)),
                new IdempotencyKey("checkout-attempt-existing"),
                Money.inr(100_000),
                PaymentMethod.UPI,
                actorId,
                NOW);
        payment.pullDomainEvents();
        return payment;
    }

    @SafeVarargs
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void assertPublishedEvents(Class<? extends DomainEvent>... eventTypes) {
        ArgumentCaptor<List<DomainEvent>> captor = ArgumentCaptor.forClass((Class) List.class);
        verify(publisher).publish(captor.capture());
        assertThat(captor.getValue()).hasSize(eventTypes.length);
        for (Class<? extends DomainEvent> eventType : eventTypes) {
            assertThat(captor.getValue()).anyMatch(eventType::isInstance);
        }
    }
}
