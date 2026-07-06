package in.bachatsetu.backend.group.application;

import static in.bachatsetu.backend.group.application.ApplicationTestFixture.createCommand;
import static in.bachatsetu.backend.group.application.ApplicationTestFixture.directTransaction;
import static in.bachatsetu.backend.group.domain.GroupDomainFixtures.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.group.application.command.ActivateGroupCommand;
import in.bachatsetu.backend.group.application.command.CloseGroupCommand;
import in.bachatsetu.backend.group.application.command.CreateSavingsGroupCommand;
import in.bachatsetu.backend.group.application.command.JoinGroupCommand;
import in.bachatsetu.backend.group.application.command.RemoveMemberCommand;
import in.bachatsetu.backend.group.application.command.SuspendGroupCommand;
import in.bachatsetu.backend.group.application.exception.DuplicateGroupCodeException;
import in.bachatsetu.backend.group.application.exception.SavingsGroupApplicationException;
import in.bachatsetu.backend.group.application.exception.SavingsGroupNotFoundException;
import in.bachatsetu.backend.group.application.port.ClockPort;
import in.bachatsetu.backend.group.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.group.application.port.GroupCodeGeneratorPort;
import in.bachatsetu.backend.group.application.port.SavingsGroupRepository;
import in.bachatsetu.backend.group.application.port.TransactionPort;
import in.bachatsetu.backend.group.domain.model.GroupCode;
import in.bachatsetu.backend.group.domain.model.GroupId;
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
        CreateSavingsGroupCommand create = createCommand();
        GroupId groupId = GroupId.newId();
        AggregateId tenantId = AggregateId.newId();
        AggregateId memberId = AggregateId.newId();
        AggregateId actorId = AggregateId.newId();

        assertThat(new JoinGroupCommand(tenantId, groupId, memberId, actorId).memberId()).isEqualTo(memberId);
        assertThat(new RemoveMemberCommand(tenantId, groupId, memberId, actorId).actorId()).isEqualTo(actorId);
        assertThat(new ActivateGroupCommand(tenantId, groupId, actorId).groupId()).isEqualTo(groupId);
        assertThat(new SuspendGroupCommand(tenantId, groupId, actorId).tenantId()).isEqualTo(tenantId);
        assertThat(new CloseGroupCommand(tenantId, groupId, actorId).actorId()).isEqualTo(actorId);
        assertThat(create.name().value()).isEqualTo("Application Group");
    }

    @Test
    void commandContractsRejectNullContext() {
        CreateSavingsGroupCommand create = createCommand();
        GroupId groupId = GroupId.newId();
        AggregateId id = AggregateId.newId();

        assertThatThrownBy(() -> new CreateSavingsGroupCommand(
                        null, create.ownerId(), create.name(), create.description(), create.type(), create.rule()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CreateSavingsGroupCommand(
                        create.tenantId(), null, create.name(), create.description(), create.type(), create.rule()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CreateSavingsGroupCommand(
                        create.tenantId(), create.ownerId(), null, create.description(), create.type(), create.rule()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CreateSavingsGroupCommand(
                        create.tenantId(), create.ownerId(), create.name(), null, create.type(), create.rule()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CreateSavingsGroupCommand(
                        create.tenantId(), create.ownerId(), create.name(), create.description(), null, create.rule()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CreateSavingsGroupCommand(
                        create.tenantId(), create.ownerId(), create.name(), create.description(), create.type(), null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new JoinGroupCommand(null, groupId, id, id))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new JoinGroupCommand(id, null, id, id))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new JoinGroupCommand(id, groupId, null, id))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new JoinGroupCommand(id, groupId, id, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new RemoveMemberCommand(null, groupId, id, id))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new RemoveMemberCommand(id, null, id, id))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new RemoveMemberCommand(id, groupId, null, id))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new RemoveMemberCommand(id, groupId, id, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void lifecycleCommandsRejectNullContext() {
        GroupId groupId = GroupId.newId();
        AggregateId id = AggregateId.newId();

        assertThatThrownBy(() -> new ActivateGroupCommand(null, groupId, id))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ActivateGroupCommand(id, null, id))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ActivateGroupCommand(id, groupId, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new SuspendGroupCommand(null, groupId, id))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new SuspendGroupCommand(id, null, id))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new SuspendGroupCommand(id, groupId, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CloseGroupCommand(null, groupId, id))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CloseGroupCommand(id, null, id))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CloseGroupCommand(id, groupId, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void outboundPortsExposeRequiredOperations() {
        Set<String> repositoryMethods = Arrays.stream(SavingsGroupRepository.class.getDeclaredMethods())
                .map(Method::getName)
                .collect(Collectors.toSet());

        assertThat(repositoryMethods).containsExactlyInAnyOrder(
                "save", "findById", "findByGroupCode", "existsByGroupCode", "findAll", "delete");

        GroupId groupId = GroupId.newId();
        GroupCodeGeneratorPort generator = ignored -> new GroupCode("BS-PORT");
        ClockPort clock = () -> NOW;
        AtomicReference<List<?>> published = new AtomicReference<>();
        DomainEventPublisherPort publisher = published::set;
        TransactionPort transaction = directTransaction();

        assertThat(generator.generate(groupId)).isEqualTo(new GroupCode("BS-PORT"));
        assertThat(clock.now()).isEqualTo(NOW);
        publisher.publish(List.of());
        assertThat(published.get()).isEmpty();
        assertThat(transaction.execute(() -> "committed")).isEqualTo("committed");
    }

    @Test
    void useCaseAndExceptionContractsArePresent() {
        List<Class<?>> useCases = List.of(
                in.bachatsetu.backend.group.application.usecase.CreateSavingsGroupUseCase.class,
                in.bachatsetu.backend.group.application.usecase.JoinGroupUseCase.class,
                in.bachatsetu.backend.group.application.usecase.RemoveMemberUseCase.class,
                in.bachatsetu.backend.group.application.usecase.ActivateGroupUseCase.class,
                in.bachatsetu.backend.group.application.usecase.SuspendGroupUseCase.class,
                in.bachatsetu.backend.group.application.usecase.CloseGroupUseCase.class,
                in.bachatsetu.backend.group.application.usecase.GetSavingsGroupUseCase.class,
                in.bachatsetu.backend.group.application.usecase.ListSavingsGroupsUseCase.class);

        assertThat(useCases).allMatch(Class::isInterface);
        assertThat(new SavingsGroupApplicationException("application failure")).hasMessage("application failure");
        assertThat(new SavingsGroupNotFoundException("missing")).isInstanceOf(SavingsGroupApplicationException.class);
        assertThat(new DuplicateGroupCodeException("duplicate")).isInstanceOf(SavingsGroupApplicationException.class);
    }
}
