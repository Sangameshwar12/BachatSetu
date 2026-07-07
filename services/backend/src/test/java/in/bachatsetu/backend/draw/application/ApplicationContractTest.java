package in.bachatsetu.backend.draw.application;

import static in.bachatsetu.backend.draw.application.ApplicationTestFixture.NOW;
import static in.bachatsetu.backend.draw.application.ApplicationTestFixture.createCommand;
import static in.bachatsetu.backend.draw.application.ApplicationTestFixture.directTransaction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.draw.application.command.CloseDrawCommand;
import in.bachatsetu.backend.draw.application.command.ConductDrawCommand;
import in.bachatsetu.backend.draw.application.command.CreateDrawCommand;
import in.bachatsetu.backend.draw.application.exception.DrawApplicationException;
import in.bachatsetu.backend.draw.application.exception.DrawNotFoundException;
import in.bachatsetu.backend.draw.application.port.ClockPort;
import in.bachatsetu.backend.draw.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.draw.application.port.TransactionPort;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ApplicationContractTest {

    @Test
    void commandContractsCarryRequiredContext() {
        CreateDrawCommand create = createCommand();
        AggregateId id = AggregateId.newId();

        assertThat(create.number()).isNotNull();
        assertThat(new ConductDrawCommand(id, id, id).drawId()).isEqualTo(id);
        assertThat(new CloseDrawCommand(id, id, id, id).winnerId()).isEqualTo(id);
    }

    @Test
    void commandContractsRejectNullContext() {
        CreateDrawCommand create = createCommand();
        AggregateId id = AggregateId.newId();

        assertThatThrownBy(() -> new CreateDrawCommand(
                        null, create.groupId(), create.cycleId(), create.number(), create.type(),
                        create.scheduledAt(), create.actorId()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CreateDrawCommand(
                        create.tenantId(), null, create.cycleId(), create.number(), create.type(),
                        create.scheduledAt(), create.actorId()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CreateDrawCommand(
                        create.tenantId(), create.groupId(), null, create.number(), create.type(),
                        create.scheduledAt(), create.actorId()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CreateDrawCommand(
                        create.tenantId(), create.groupId(), create.cycleId(), null, create.type(),
                        create.scheduledAt(), create.actorId()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CreateDrawCommand(
                        create.tenantId(), create.groupId(), create.cycleId(), create.number(), null,
                        create.scheduledAt(), create.actorId()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CreateDrawCommand(
                        create.tenantId(), create.groupId(), create.cycleId(), create.number(), create.type(),
                        null, create.actorId()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CreateDrawCommand(
                        create.tenantId(), create.groupId(), create.cycleId(), create.number(), create.type(),
                        create.scheduledAt(), null))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new ConductDrawCommand(null, id, id)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ConductDrawCommand(id, null, id)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ConductDrawCommand(id, id, null)).isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new CloseDrawCommand(null, id, id, id)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CloseDrawCommand(id, null, id, id)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CloseDrawCommand(id, id, null, id)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CloseDrawCommand(id, id, id, null)).isInstanceOf(NullPointerException.class);
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
                in.bachatsetu.backend.draw.application.usecase.CreateDrawUseCase.class,
                in.bachatsetu.backend.draw.application.usecase.GetDrawUseCase.class,
                in.bachatsetu.backend.draw.application.usecase.ListDrawsUseCase.class,
                in.bachatsetu.backend.draw.application.usecase.ConductDrawUseCase.class,
                in.bachatsetu.backend.draw.application.usecase.CloseDrawUseCase.class);

        assertThat(useCases).allMatch(Class::isInterface);
        assertThat(new DrawApplicationException("application failure")).hasMessage("application failure");
        assertThat(new DrawNotFoundException("missing")).isInstanceOf(DrawApplicationException.class);
    }

    @Test
    void domainPortExposesTenantScopedLookupAndPagination() {
        Set<String> methods = Arrays.stream(in.bachatsetu.backend.draw.domain.port.DrawRepository.class
                        .getDeclaredMethods())
                .map(Method::getName)
                .collect(Collectors.toSet());

        assertThat(methods).contains(
                "findById", "findByGroupAndNumber", "findByCycleId", "findPage", "save");
    }
}
