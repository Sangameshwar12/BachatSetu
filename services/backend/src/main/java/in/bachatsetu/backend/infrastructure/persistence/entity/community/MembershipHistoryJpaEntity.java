package in.bachatsetu.backend.infrastructure.persistence.entity.community;

import in.bachatsetu.backend.infrastructure.persistence.entity.BaseJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

/** Immutable persistence record of a membership lifecycle transition. */
@Entity
@Table(
        name = "membership_history",
        schema = "community",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_membership_history_event", columnNames = {"group_member_id", "event_type"}),
        indexes = {
            @Index(name = "idx_membership_history_group_time", columnList = "group_id,occurred_at"),
            @Index(name = "idx_membership_history_member_time", columnList = "member_id,occurred_at")
        })
public class MembershipHistoryJpaEntity extends BaseJpaEntity {

    @NotNull
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false, updatable = false)
    private SavingsGroupJpaEntity group;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_member_id", nullable = false, updatable = false)
    private GroupMemberJpaEntity membership;

    @NotNull
    @Column(name = "member_id", nullable = false, updatable = false)
    private UUID memberId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, updatable = false, length = 20)
    private MembershipHistoryEventType eventType;

    @NotNull
    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected MembershipHistoryJpaEntity() {
    }

    public MembershipHistoryJpaEntity(
            UUID id,
            UUID tenantId,
            SavingsGroupJpaEntity group,
            GroupMemberJpaEntity membership,
            UUID memberId,
            MembershipHistoryEventType eventType,
            Instant occurredAt) {
        super(id);
        this.tenantId = tenantId;
        this.group = group;
        this.membership = membership;
        this.memberId = memberId;
        this.eventType = eventType;
        this.occurredAt = occurredAt;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public SavingsGroupJpaEntity getGroup() {
        return group;
    }

    public GroupMemberJpaEntity getMembership() {
        return membership;
    }

    public UUID getMemberId() {
        return memberId;
    }

    public MembershipHistoryEventType getEventType() {
        return eventType;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    MembershipHistoryJpaEntity rebind(
            SavingsGroupJpaEntity reboundGroup,
            GroupMemberJpaEntity reboundMembership) {
        return new MembershipHistoryJpaEntity(
                getId(),
                tenantId,
                reboundGroup,
                reboundMembership,
                memberId,
                eventType,
                occurredAt);
    }
}
