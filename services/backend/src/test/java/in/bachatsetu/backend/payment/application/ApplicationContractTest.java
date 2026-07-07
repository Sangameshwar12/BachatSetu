package in.bachatsetu.backend.payment.application;

import static in.bachatsetu.backend.payment.application.ApplicationTestFixture.createCommand;
import static in.bachatsetu.backend.payment.application.ApplicationTestFixture.directTransaction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.payment.application.command.CreatePaymentCommand;
import in.bachatsetu.backend.payment.application.command.UpdatePaymentStatusCommand;
import in.bachatsetu.backend.payment.application.exception.PaymentApplicationException;
import in.bachatsetu.backend.payment.application.exception.PaymentNotFoundException;
import in.bachatsetu.backend.payment.application.port.ClockPort;
import in.bachatsetu.backend.payment.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.payment.application.port.TransactionPort;
import in.bachatsetu.backend.payment.domain.model.PaymentStatus;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ApplicationContractTest {

    private static final Instant NOW = Instant.parse("2026-07-07T08:00:00Z");

    @Test
    void commandContractsCarryRequiredContext() {
        CreatePaymentCommand create = createCommand();
        AggregateId id = AggregateId.newId();

        assertThat(create.method()).isNotNull();
        assertThat(new UpdatePaymentStatusCommand(id, id, PaymentStatus.VERIFIED, null, null, id).targetStatus())
                .isEqualTo(PaymentStatus.VERIFIED);
    }

    @Test
    void commandContractsRejectNullRequiredContext() {
        CreatePaymentCommand create = createCommand();
        AggregateId id = AggregateId.newId();

        assertThatThrownBy(() -> new CreatePaymentCommand(
                        null, create.groupId(), create.memberId(), create.idempotencyKey(), create.amount(),
                        create.method(), create.actorId()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CreatePaymentCommand(
                        create.tenantId(), null, create.memberId(), create.idempotencyKey(), create.amount(),
                        create.method(), create.actorId()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CreatePaymentCommand(
                        create.tenantId(), create.groupId(), null, create.idempotencyKey(), create.amount(),
                        create.method(), create.actorId()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CreatePaymentCommand(
                        create.tenantId(), create.groupId(), create.memberId(), null, create.amount(),
                        create.method(), create.actorId()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CreatePaymentCommand(
                        create.tenantId(), create.groupId(), create.memberId(), create.idempotencyKey(), null,
                        create.method(), create.actorId()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CreatePaymentCommand(
                        create.tenantId(), create.groupId(), create.memberId(), create.idempotencyKey(),
                        create.amount(), null, create.actorId()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CreatePaymentCommand(
                        create.tenantId(), create.groupId(), create.memberId(), create.idempotencyKey(),
                        create.amount(), create.method(), null))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new UpdatePaymentStatusCommand(null, id, PaymentStatus.VERIFIED, null, null, id))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new UpdatePaymentStatusCommand(id, null, PaymentStatus.VERIFIED, null, null, id))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new UpdatePaymentStatusCommand(id, id, null, null, null, id))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new UpdatePaymentStatusCommand(id, id, PaymentStatus.VERIFIED, null, null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void outboundPortsExposeRequiredOperations() {
        ClockPort clock = () -> NOW;
        AtomicReference<List<?>> published = new AtomicReference<>();
        DomainEventPublisherPort publisher = published::set;
        TransactionPort transaction = directTransaction();

        assertThat(clock.now()).isEqualTo(NOW);
        publisher.publish(List.of());
        assertThat(published.get()).isEmpty();
        assertThat(transaction.execute(() -> "committed")).isEqualTo("committed");
    }

    @Test
    void useCaseAndExceptionContractsArePresent() {
        List<Class<?>> useCases = List.of(
                in.bachatsetu.backend.payment.application.usecase.CreatePaymentUseCase.class,
                in.bachatsetu.backend.payment.application.usecase.GetPaymentUseCase.class,
                in.bachatsetu.backend.payment.application.usecase.ListPaymentsUseCase.class,
                in.bachatsetu.backend.payment.application.usecase.UpdatePaymentStatusUseCase.class);

        assertThat(useCases).allMatch(Class::isInterface);
        assertThat(new PaymentApplicationException("application failure")).hasMessage("application failure");
        assertThat(new PaymentNotFoundException("missing")).isInstanceOf(PaymentApplicationException.class);
    }

    @Test
    void domainPortExposesTenantScopedLookupAndPagination() {
        Set<String> methods = Arrays.stream(in.bachatsetu.backend.payment.domain.port.PaymentRepository.class
                        .getDeclaredMethods())
                .map(Method::getName)
                .collect(Collectors.toSet());

        assertThat(methods).contains(
                "findById", "findByReference", "findByIdempotencyKey", "findByProviderReference",
                "findPage", "save");
    }
}
