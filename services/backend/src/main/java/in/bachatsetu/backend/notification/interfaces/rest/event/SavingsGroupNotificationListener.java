package in.bachatsetu.backend.notification.interfaces.rest.event;

import in.bachatsetu.backend.group.domain.event.MemberJoined;
import in.bachatsetu.backend.group.domain.event.MemberRemoved;
import in.bachatsetu.backend.group.domain.event.SavingsGroupCreated;
import in.bachatsetu.backend.group.domain.port.GroupRepository;
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
 * Notifies the relevant member for the three Savings Group membership events this sprint integrates: a
 * group being created, a member joining, and a member being removed.
 *
 * <p>Reacts to the pre-existing {@link SavingsGroupCreated}, {@link MemberJoined}, and {@link MemberRemoved}
 * domain events rather than being called directly by any Group application service, so the Group module
 * holds no compile-time dependency on Notification.
 *
 * <p>Each handler listens with {@link TransactionPhase#AFTER_COMMIT} and independently catches and logs any
 * failure, so one event type failing to notify never affects another, and never appears to fail the
 * already-committed group operation itself.
 */
@Component
@ConditionalOnProperty(
        prefix = "bachatsetu.persistence.repositories",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class SavingsGroupNotificationListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(SavingsGroupNotificationListener.class);

    private final GroupRepository groupRepository;
    private final CreateNotificationUseCase createNotification;

    public SavingsGroupNotificationListener(
            GroupRepository groupRepository, CreateNotificationUseCase createNotification) {
        this.groupRepository = Objects.requireNonNull(groupRepository, "group repository must not be null");
        this.createNotification = Objects.requireNonNull(createNotification, "create notification must not be null");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onGroupCreated(SavingsGroupCreated event) {
        try {
            notify(event.tenantId(), event.ownerId().value(), "Group Created",
                    "Your savings group " + event.groupCode().value() + " has been created successfully.");
        } catch (RuntimeException exception) {
            LOGGER.error("Failed to send a group-created notification for group {}", event.aggregateId(), exception);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMemberJoined(MemberJoined event) {
        try {
            groupRepository.findById(event.aggregateId()).ifPresent(group -> notify(
                    group.tenantId(), event.memberId(), "Member Joined",
                    "You have joined " + group.name().value() + "."));
        } catch (RuntimeException exception) {
            LOGGER.error("Failed to send a member-joined notification for group {}", event.aggregateId(), exception);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMemberRemoved(MemberRemoved event) {
        try {
            groupRepository.findById(event.aggregateId()).ifPresent(group -> notify(
                    group.tenantId(), event.memberId(), "Member Removed",
                    "You have been removed from " + group.name().value() + "."));
        } catch (RuntimeException exception) {
            LOGGER.error("Failed to send a member-removed notification for group {}", event.aggregateId(), exception);
        }
    }

    private void notify(AggregateId tenantId, AggregateId recipientUserId, String title, String body) {
        createNotification.execute(new CreateNotificationCommand(
                tenantId,
                recipientUserId,
                recipientUserId.value().toString(),
                NotificationChannel.PUSH,
                NotificationCategory.GROUP,
                Map.of("title", title, "body", body),
                recipientUserId));
    }
}
