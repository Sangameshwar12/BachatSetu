package in.bachatsetu.backend.automation.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.automation.application.port.ClockPort;
import in.bachatsetu.backend.automation.application.query.JobRunResult;
import in.bachatsetu.backend.draw.application.command.ConductDrawCommand;
import in.bachatsetu.backend.draw.application.query.DrawResult;
import in.bachatsetu.backend.draw.application.usecase.ConductDrawUseCase;
import in.bachatsetu.backend.draw.domain.model.Draw;
import in.bachatsetu.backend.draw.domain.model.DrawNumber;
import in.bachatsetu.backend.draw.domain.model.DrawType;
import in.bachatsetu.backend.draw.domain.port.DrawRepository;
import in.bachatsetu.backend.group.domain.GroupDomainFixtures;
import in.bachatsetu.backend.group.domain.model.SavingsGroup;
import in.bachatsetu.backend.group.domain.port.GroupRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DrawSchedulerApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-08T08:00:00Z");

    private DrawRepository drawRepository;
    private GroupRepository groupRepository;
    private ConductDrawUseCase conductDraw;
    private ClockPort clock;
    private DrawSchedulerApplicationService service;

    @BeforeEach
    void setUp() {
        drawRepository = mock(DrawRepository.class);
        groupRepository = mock(GroupRepository.class);
        conductDraw = mock(ConductDrawUseCase.class);
        clock = mock(ClockPort.class);
        when(clock.now()).thenReturn(NOW);
        service = new DrawSchedulerApplicationService(drawRepository, groupRepository, conductDraw, clock);
    }

    @Test
    void conductsEveryDueScheduledDrawOnBehalfOfItsOrganizer() {
        AggregateId organizerId = AggregateId.newId();
        Draw firstDraw = newScheduledDraw(1);
        Draw secondDraw = newScheduledDraw(2);
        SavingsGroup group = GroupDomainFixtures.newGroup(organizerId, 5);
        when(drawRepository.findDueScheduled(NOW)).thenReturn(List.of(firstDraw, secondDraw));
        when(groupRepository.findById(firstDraw.groupId())).thenReturn(Optional.of(group));
        when(groupRepository.findById(secondDraw.groupId())).thenReturn(Optional.of(group));
        when(conductDraw.execute(any())).thenReturn(newDrawResult());

        JobRunResult result = service.execute();

        assertThat(result.processedCount()).isEqualTo(2);
        assertThat(result.failedCount()).isZero();
        ArgumentCaptor<ConductDrawCommand> captor = ArgumentCaptor.forClass(ConductDrawCommand.class);
        verify(conductDraw, times(2)).execute(captor.capture());
        assertThat(captor.getAllValues()).allSatisfy(command -> assertThat(command.actorId()).isEqualTo(organizerId));
    }

    @Test
    void returnsAnEmptyResultWhenNoDrawsAreDue() {
        when(drawRepository.findDueScheduled(NOW)).thenReturn(List.of());

        JobRunResult result = service.execute();

        assertThat(result).isEqualTo(JobRunResult.empty());
        verify(conductDraw, never()).execute(any());
    }

    @Test
    void continuesProcessingRemainingDrawsWhenOneFails() {
        Draw failingDraw = newScheduledDraw(1);
        Draw succeedingDraw = newScheduledDraw(2);
        SavingsGroup group = GroupDomainFixtures.newGroup(AggregateId.newId(), 5);
        when(drawRepository.findDueScheduled(NOW)).thenReturn(List.of(failingDraw, succeedingDraw));
        when(groupRepository.findById(failingDraw.groupId())).thenReturn(Optional.empty());
        when(groupRepository.findById(succeedingDraw.groupId())).thenReturn(Optional.of(group));
        when(conductDraw.execute(any())).thenReturn(newDrawResult());

        JobRunResult result = service.execute();

        assertThat(result.processedCount()).isEqualTo(1);
        assertThat(result.failedCount()).isEqualTo(1);
        verify(conductDraw, times(1)).execute(any());
    }

    @Test
    void continuesTheRunWhenConductDrawItselfThrows() {
        Draw draw = newScheduledDraw(1);
        SavingsGroup group = GroupDomainFixtures.newGroup(AggregateId.newId(), 5);
        when(drawRepository.findDueScheduled(NOW)).thenReturn(List.of(draw));
        when(groupRepository.findById(draw.groupId())).thenReturn(Optional.of(group));
        when(conductDraw.execute(any())).thenThrow(new RuntimeException("conduct failed"));

        JobRunResult result = service.execute();

        assertThat(result.processedCount()).isZero();
        assertThat(result.failedCount()).isEqualTo(1);
    }

    @Test
    void rejectsNullConstructorArguments() {
        assertThatThrownBy(() -> new DrawSchedulerApplicationService(null, groupRepository, conductDraw, clock))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new DrawSchedulerApplicationService(drawRepository, null, conductDraw, clock))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new DrawSchedulerApplicationService(drawRepository, groupRepository, null, clock))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new DrawSchedulerApplicationService(drawRepository, groupRepository, conductDraw, null))
                .isInstanceOf(NullPointerException.class);
    }

    private Draw newScheduledDraw(int number) {
        return Draw.schedule(
                AggregateId.newId(), AggregateId.newId(), AggregateId.newId(), AggregateId.newId(),
                new DrawNumber(number), DrawType.RANDOM, NOW, AggregateId.newId(), NOW.minusSeconds(60));
    }

    private DrawResult newDrawResult() {
        return new DrawResult(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), 1, "RANDOM", "OPEN", NOW, null, List.of(), NOW, NOW, 1);
    }
}
