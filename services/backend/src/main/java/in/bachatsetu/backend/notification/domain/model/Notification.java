package in.bachatsetu.backend.notification.domain.model;

import in.bachatsetu.backend.notification.domain.event.NotificationQueued;
import in.bachatsetu.backend.notification.domain.event.NotificationStatusChanged;
import in.bachatsetu.backend.notification.domain.exception.InvalidNotificationStateException;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.AuditInfo;
import in.bachatsetu.backend.shared.domain.BaseAggregateRoot;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class Notification extends BaseAggregateRoot {

    private final AggregateId tenantId;
    private final NotificationRecipient recipient;
    private final NotificationChannel channel;
    private final NotificationCategory category;
    private final NotificationContent content;
    private final Instant scheduledAt;
    private NotificationStatus status;
    private final List<DeliveryAttempt> attempts;

    public Notification(
            AggregateId id,
            AggregateId tenantId,
            NotificationRecipient recipient,
            NotificationChannel channel,
            NotificationCategory category,
            NotificationContent content,
            Instant scheduledAt,
            NotificationStatus status,
            List<DeliveryAttempt> attempts,
            AuditInfo auditInfo,
            long version) {
        super(id, auditInfo, version);
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId must not be null");
        this.recipient = Objects.requireNonNull(recipient, "recipient must not be null");
        this.channel = Objects.requireNonNull(channel, "channel must not be null");
        this.category = Objects.requireNonNull(category, "category must not be null");
        this.content = Objects.requireNonNull(content, "content must not be null");
        this.scheduledAt = Objects.requireNonNull(scheduledAt, "scheduledAt must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.attempts = new ArrayList<>(Objects.requireNonNull(attempts, "attempts must not be null"));
    }

    public static Notification queue(
            AggregateId id,
            AggregateId tenantId,
            NotificationRecipient recipient,
            NotificationChannel channel,
            NotificationCategory category,
            NotificationContent content,
            Instant scheduledAt,
            AggregateId actorId,
            Instant queuedAt) {
        if (scheduledAt.isBefore(queuedAt)) {
            throw new IllegalArgumentException("scheduledAt must not be in the past");
        }
        Notification notification = new Notification(
                id,
                tenantId,
                recipient,
                channel,
                category,
                content,
                scheduledAt,
                NotificationStatus.QUEUED,
                List.of(),
                AuditInfo.createdBy(actorId, queuedAt),
                0);
        notification.registerEvent(new NotificationQueued(
                UUID.randomUUID(), id, recipient.userId(), channel, category, scheduledAt, queuedAt));
        return notification;
    }

    public DeliveryAttempt startDelivery(AggregateId actorId, Instant startedAt) {
        if (status != NotificationStatus.QUEUED && status != NotificationStatus.FAILED) {
            throw new InvalidNotificationStateException("notification cannot start delivery");
        }
        if (startedAt.isBefore(scheduledAt)) {
            throw new InvalidNotificationStateException("notification is not scheduled yet");
        }
        DeliveryAttempt attempt = new DeliveryAttempt(
                AggregateId.newId(), attempts.size() + 1, startedAt, DeliveryAttemptStatus.STARTED, null, null);
        attempts.add(attempt);
        changeStatus(NotificationStatus.SENDING, actorId, startedAt);
        return attempt;
    }

    public void markSent(String providerMessageId, AggregateId actorId, Instant sentAt) {
        requireStatus(NotificationStatus.SENDING);
        attempts.getLast().accept(providerMessageId);
        changeStatus(NotificationStatus.SENT, actorId, sentAt);
    }

    public void markDelivered(AggregateId actorId, Instant deliveredAt) {
        requireStatus(NotificationStatus.SENT);
        attempts.getLast().deliver();
        changeStatus(NotificationStatus.DELIVERED, actorId, deliveredAt);
    }

    public void markFailed(String failureCode, AggregateId actorId, Instant failedAt) {
        requireStatus(NotificationStatus.SENDING, NotificationStatus.SENT);
        attempts.getLast().fail(failureCode);
        changeStatus(NotificationStatus.FAILED, actorId, failedAt);
    }

    private void requireStatus(NotificationStatus... allowedStatuses) {
        for (NotificationStatus allowedStatus : allowedStatuses) {
            if (status == allowedStatus) {
                return;
            }
        }
        throw new InvalidNotificationStateException("notification state does not permit this operation");
    }

    private void changeStatus(NotificationStatus nextStatus, AggregateId actorId, Instant changedAt) {
        NotificationStatus previousStatus = status;
        status = nextStatus;
        markChanged(actorId, changedAt);
        registerEvent(new NotificationStatusChanged(
                UUID.randomUUID(), id(), previousStatus, nextStatus, changedAt));
    }

    public AggregateId tenantId() { return tenantId; }
    public NotificationRecipient recipient() { return recipient; }
    public NotificationChannel channel() { return channel; }
    public NotificationCategory category() { return category; }
    public NotificationContent content() { return content; }
    public Instant scheduledAt() { return scheduledAt; }
    public NotificationStatus status() { return status; }
    public List<DeliveryAttempt> attempts() { return List.copyOf(attempts); }
}
