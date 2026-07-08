package in.bachatsetu.backend.auction.interfaces.rest.event;

import in.bachatsetu.backend.draw.domain.event.DrawCompleted;
import in.bachatsetu.backend.draw.domain.model.Draw;
import in.bachatsetu.backend.draw.domain.model.DrawType;
import in.bachatsetu.backend.draw.domain.port.DrawRepository;
import in.bachatsetu.backend.group.application.port.SavingsGroupRepository;
import in.bachatsetu.backend.group.domain.model.GroupId;
import in.bachatsetu.backend.group.domain.model.SavingsGroup;
import in.bachatsetu.backend.notification.application.command.CreateNotificationCommand;
import in.bachatsetu.backend.notification.application.usecase.CreateNotificationUseCase;
import in.bachatsetu.backend.notification.domain.model.NotificationCategory;
import in.bachatsetu.backend.notification.domain.model.NotificationChannel;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Notifies a draw's winner and the owning group's organizer once its closing transaction has committed.
 *
 * <p>Reacts to the pre-existing {@link DrawCompleted} domain event rather than being called directly by
 * {@code CloseAuctionApplicationService} (or {@code CloseDrawApplicationService}): this is the
 * ports-and-events integration between the Auction/Draw and Notification modules, so neither module holds a
 * compile-time dependency on the other's use cases. {@link DrawCompleted} fires for every completed draw,
 * not only auction-type ones, so this one listener naturally covers both the "Draw Completed" and "Auction
 * Completed" integrations: the winner's wording branches on {@link Draw#type()} ({@link DrawType#AUCTION}
 * gets "Auction Won" wording; every other type gets generic draw-winner wording), while the organizer's
 * wording is the same regardless of draw type.
 *
 * <p>Each notification attempt is independently caught and logged rather than rethrown, so a failure
 * notifying the winner never prevents the organizer from being notified, and neither ever appears to fail
 * the already-committed draw closure. Listening with {@link TransactionPhase#AFTER_COMMIT} ensures a
 * notification is only attempted once the draw closure is durably committed — never for a closure that is
 * later rolled back.
 */
@Component
@ConditionalOnProperty(
        prefix = "bachatsetu.persistence.repositories",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class DrawCompletionNotificationListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(DrawCompletionNotificationListener.class);

    private final DrawRepository drawRepository;
    private final SavingsGroupRepository groupRepository;
    private final CreateNotificationUseCase createNotification;

    public DrawCompletionNotificationListener(
            DrawRepository drawRepository,
            SavingsGroupRepository groupRepository,
            CreateNotificationUseCase createNotification) {
        this.drawRepository = Objects.requireNonNull(drawRepository, "draw repository must not be null");
        this.groupRepository = Objects.requireNonNull(groupRepository, "group repository must not be null");
        this.createNotification = Objects.requireNonNull(createNotification, "create notification must not be null");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDrawCompleted(DrawCompleted event) {
        Draw draw = loadDraw(event);
        if (draw == null) {
            return;
        }
        notifyWinner(draw, event.winnerMemberId());
        groupRepository.findById(draw.tenantId(), new GroupId(draw.groupId()))
                .ifPresent(group -> notifyOrganizer(draw, group, event.winnerMemberId()));
    }

    private Draw loadDraw(DrawCompleted event) {
        try {
            return drawRepository.findById(event.aggregateId()).orElse(null);
        } catch (RuntimeException exception) {
            LOGGER.error("Failed to load draw {} for its completion notification", event.aggregateId(), exception);
            return null;
        }
    }

    private void notifyWinner(Draw draw, AggregateId winnerId) {
        try {
            boolean isAuction = draw.type() == DrawType.AUCTION;
            NotificationCategory category = isAuction ? NotificationCategory.AUCTION : NotificationCategory.DRAW;
            String title = isAuction ? "Auction Won" : "Congratulations!";
            String body = isAuction
                    ? "You have successfully won this month's auction."
                    : "You won this month's draw.";
            createNotification.execute(new CreateNotificationCommand(
                    draw.tenantId(),
                    winnerId,
                    winnerId.value().toString(),
                    NotificationChannel.PUSH,
                    category,
                    Map.of("title", title, "body", body),
                    winnerId));
        } catch (RuntimeException exception) {
            LOGGER.error("Failed to send a draw-winner notification for draw {}", draw.id(), exception);
        }
    }

    private void notifyOrganizer(Draw draw, SavingsGroup group, AggregateId winnerId) {
        if (group.organizerId().equals(winnerId)) {
            return;
        }
        try {
            createNotification.execute(new CreateNotificationCommand(
                    draw.tenantId(),
                    group.organizerId(),
                    group.organizerId().value().toString(),
                    NotificationChannel.PUSH,
                    NotificationCategory.DRAW,
                    Map.of("title", "Draw completed", "body", "Monthly draw completed successfully."),
                    group.organizerId()));
        } catch (RuntimeException exception) {
            LOGGER.error("Failed to send a draw-organizer notification for draw {}", draw.id(), exception);
        }
    }
}
