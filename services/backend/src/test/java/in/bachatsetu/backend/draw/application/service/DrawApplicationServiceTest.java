package in.bachatsetu.backend.draw.application.service;

import static in.bachatsetu.backend.draw.application.ApplicationTestFixture.NOW;
import static in.bachatsetu.backend.draw.application.ApplicationTestFixture.createCommand;
import static in.bachatsetu.backend.draw.application.ApplicationTestFixture.directTransaction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.audit.application.usecase.CreateAuditEntryUseCase;
import in.bachatsetu.backend.draw.application.command.CloseDrawCommand;
import in.bachatsetu.backend.draw.application.command.ConductDrawCommand;
import in.bachatsetu.backend.draw.application.command.CreateDrawCommand;
import in.bachatsetu.backend.draw.application.exception.DrawAccessDeniedException;
import in.bachatsetu.backend.draw.application.exception.DrawNotFoundException;
import in.bachatsetu.backend.draw.application.mapper.DrawApplicationMapper;
import in.bachatsetu.backend.draw.application.port.ClockPort;
import in.bachatsetu.backend.draw.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.draw.application.port.TransactionPort;
import in.bachatsetu.backend.draw.application.query.DrawResult;
import in.bachatsetu.backend.draw.application.query.DrawSummary;
import in.bachatsetu.backend.draw.application.security.DrawAuthorizationService;
import in.bachatsetu.backend.draw.application.usecase.CloseDrawUseCase;
import in.bachatsetu.backend.draw.application.usecase.ConductDrawUseCase;
import in.bachatsetu.backend.draw.application.usecase.CreateDrawUseCase;
import in.bachatsetu.backend.draw.application.usecase.GetDrawUseCase;
import in.bachatsetu.backend.draw.application.usecase.ListDrawsUseCase;
import in.bachatsetu.backend.draw.domain.event.DrawCompleted;
import in.bachatsetu.backend.draw.domain.event.DrawScheduled;
import in.bachatsetu.backend.draw.domain.exception.InvalidDrawStateException;
import in.bachatsetu.backend.draw.domain.factory.DrawFactory;
import in.bachatsetu.backend.draw.domain.model.Draw;
import in.bachatsetu.backend.draw.domain.model.DrawNumber;
import in.bachatsetu.backend.draw.domain.model.DrawType;
import in.bachatsetu.backend.draw.domain.port.DrawPage;
import in.bachatsetu.backend.draw.domain.port.DrawPageRequest;
import in.bachatsetu.backend.draw.domain.port.DrawRepository;
import in.bachatsetu.backend.draw.domain.port.DrawSortField;
import in.bachatsetu.backend.draw.domain.port.SortDirection;
import in.bachatsetu.backend.group.application.port.SavingsGroupRepository;
import in.bachatsetu.backend.group.domain.GroupDomainFixtures;
import in.bachatsetu.backend.group.domain.model.CreatedAt;
import in.bachatsetu.backend.group.domain.model.GroupCode;
import in.bachatsetu.backend.group.domain.model.GroupDescription;
import in.bachatsetu.backend.group.domain.model.GroupId;
import in.bachatsetu.backend.group.domain.model.GroupName;
import in.bachatsetu.backend.group.domain.model.GroupType;
import in.bachatsetu.backend.group.domain.model.OwnerId;
import in.bachatsetu.backend.group.domain.model.SavingsGroup;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.DomainEvent;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DrawApplicationServiceTest {

    private DrawRepository repository;
    private SavingsGroupRepository groupRepository;
    private DrawFactory drawFactory;
    private DomainEventPublisherPort publisher;
    private ClockPort clock;
    private TransactionPort transaction;
    private DrawApplicationMapper mapper;
    private DrawAuthorizationService authorization;
    private CreateAuditEntryUseCase createAuditEntry;

    @BeforeEach
    void setUp() {
        repository = mock(DrawRepository.class);
        groupRepository = mock(SavingsGroupRepository.class);
        drawFactory = new DrawFactory(Clock.fixed(NOW, ZoneOffset.UTC));
        publisher = mock(DomainEventPublisherPort.class);
        clock = () -> NOW.plusSeconds(7200);
        transaction = directTransaction();
        mapper = new DrawApplicationMapper();
        authorization = new DrawAuthorizationService();
        createAuditEntry = mock(CreateAuditEntryUseCase.class);
    }

    @Test
    void createsSavesPublishesAndMapsDraw() {
        CreateDrawCommand command = createCommand();
        stubOwningGroup(command.tenantId(), command.groupId(), command.actorId());
        CreateDrawUseCase service = new CreateDrawApplicationService(
                repository, groupRepository, drawFactory, publisher, transaction, mapper, authorization);

        DrawResult result = service.execute(command);

        assertThat(result.status()).isEqualTo("SCHEDULED");
        assertThat(result.number()).isEqualTo(command.number().value());
        verify(repository).save(any(Draw.class));
        assertPublishedEvents(DrawScheduled.class);
    }

    @Test
    void createRejectsAnActorWhoIsNotTheGroupOwner() {
        CreateDrawCommand command = createCommand();
        stubOwningGroup(command.tenantId(), command.groupId(), AggregateId.newId());
        CreateDrawUseCase service = new CreateDrawApplicationService(
                repository, groupRepository, drawFactory, publisher, transaction, mapper, authorization);

        assertThatThrownBy(() -> service.execute(command)).isInstanceOf(DrawAccessDeniedException.class);
        verify(repository, never()).save(any());
        verify(publisher, never()).publish(any());
    }

    @Test
    void createRejectsWhenTheTargetGroupDoesNotExist() {
        CreateDrawCommand command = createCommand();
        when(groupRepository.findById(command.tenantId(), new GroupId(command.groupId())))
                .thenReturn(Optional.empty());
        CreateDrawUseCase service = new CreateDrawApplicationService(
                repository, groupRepository, drawFactory, publisher, transaction, mapper, authorization);

        assertThatThrownBy(() -> service.execute(command)).isInstanceOf(DrawAccessDeniedException.class);
        verify(repository, never()).save(any());
        verify(publisher, never()).publish(any());
    }

    @Test
    void retrievesTenantScopedDraw() {
        AggregateId tenantId = AggregateId.newId();
        Draw draw = newScheduledDraw(AggregateId.newId());
        when(repository.findById(tenantId, draw.id())).thenReturn(Optional.of(draw));
        GetDrawUseCase service = new GetDrawApplicationService(repository, transaction, mapper);

        DrawResult result = service.execute(tenantId, draw.id());

        assertThat(result.drawId()).isEqualTo(draw.id().value());
    }

    @Test
    void tenantScopedLookupHidesDrawsFromOtherTenants() {
        AggregateId tenantId = AggregateId.newId();
        Draw draw = newScheduledDraw(AggregateId.newId());
        when(repository.findById(tenantId, draw.id())).thenReturn(Optional.empty());
        GetDrawUseCase service = new GetDrawApplicationService(repository, transaction, mapper);

        assertThatThrownBy(() -> service.execute(tenantId, draw.id()))
                .isInstanceOf(DrawNotFoundException.class);
    }

    @Test
    void listsTenantScopedDrawSummaries() {
        AggregateId tenantId = AggregateId.newId();
        Draw first = newScheduledDraw(AggregateId.newId());
        Draw second = newScheduledDraw(AggregateId.newId());
        DrawPageRequest pageRequest = new DrawPageRequest(0, 20, DrawSortField.CREATED_AT, SortDirection.ASC);
        when(repository.findPage(tenantId, pageRequest))
                .thenReturn(new DrawPage<>(List.of(first, second), 0, 20, 2));
        ListDrawsUseCase service = new ListDrawsApplicationService(repository, transaction, mapper);

        DrawPage<DrawSummary> page = service.execute(tenantId, pageRequest);

        assertThat(page.content()).hasSize(2);
        assertThat(page.totalElements()).isEqualTo(2);
        assertThatThrownBy(() -> page.content().add(mapper.toSummary(first)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void conductsAScheduledDraw() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId ownerId = AggregateId.newId();
        Draw draw = newScheduledDraw(AggregateId.newId());
        when(repository.findById(tenantId, draw.id())).thenReturn(Optional.of(draw));
        stubOwningGroup(draw.tenantId(), draw.groupId(), ownerId);
        ConductDrawUseCase service = new ConductDrawApplicationService(
                repository, groupRepository, publisher, clock, transaction, mapper, authorization);

        DrawResult result = service.execute(new ConductDrawCommand(tenantId, draw.id(), ownerId));

        assertThat(result.status()).isEqualTo("OPEN");
        verify(repository).save(draw);
        verify(publisher).publish(any());
    }

    @Test
    void conductRejectsAnActorWhoIsNotTheGroupOwner() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId ownerId = AggregateId.newId();
        Draw draw = newScheduledDraw(AggregateId.newId());
        when(repository.findById(tenantId, draw.id())).thenReturn(Optional.of(draw));
        stubOwningGroup(draw.tenantId(), draw.groupId(), ownerId);
        ConductDrawUseCase service = new ConductDrawApplicationService(
                repository, groupRepository, publisher, clock, transaction, mapper, authorization);

        assertThatThrownBy(() -> service.execute(new ConductDrawCommand(tenantId, draw.id(), AggregateId.newId())))
                .isInstanceOf(DrawAccessDeniedException.class);
        verify(repository, never()).save(any());
        verify(publisher, never()).publish(any());
    }

    @Test
    void rejectsConductingBeforeTheScheduledTime() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId ownerId = AggregateId.newId();
        Draw draw = newScheduledDraw(AggregateId.newId());
        when(repository.findById(tenantId, draw.id())).thenReturn(Optional.of(draw));
        stubOwningGroup(draw.tenantId(), draw.groupId(), ownerId);
        ClockPort earlyClock = () -> NOW.plusSeconds(10);
        ConductDrawUseCase service = new ConductDrawApplicationService(
                repository, groupRepository, publisher, earlyClock, transaction, mapper, authorization);

        assertThatThrownBy(() -> service.execute(new ConductDrawCommand(tenantId, draw.id(), ownerId)))
                .isInstanceOf(InvalidDrawStateException.class);
        verify(repository, never()).save(any());
        verify(publisher, never()).publish(any());
    }

    @Test
    void reportsMissingDrawOnConductWithoutSavingOrPublishing() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId drawId = AggregateId.newId();
        when(repository.findById(tenantId, drawId)).thenReturn(Optional.empty());
        ConductDrawUseCase service = new ConductDrawApplicationService(
                repository, groupRepository, publisher, clock, transaction, mapper, authorization);

        assertThatThrownBy(() -> service.execute(new ConductDrawCommand(tenantId, drawId, AggregateId.newId())))
                .isInstanceOf(DrawNotFoundException.class);
        verify(repository, never()).save(any());
        verify(publisher, never()).publish(any());
    }

    @Test
    void closesAnOpenDrawWithItsWinner() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId actorId = AggregateId.newId();
        AggregateId winnerId = AggregateId.newId();
        Draw draw = newScheduledDraw(actorId, DrawType.RANDOM);
        draw.open(actorId, NOW.plusSeconds(3600));
        draw.pullDomainEvents();
        when(repository.findById(tenantId, draw.id())).thenReturn(Optional.of(draw));
        stubOwningGroup(draw.tenantId(), draw.groupId(), actorId);
        CloseDrawUseCase service = new CloseDrawApplicationService(
                repository, groupRepository, publisher, clock, transaction, mapper, authorization, createAuditEntry);

        DrawResult result = service.execute(new CloseDrawCommand(tenantId, draw.id(), winnerId, actorId));

        assertThat(result.status()).isEqualTo("COMPLETED");
        assertThat(result.winnerMemberId()).isEqualTo(winnerId.value());
        verify(repository).save(draw);
        assertPublishedEvents(DrawCompleted.class);
    }

    @Test
    void closeRejectsAnActorWhoIsNotTheGroupOwner() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId ownerId = AggregateId.newId();
        Draw draw = newScheduledDraw(AggregateId.newId(), DrawType.RANDOM);
        draw.open(ownerId, NOW.plusSeconds(3600));
        draw.pullDomainEvents();
        when(repository.findById(tenantId, draw.id())).thenReturn(Optional.of(draw));
        stubOwningGroup(draw.tenantId(), draw.groupId(), ownerId);
        CloseDrawUseCase service = new CloseDrawApplicationService(
                repository, groupRepository, publisher, clock, transaction, mapper, authorization, createAuditEntry);

        assertThatThrownBy(() -> service.execute(
                        new CloseDrawCommand(tenantId, draw.id(), AggregateId.newId(), AggregateId.newId())))
                .isInstanceOf(DrawAccessDeniedException.class);
        verify(repository, never()).save(any());
        verify(publisher, never()).publish(any());
    }

    @Test
    void rejectsClosingADrawThatIsNotOpen() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId ownerId = AggregateId.newId();
        Draw draw = newScheduledDraw(AggregateId.newId());
        when(repository.findById(tenantId, draw.id())).thenReturn(Optional.of(draw));
        stubOwningGroup(draw.tenantId(), draw.groupId(), ownerId);
        CloseDrawUseCase service = new CloseDrawApplicationService(
                repository, groupRepository, publisher, clock, transaction, mapper, authorization, createAuditEntry);

        assertThatThrownBy(() -> service.execute(
                        new CloseDrawCommand(tenantId, draw.id(), AggregateId.newId(), ownerId)))
                .isInstanceOf(InvalidDrawStateException.class);
        verify(repository, never()).save(any());
        verify(publisher, never()).publish(any());
    }

    @Test
    void reportsMissingDrawOnCloseWithoutSavingOrPublishing() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId drawId = AggregateId.newId();
        when(repository.findById(tenantId, drawId)).thenReturn(Optional.empty());
        CloseDrawUseCase service = new CloseDrawApplicationService(
                repository, groupRepository, publisher, clock, transaction, mapper, authorization, createAuditEntry);

        assertThatThrownBy(() -> service.execute(
                        new CloseDrawCommand(tenantId, drawId, AggregateId.newId(), AggregateId.newId())))
                .isInstanceOf(DrawNotFoundException.class);
        verify(repository, never()).save(any());
        verify(publisher, never()).publish(any());
    }

    @Test
    void rejectsNullUseCaseInputs() {
        assertThatThrownBy(() -> new CreateDrawApplicationService(
                        repository, groupRepository, drawFactory, publisher, transaction, mapper, authorization)
                        .execute(null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new GetDrawApplicationService(repository, transaction, mapper)
                        .execute(null, AggregateId.newId()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new GetDrawApplicationService(repository, transaction, mapper)
                        .execute(AggregateId.newId(), null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ListDrawsApplicationService(repository, transaction, mapper)
                        .execute(null, new DrawPageRequest(0, 20, DrawSortField.CREATED_AT, SortDirection.ASC)))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ListDrawsApplicationService(repository, transaction, mapper)
                        .execute(AggregateId.newId(), null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ConductDrawApplicationService(
                        repository, groupRepository, publisher, clock, transaction, mapper, authorization)
                        .execute(null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CloseDrawApplicationService(
                        repository, groupRepository, publisher, clock, transaction, mapper, authorization,
                        createAuditEntry)
                        .execute(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void validatesRequiredServiceDependencies() {
        assertThatThrownBy(() -> new CreateDrawApplicationService(
                        repository, groupRepository, null, publisher, transaction, mapper, authorization))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CreateDrawApplicationService(
                        repository, groupRepository, drawFactory, publisher, null, mapper, authorization))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CreateDrawApplicationService(
                        repository, groupRepository, drawFactory, publisher, transaction, null, authorization))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CreateDrawApplicationService(
                        repository, groupRepository, drawFactory, publisher, transaction, mapper, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new GetDrawApplicationService(null, transaction, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new GetDrawApplicationService(repository, null, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new GetDrawApplicationService(repository, transaction, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ListDrawsApplicationService(null, transaction, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ListDrawsApplicationService(repository, null, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ListDrawsApplicationService(repository, transaction, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ConductDrawApplicationService(
                        null, groupRepository, publisher, clock, transaction, mapper, authorization))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ConductDrawApplicationService(
                        repository, groupRepository, null, clock, transaction, mapper, authorization))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ConductDrawApplicationService(
                        repository, groupRepository, publisher, null, transaction, mapper, authorization))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ConductDrawApplicationService(
                        repository, groupRepository, publisher, clock, null, mapper, authorization))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ConductDrawApplicationService(
                        repository, groupRepository, publisher, clock, transaction, null, authorization))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ConductDrawApplicationService(
                        repository, groupRepository, publisher, clock, transaction, mapper, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CloseDrawApplicationService(
                        null, groupRepository, publisher, clock, transaction, mapper, authorization,
                        createAuditEntry))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CloseDrawApplicationService(
                        repository, groupRepository, null, clock, transaction, mapper, authorization,
                        createAuditEntry))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CloseDrawApplicationService(
                        repository, groupRepository, publisher, null, transaction, mapper, authorization,
                        createAuditEntry))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CloseDrawApplicationService(
                        repository, groupRepository, publisher, clock, null, mapper, authorization,
                        createAuditEntry))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CloseDrawApplicationService(
                        repository, groupRepository, publisher, clock, transaction, null, authorization,
                        createAuditEntry))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CloseDrawApplicationService(
                        repository, groupRepository, publisher, clock, transaction, mapper, null,
                        createAuditEntry))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CloseDrawApplicationService(
                        repository, groupRepository, publisher, clock, transaction, mapper, authorization, null))
                .isInstanceOf(NullPointerException.class);
    }

    private void stubOwningGroup(AggregateId tenantId, AggregateId groupId, AggregateId ownerId) {
        SavingsGroup group = SavingsGroup.create(
                new GroupId(groupId),
                tenantId,
                new OwnerId(ownerId),
                new GroupCode("BS-TEST"),
                new GroupName("Bachat Circle"),
                new GroupDescription("Monthly community savings"),
                GroupType.BHISHI,
                GroupDomainFixtures.monthlyRule(5),
                new CreatedAt(GroupDomainFixtures.NOW));
        when(groupRepository.findById(tenantId, new GroupId(groupId))).thenReturn(Optional.of(group));
    }

    private Draw newScheduledDraw(AggregateId actorId) {
        return newScheduledDraw(actorId, DrawType.AUCTION);
    }

    private Draw newScheduledDraw(AggregateId actorId, DrawType type) {
        Draw draw = Draw.schedule(
                AggregateId.newId(),
                AggregateId.newId(),
                AggregateId.newId(),
                AggregateId.newId(),
                new DrawNumber(1),
                type,
                NOW.plusSeconds(3600),
                actorId,
                NOW);
        draw.pullDomainEvents();
        return draw;
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
