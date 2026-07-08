package in.bachatsetu.backend.auction.interfaces.rest.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.draw.domain.event.DrawCompleted;
import in.bachatsetu.backend.draw.domain.model.BidAmount;
import in.bachatsetu.backend.draw.domain.model.Draw;
import in.bachatsetu.backend.draw.domain.model.DrawNumber;
import in.bachatsetu.backend.draw.domain.model.DrawType;
import in.bachatsetu.backend.draw.domain.port.DrawRepository;
import in.bachatsetu.backend.group.application.port.SavingsGroupRepository;
import in.bachatsetu.backend.group.domain.GroupDomainFixtures;
import in.bachatsetu.backend.group.domain.model.GroupId;
import in.bachatsetu.backend.group.domain.model.SavingsGroup;
import in.bachatsetu.backend.notification.application.command.CreateNotificationCommand;
import in.bachatsetu.backend.notification.application.usecase.CreateNotificationUseCase;
import in.bachatsetu.backend.notification.domain.model.NotificationCategory;
import in.bachatsetu.backend.notification.domain.model.NotificationChannel;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Money;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DrawCompletionNotificationListenerTest {

    private static final Instant NOW = Instant.parse("2026-07-08T08:00:00Z");

    private DrawRepository drawRepository;
    private SavingsGroupRepository groupRepository;
    private CreateNotificationUseCase createNotification;
    private DrawCompletionNotificationListener listener;

    @BeforeEach
    void setUp() {
        drawRepository = mock(DrawRepository.class);
        groupRepository = mock(SavingsGroupRepository.class);
        createNotification = mock(CreateNotificationUseCase.class);
        listener = new DrawCompletionNotificationListener(drawRepository, groupRepository, createNotification);
    }

    @Test
    void notifiesTheWinnerWithAuctionWonWordingForAnAuctionTypeDraw() {
        AggregateId winnerId = AggregateId.newId();
        AggregateId organizerId = AggregateId.newId();
        Draw draw = newCompletedDraw(DrawType.AUCTION, winnerId);
        SavingsGroup group = GroupDomainFixtures.newGroup(organizerId, 5);
        when(drawRepository.findById(draw.id())).thenReturn(Optional.of(draw));
        when(groupRepository.findById(draw.tenantId(), new GroupId(draw.groupId()))).thenReturn(Optional.of(group));

        listener.onDrawCompleted(new DrawCompleted(UUID.randomUUID(), draw.id(), winnerId, NOW));

        ArgumentCaptor<CreateNotificationCommand> captor = ArgumentCaptor.forClass(CreateNotificationCommand.class);
        verify(createNotification, times(2)).execute(captor.capture());
        List<CreateNotificationCommand> commands = captor.getAllValues();

        CreateNotificationCommand winnerCommand = commands.get(0);
        assertThat(winnerCommand.tenantId()).isEqualTo(draw.tenantId());
        assertThat(winnerCommand.recipientUserId()).isEqualTo(winnerId);
        assertThat(winnerCommand.channel()).isEqualTo(NotificationChannel.PUSH);
        assertThat(winnerCommand.category()).isEqualTo(NotificationCategory.AUCTION);
        assertThat(winnerCommand.placeholders()).containsEntry("title", "Auction Won");
        assertThat(winnerCommand.placeholders())
                .containsEntry("body", "You have successfully won this month's auction.");

        CreateNotificationCommand organizerCommand = commands.get(1);
        assertThat(organizerCommand.recipientUserId()).isEqualTo(organizerId);
        assertThat(organizerCommand.category()).isEqualTo(NotificationCategory.DRAW);
        assertThat(organizerCommand.placeholders()).containsEntry("title", "Draw completed");
        assertThat(organizerCommand.placeholders())
                .containsEntry("body", "Monthly draw completed successfully.");
    }

    @Test
    void notifiesTheWinnerWithGenericDrawWordingForANonAuctionDraw() {
        AggregateId winnerId = AggregateId.newId();
        Draw draw = newCompletedDraw(DrawType.RANDOM, winnerId);
        when(drawRepository.findById(draw.id())).thenReturn(Optional.of(draw));
        when(groupRepository.findById(draw.tenantId(), new GroupId(draw.groupId()))).thenReturn(Optional.empty());

        listener.onDrawCompleted(new DrawCompleted(UUID.randomUUID(), draw.id(), winnerId, NOW));

        ArgumentCaptor<CreateNotificationCommand> captor = ArgumentCaptor.forClass(CreateNotificationCommand.class);
        verify(createNotification).execute(captor.capture());
        assertThat(captor.getValue().category()).isEqualTo(NotificationCategory.DRAW);
        assertThat(captor.getValue().placeholders()).containsEntry("title", "Congratulations!");
        assertThat(captor.getValue().placeholders()).containsEntry("body", "You won this month's draw.");
    }

    @Test
    void doesNotNotifyTheOrganizerSeparatelyWhenTheOrganizerIsTheWinner() {
        AggregateId winnerId = AggregateId.newId();
        Draw draw = newCompletedDraw(DrawType.RANDOM, winnerId);
        SavingsGroup group = GroupDomainFixtures.newGroup(winnerId, 5);
        when(drawRepository.findById(draw.id())).thenReturn(Optional.of(draw));
        when(groupRepository.findById(draw.tenantId(), new GroupId(draw.groupId()))).thenReturn(Optional.of(group));

        listener.onDrawCompleted(new DrawCompleted(UUID.randomUUID(), draw.id(), winnerId, NOW));

        verify(createNotification, times(1)).execute(any());
    }

    @Test
    void skipsTheOrganizerNotificationWhenTheGroupCannotBeFound() {
        AggregateId winnerId = AggregateId.newId();
        Draw draw = newCompletedDraw(DrawType.RANDOM, winnerId);
        when(drawRepository.findById(draw.id())).thenReturn(Optional.of(draw));
        when(groupRepository.findById(draw.tenantId(), new GroupId(draw.groupId()))).thenReturn(Optional.empty());

        listener.onDrawCompleted(new DrawCompleted(UUID.randomUUID(), draw.id(), winnerId, NOW));

        verify(createNotification, times(1)).execute(any());
    }

    @Test
    void doesNothingWhenTheDrawNoLongerExists() {
        AggregateId drawId = AggregateId.newId();
        when(drawRepository.findById(drawId)).thenReturn(Optional.empty());

        listener.onDrawCompleted(new DrawCompleted(UUID.randomUUID(), drawId, AggregateId.newId(), NOW));

        verify(createNotification, never()).execute(any());
    }

    @Test
    void aFailureNotifyingTheWinnerDoesNotPreventNotifyingTheOrganizer() {
        AggregateId winnerId = AggregateId.newId();
        AggregateId organizerId = AggregateId.newId();
        Draw draw = newCompletedDraw(DrawType.RANDOM, winnerId);
        SavingsGroup group = GroupDomainFixtures.newGroup(organizerId, 5);
        when(drawRepository.findById(draw.id())).thenReturn(Optional.of(draw));
        when(groupRepository.findById(draw.tenantId(), new GroupId(draw.groupId()))).thenReturn(Optional.of(group));
        when(createNotification.execute(any())).thenThrow(new RuntimeException("dispatch failed"));

        assertThatCode(() -> listener.onDrawCompleted(new DrawCompleted(UUID.randomUUID(), draw.id(), winnerId, NOW)))
                .doesNotThrowAnyException();

        verify(createNotification, times(2)).execute(any());
    }

    @Test
    void rejectsNullConstructorArguments() {
        assertThatThrownBy(() -> new DrawCompletionNotificationListener(null, groupRepository, createNotification))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new DrawCompletionNotificationListener(drawRepository, null, createNotification))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new DrawCompletionNotificationListener(drawRepository, groupRepository, null))
                .isInstanceOf(NullPointerException.class);
    }

    private Draw newCompletedDraw(DrawType type, AggregateId winnerId) {
        Draw draw = Draw.schedule(
                AggregateId.newId(), AggregateId.newId(), AggregateId.newId(), AggregateId.newId(),
                new DrawNumber(1), type, NOW, winnerId, NOW);
        draw.open(winnerId, NOW);
        if (type == DrawType.AUCTION) {
            draw.submitBid(winnerId, new BidAmount(Money.inr(10_000)), winnerId, NOW.plusSeconds(10));
        }
        draw.complete(winnerId, winnerId, NOW.plusSeconds(20));
        return draw;
    }
}
