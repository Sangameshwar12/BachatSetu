package in.bachatsetu.backend.infrastructure.persistence.entity.notification;

import in.bachatsetu.backend.infrastructure.persistence.entity.BaseJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.identity.UserJpaEntity;
import in.bachatsetu.backend.notification.domain.model.NotificationCategory;
import in.bachatsetu.backend.notification.domain.model.NotificationChannel;
import in.bachatsetu.backend.notification.domain.model.NotificationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "notifications",
        schema = "notification",
        indexes = {
            @Index(name = "idx_notifications_tenant_status_schedule", columnList = "tenant_id,status,scheduled_at"),
            @Index(name = "idx_notifications_user_type", columnList = "user_id,notification_type")
        })
public class NotificationJpaEntity extends BaseJpaEntity {

    @NotNull
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserJpaEntity user;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 40)
    private NotificationCategory category;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private NotificationChannel channel;

    @NotBlank
    @Size(max = 254)
    @Column(name = "recipient_reference", nullable = false, length = 254)
    private String recipientReference;

    @Size(max = 160)
    @Column(name = "subject", length = 160)
    private String subject;

    @NotBlank
    @Size(max = 4000)
    @Column(name = "message_body", nullable = false, length = 4000)
    private String body;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private NotificationStatus status;

    @NotNull
    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Min(0)
    @Max(9)
    @Column(name = "priority", nullable = false)
    private int priority;

    protected NotificationJpaEntity() {
    }

    public NotificationJpaEntity(
            UUID id, UUID tenantId, UserJpaEntity user, NotificationCategory category,
            NotificationChannel channel, String recipientReference, String subject,
            String body, NotificationStatus status, Instant scheduledAt,
            Instant sentAt, int priority) {
        super(id);
        this.tenantId = tenantId;
        this.user = user;
        this.category = category;
        this.channel = channel;
        this.recipientReference = recipientReference;
        this.subject = subject;
        this.body = body;
        this.status = status;
        this.scheduledAt = scheduledAt;
        this.sentAt = sentAt;
        this.priority = priority;
    }

    public UUID getTenantId() { return tenantId; }
    public UserJpaEntity getUser() { return user; }
    public NotificationCategory getCategory() { return category; }
    public NotificationChannel getChannel() { return channel; }
    public String getRecipientReference() { return recipientReference; }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
    public NotificationStatus getStatus() { return status; }
    public Instant getScheduledAt() { return scheduledAt; }
    public Instant getSentAt() { return sentAt; }
    public int getPriority() { return priority; }
}
