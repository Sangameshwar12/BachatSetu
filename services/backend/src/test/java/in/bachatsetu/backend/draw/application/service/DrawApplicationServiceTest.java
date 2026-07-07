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

import in.bachatsetu.backend.draw.application.command.CloseDrawCommand;
import in.bachatsetu.backend.draw.application.command.ConductDrawCommand;
import in.bachatsetu.backend.draw.application.command.CreateDrawCommand;
import in.bachatsetu.backend.draw.application.exception.DrawNotFoundException;
import in.bachatsetu.backend.draw.application.mapper.DrawApplicationMapper;
import in.bachatsetu.backend.draw.application.port.ClockPort;
import in.bachatsetu.backend.draw.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.draw.application.port.TransactionPort;
import in.bachatsetu.backend.draw.application.query.DrawResult;
import in.bachatsetu.backend.draw.application.query.DrawSummary;
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
    private DrawFactory drawFactory;
    private DomainEventPublisherPort publisher;
    private ClockPort clock;
    private TransactionPort transaction;
    private DrawApplicationMapper mapper;

    @BeforeEach
    void setUp() {
        repository = mock(DrawRepository.class);
        drawFactory = new DrawFactory(Clock.fixed(NOW, ZoneOffset.UTC));
        publisher = mock(DomainEventPublisherPort.class);
        clock = () -> NOW.plusSeconds(7200);
        transaction = directTransaction();
        mapper = new DrawApplicationMapper();
    }

    @Test
    void createsSavesPublishesAndMapsDraw() {
        CreateDrawCommand command = createCommand();
        CreateDrawUseCase service = new CreateDrawApplicationService(repository, drawFactory, publisher, transaction, mapper);

        DrawResult result = service.execute(command);

        assertThat(result.status()).isEqualTo("SCHEDULED");
        assertThat(result.number()).isEqualTo(command.number().value());
        verify(repository).save(any(Draw.class));
        assertPublishedEvents(DrawScheduled.class);
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
        Draw draw = newScheduledDraw(AggregateId.newId());
        when(repository.findById(tenantId, draw.id())).thenReturn(Optional.of(draw));
        ConductDrawUseCase service = new ConductDrawApplicationService(repository, publisher, clock, transaction, mapper);

        DrawResult result = service.execute(new ConductDrawCommand(tenantId, draw.id(), draw.tenantId()));

        assertThat(result.status()).isEqualTo("OPEN");
        verify(repository).save(draw);
        verify(publisher).publish(any());
    }

    @Test
    void rejectsConductingBeforeTheScheduledTime() {
        AggregateId tenantId = AggregateId.newId();
        Draw draw = newScheduledDraw(AggregateId.newId());
        when(repository.findById(tenantId, draw.id())).thenReturn(Optional.of(draw));
        ClockPort earlyClock = () -> NOW.plusSeconds(10);
        ConductDrawUseCase service =
                new ConductDrawApplicationService(repository, publisher, earlyClock, transaction, mapper);

        assertThatThrownBy(() -> service.execute(new ConductDrawCommand(tenantId, draw.id(), draw.tenantId())))
                .isInstanceOf(InvalidDrawStateException.class);
        verify(repository, never()).save(any());
        verify(publisher, never()).publish(any());
    }

    @Test
    void reportsMissingDrawOnConductWithoutSavingOrPublishing() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId drawId = AggregateId.newId();
        when(repository.findById(tenantId, drawId)).thenReturn(Optional.empty());
        ConductDrawUseCase service = new ConductDrawApplicationService(repository, publisher, clock, transaction, mapper);

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
        CloseDrawUseCase service = new CloseDrawApplicationService(repository, publisher, clock, transaction, mapper);

        DrawResult result = service.execute(new CloseDrawCommand(tenantId, draw.id(), winnerId, actorId));

        assertThat(result.status()).isEqualTo("COMPLETED");
        assertThat(result.winnerMemberId()).isEqualTo(winnerId.value());
        verify(repository).save(draw);
        assertPublishedEvents(DrawCompleted.class);
    }

    @Test
    void rejectsClosingADrawThatIsNotOpen() {
        AggregateId tenantId = AggregateId.newId();
        Draw draw = newScheduledDraw(AggregateId.newId());
        when(repository.findById(tenantId, draw.id())).thenReturn(Optional.of(draw));
        CloseDrawUseCase service = new CloseDrawApplicationService(repository, publisher, clock, transaction, mapper);

        assertThatThrownBy(() -> service.execute(
                        new CloseDrawCommand(tenantId, draw.id(), AggregateId.newId(), draw.tenantId())))
                .isInstanceOf(InvalidDrawStateException.class);
        verify(repository, never()).save(any());
        verify(publisher, never()).publish(any());
    }

    @Test
    void reportsMissingDrawOnCloseWithoutSavingOrPublishing() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId drawId = AggregateId.newId();
        when(repository.findById(tenantId, drawId)).thenReturn(Optional.empty());
        CloseDrawUseCase service = new CloseDrawApplicationService(repository, publisher, clock, transaction, mapper);

        assertThatThrownBy(() -> service.execute(
                        new CloseDrawCommand(tenantId, drawId, AggregateId.newId(), AggregateId.newId())))
                .isInstanceOf(DrawNotFoundException.class);
        verify(repository, never()).save(any());
        verify(publisher, never()).publish(any());
    }

    @Test
    void rejectsNullUseCaseInputs() {
        assertThatThrownBy(() -> new CreateDrawApplicationService(repository, drawFactory, publisher, transaction, mapper)
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
        assertThatThrownBy(() -> new ConductDrawApplicationService(repository, publisher, clock, transaction, mapper)
                .execute(null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CloseDrawApplicationService(repository, publisher, clock, transaction, mapper)
                .execute(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void validatesRequiredServiceDependencies() {
        assertThatThrownBy(() -> new CreateDrawApplicationService(repository, null, publisher, transaction, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CreateDrawApplicationService(repository, drawFactory, publisher, null, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CreateDrawApplicationService(repository, drawFactory, publisher, transaction, null))
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
        assertThatThrownBy(() -> new ConductDrawApplicationService(null, publisher, clock, transaction, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ConductDrawApplicationService(repository, null, clock, transaction, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ConductDrawApplicationService(repository, publisher, null, transaction, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ConductDrawApplicationService(repository, publisher, clock, null, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ConductDrawApplicationService(repository, publisher, clock, transaction, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CloseDrawApplicationService(null, publisher, clock, transaction, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CloseDrawApplicationService(repository, null, clock, transaction, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CloseDrawApplicationService(repository, publisher, null, transaction, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CloseDrawApplicationService(repository, publisher, clock, null, mapper))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CloseDrawApplicationService(repository, publisher, clock, transaction, null))
                .isInstanceOf(NullPointerException.class);
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
