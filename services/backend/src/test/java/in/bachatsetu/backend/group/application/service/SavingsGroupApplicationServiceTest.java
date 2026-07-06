package in.bachatsetu.backend.group.application.service;

import static in.bachatsetu.backend.group.application.ApplicationTestFixture.activeGroup;
import static in.bachatsetu.backend.group.application.ApplicationTestFixture.createCommand;
import static in.bachatsetu.backend.group.application.ApplicationTestFixture.directTransaction;
import static in.bachatsetu.backend.group.domain.GroupDomainFixtures.NOW;
import static in.bachatsetu.backend.group.domain.GroupDomainFixtures.newGroup;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.group.application.command.ActivateGroupCommand;
import in.bachatsetu.backend.group.application.command.CloseGroupCommand;
import in.bachatsetu.backend.group.application.command.CreateSavingsGroupCommand;
import in.bachatsetu.backend.group.application.command.JoinGroupCommand;
import in.bachatsetu.backend.group.application.command.RemoveMemberCommand;
import in.bachatsetu.backend.group.application.command.SuspendGroupCommand;
import in.bachatsetu.backend.group.application.exception.DuplicateGroupCodeException;
import in.bachatsetu.backend.group.application.exception.SavingsGroupNotFoundException;
import in.bachatsetu.backend.group.application.mapper.SavingsGroupApplicationMapper;
import in.bachatsetu.backend.group.application.port.ClockPort;
import in.bachatsetu.backend.group.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.group.application.port.GroupCodeGeneratorPort;
import in.bachatsetu.backend.group.application.port.SavingsGroupRepository;
import in.bachatsetu.backend.group.application.port.TransactionPort;
import in.bachatsetu.backend.group.application.query.SavingsGroupResult;
import in.bachatsetu.backend.group.application.query.SavingsGroupSummary;
import in.bachatsetu.backend.group.application.usecase.ActivateGroupUseCase;
import in.bachatsetu.backend.group.application.usecase.CloseGroupUseCase;
import in.bachatsetu.backend.group.application.usecase.CreateSavingsGroupUseCase;
import in.bachatsetu.backend.group.application.usecase.GetSavingsGroupUseCase;
import in.bachatsetu.backend.group.application.usecase.JoinGroupUseCase;
import in.bachatsetu.backend.group.application.usecase.ListSavingsGroupsUseCase;
import in.bachatsetu.backend.group.application.usecase.RemoveMemberUseCase;
import in.bachatsetu.backend.group.application.usecase.SuspendGroupUseCase;
import in.bachatsetu.backend.group.domain.event.GroupActivated;
import in.bachatsetu.backend.group.domain.event.GroupClosed;
import in.bachatsetu.backend.group.domain.event.GroupSuspended;
import in.bachatsetu.backend.group.domain.event.MemberJoined;
import in.bachatsetu.backend.group.domain.event.MemberRemoved;
import in.bachatsetu.backend.group.domain.event.SavingsGroupCreated;
import in.bachatsetu.backend.group.domain.model.GroupCode;
import in.bachatsetu.backend.group.domain.model.GroupId;
import in.bachatsetu.backend.group.domain.model.GroupStatus;
import in.bachatsetu.backend.group.domain.model.SavingsGroup;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.DomainEvent;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SavingsGroupApplicationServiceTest {

    private SavingsGroupRepository repository;
    private DomainEventPublisherPort publisher;
    private ClockPort clock;
    private TransactionPort transaction;
    private SavingsGroupApplicationMapper mapper;

    @BeforeEach
    void setUp() {
        repository = mock(SavingsGroupRepository.class);
        publisher = mock(DomainEventPublisherPort.class);
        clock = () -> NOW.plusSeconds(10);
        transaction = directTransaction();
        mapper = new SavingsGroupApplicationMapper();
    }

    @Test
    void createsSavesPublishesAndMapsGroup() {
        CreateSavingsGroupCommand command = createCommand();
        GroupCodeGeneratorPort codeGenerator = groupId -> new GroupCode("BS-APP-CREATE");
        CreateSavingsGroupUseCase service = new CreateSavingsGroupApplicationService(
                repository, codeGenerator, publisher, clock, transaction, mapper);

        SavingsGroupResult result = service.execute(command);

        assertThat(result.groupCode()).isEqualTo("BS-APP-CREATE");
        assertThat(result.status()).isEqualTo(GroupStatus.INACTIVE.name());
        assertThat(result.ownerId()).isEqualTo(command.ownerId().value().value());
        assertThat(result.createdAt()).isEqualTo(NOW.plusSeconds(10));
        verify(repository).existsByGroupCode(command.tenantId(), new GroupCode("BS-APP-CREATE"));
        verify(repository).save(any(SavingsGroup.class));
        assertPublishedEvent(SavingsGroupCreated.class);
    }

    @Test
    void rejectsDuplicateOrNullGeneratedCode() {
        CreateSavingsGroupCommand command = createCommand();
        when(repository.existsByGroupCode(command.tenantId(), new GroupCode("BS-DUPLICATE")))
                .thenReturn(true);
        CreateSavingsGroupApplicationService duplicateService = new CreateSavingsGroupApplicationService(
                repository,
                groupId -> new GroupCode("BS-DUPLICATE"),
                publisher,
                clock,
                transaction,
                mapper);

        assertThatThrownBy(() -> duplicateService.execute(command))
                .isInstanceOf(DuplicateGroupCodeException.class);
        verify(repository, never()).save(any());

        CreateSavingsGroupApplicationService nullCodeService = new CreateSavingsGroupApplicationService(
                repository, groupId -> null, publisher, clock, transaction, mapper);
        assertThatThrownBy(() -> nullCodeService.execute(command)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void doesNotPublishWhenPersistenceFails() {
        AtomicReference<SavingsGroup> attempted = new AtomicReference<>();
        org.mockito.Mockito.doAnswer(invocation -> {
                    attempted.set(invocation.getArgument(0));
                    throw new IllegalStateException("storage unavailable");
                })
                .when(repository)
                .save(any(SavingsGroup.class));
        CreateSavingsGroupApplicationService service = new CreateSavingsGroupApplicationService(
                repository,
                groupId -> new GroupCode("BS-SAVE-FAIL"),
                publisher,
                clock,
                transaction,
                mapper);

        assertThatThrownBy(() -> service.execute(createCommand())).isInstanceOf(IllegalStateException.class);

        verify(publisher, never()).publish(any());
        assertThat(attempted.get().domainEvents()).singleElement().isInstanceOf(SavingsGroupCreated.class);
    }

    @Test
    void joinsAndRemovesMemberThroughAggregate() {
        SavingsGroup group = activeGroup();
        AggregateId tenantId = group.tenantId();
        AggregateId memberId = AggregateId.newId();
        AggregateId actorId = group.organizerId();
        when(repository.findById(tenantId, group.groupId())).thenReturn(Optional.of(group));
        JoinGroupUseCase joinService = new JoinGroupApplicationService(
                repository, publisher, clock, transaction, mapper);

        SavingsGroupResult joined = joinService.execute(
                new JoinGroupCommand(tenantId, group.groupId(), memberId, actorId));

        assertThat(joined.activeMemberCount()).isEqualTo(2);
        assertPublishedEvent(MemberJoined.class);

        RemoveMemberUseCase removeService = new RemoveMemberApplicationService(
                repository, publisher, () -> NOW.plusSeconds(11), transaction, mapper);
        SavingsGroupResult removed = removeService.execute(
                new RemoveMemberCommand(tenantId, group.groupId(), memberId, actorId));

        assertThat(removed.activeMemberCount()).isEqualTo(1);
        assertThat(removed.members()).filteredOn(member -> member.memberId().equals(memberId.value()))
                .singleElement()
                .satisfies(member -> assertThat(member.active()).isFalse());
        assertPublishedEvent(MemberRemoved.class);
    }

    @Test
    void orchestratesLifecycleUseCases() {
        SavingsGroup group = newGroup(5);
        group.pullDomainEvents();
        when(repository.findById(group.tenantId(), group.groupId())).thenReturn(Optional.of(group));

        ActivateGroupUseCase activate = new ActivateGroupApplicationService(
                repository, publisher, clock, transaction, mapper);
        SavingsGroupResult activated = activate.execute(
                new ActivateGroupCommand(group.tenantId(), group.groupId(), group.organizerId()));
        assertThat(activated.status()).isEqualTo(GroupStatus.ACTIVE.name());
        assertPublishedEvent(GroupActivated.class);

        SuspendGroupUseCase suspend = new SuspendGroupApplicationService(
                repository, publisher, () -> NOW.plusSeconds(11), transaction, mapper);
        SavingsGroupResult suspended = suspend.execute(
                new SuspendGroupCommand(group.tenantId(), group.groupId(), group.organizerId()));
        assertThat(suspended.status()).isEqualTo(GroupStatus.SUSPENDED.name());
        assertPublishedEvent(GroupSuspended.class);

        SavingsGroup closable = newGroup(5);
        closable.pullDomainEvents();
        when(repository.findById(closable.tenantId(), closable.groupId())).thenReturn(Optional.of(closable));
        CloseGroupUseCase close = new CloseGroupApplicationService(
                repository, publisher, clock, transaction, mapper);
        SavingsGroupResult closed = close.execute(
                new CloseGroupCommand(closable.tenantId(), closable.groupId(), closable.organizerId()));
        assertThat(closed.status()).isEqualTo(GroupStatus.CLOSED.name());
        assertPublishedEvent(GroupClosed.class);
    }

    @Test
    void retrievesAndListsTenantGroups() {
        SavingsGroup first = newGroup(5);
        SavingsGroup second = newGroup(7);
        AggregateId tenantId = first.tenantId();
        when(repository.findById(tenantId, first.groupId())).thenReturn(Optional.of(first));
        when(repository.findAll(tenantId)).thenReturn(List.of(first, second));
        GetSavingsGroupUseCase getService = new GetSavingsGroupApplicationService(repository, transaction, mapper);
        ListSavingsGroupsUseCase listService = new ListSavingsGroupsApplicationService(repository, transaction, mapper);

        SavingsGroupResult result = getService.execute(tenantId, first.groupId());
        List<SavingsGroupSummary> summaries = listService.execute(tenantId);

        assertThat(result.groupId()).isEqualTo(first.id().value());
        assertThat(summaries).hasSize(2).extracting(SavingsGroupSummary::maximumMembers)
                .containsExactly(5, 7);
        assertThatThrownBy(() -> summaries.add(mapper.toSummary(first)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void reportsMissingGroupsWithoutSavingOrPublishing() {
        AggregateId tenantId = AggregateId.newId();
        GroupId groupId = GroupId.newId();
        JoinGroupApplicationService join = new JoinGroupApplicationService(
                repository, publisher, clock, transaction, mapper);
        GetSavingsGroupApplicationService get = new GetSavingsGroupApplicationService(
                repository, transaction, mapper);

        assertThatThrownBy(() -> join.execute(new JoinGroupCommand(
                        tenantId, groupId, AggregateId.newId(), AggregateId.newId())))
                .isInstanceOf(SavingsGroupNotFoundException.class);
        assertThatThrownBy(() -> get.execute(tenantId, groupId))
                .isInstanceOf(SavingsGroupNotFoundException.class);
        verify(repository, never()).save(any());
        verify(publisher, never()).publish(any());
    }

    @Test
    void rejectsNullUseCaseInputs() {
        assertThatThrownBy(() -> new CreateSavingsGroupApplicationService(
                        repository,
                        groupId -> new GroupCode("BS-NULL"),
                        publisher,
                        clock,
                        transaction,
                        mapper)
                .execute(null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new JoinGroupApplicationService(
                        repository, publisher, clock, transaction, mapper)
                .execute(null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new RemoveMemberApplicationService(
                        repository, publisher, clock, transaction, mapper)
                .execute(null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ActivateGroupApplicationService(
                        repository, publisher, clock, transaction, mapper)
                .execute(null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new SuspendGroupApplicationService(
                        repository, publisher, clock, transaction, mapper)
                .execute(null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CloseGroupApplicationService(
                        repository, publisher, clock, transaction, mapper)
                .execute(null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new GetSavingsGroupApplicationService(repository, transaction, mapper)
                        .execute(null, GroupId.newId()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new GetSavingsGroupApplicationService(repository, transaction, mapper)
                        .execute(AggregateId.newId(), null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ListSavingsGroupsApplicationService(repository, transaction, mapper)
                        .execute(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void validatesRequiredServiceDependencies() {
        assertThatThrownBy(() -> new CreateSavingsGroupApplicationService(
                        null, groupId -> new GroupCode("BS-X"), publisher, clock, transaction, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CreateSavingsGroupApplicationService(
                        repository, null, publisher, clock, transaction, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new JoinGroupApplicationService(
                        repository, publisher, null, transaction, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new JoinGroupApplicationService(
                        repository, publisher, clock, null, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new JoinGroupApplicationService(
                        null, publisher, clock, transaction, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new JoinGroupApplicationService(
                        repository, null, clock, transaction, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new JoinGroupApplicationService(
                        repository, publisher, clock, transaction, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new GetSavingsGroupApplicationService(null, transaction, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new GetSavingsGroupApplicationService(repository, null, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new GetSavingsGroupApplicationService(repository, transaction, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ListSavingsGroupsApplicationService(null, transaction, mapper))
                .isInstanceOf(NullPointerException.class);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void assertPublishedEvent(Class<? extends DomainEvent> eventType) {
        ArgumentCaptor<List<DomainEvent>> captor = ArgumentCaptor.forClass((Class) List.class);
        verify(publisher).publish(captor.capture());
        assertThat(captor.getValue()).singleElement().isInstanceOf(eventType);
        org.mockito.Mockito.clearInvocations(publisher);
    }
}
