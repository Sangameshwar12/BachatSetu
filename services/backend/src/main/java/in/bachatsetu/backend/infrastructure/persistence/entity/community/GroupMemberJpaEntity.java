package in.bachatsetu.backend.infrastructure.persistence.entity.community;

import in.bachatsetu.backend.infrastructure.persistence.entity.BaseJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.identity.UserJpaEntity;
import in.bachatsetu.backend.member.domain.model.GroupRole;
import in.bachatsetu.backend.member.domain.model.ParticipationStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Canonical persistence representation of membership within a savings group. */
@Entity
@Table(
        name = "group_members",
        schema = "community",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_group_members_group_user", columnNames = {"group_id", "user_id"}),
            @UniqueConstraint(name = "uk_group_members_number", columnNames = {"group_id", "member_number"})
        },
        indexes = {
            @Index(name = "idx_group_members_user_status", columnList = "user_id,status"),
            @Index(name = "idx_group_members_tenant_status", columnList = "tenant_id,status")
        })
public class GroupMemberJpaEntity extends BaseJpaEntity {

    @NotNull
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false, updatable = false)
    private SavingsGroupJpaEntity group;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private UserJpaEntity user;

    @NotBlank
    @Size(max = 32)
    @Column(name = "member_number", nullable = false, length = 32)
    private String memberNumber;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "role_in_group", nullable = false, length = 30)
    private GroupRole role;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ParticipationStatus status;

    @NotNull
    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    @Column(name = "exited_at")
    private Instant removedAt;

    @OneToMany(
            mappedBy = "membership",
            fetch = FetchType.LAZY,
            cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @OrderBy("occurredAt ASC")
    private List<MembershipHistoryJpaEntity> history = new ArrayList<>();

    protected GroupMemberJpaEntity() {
    }

    public GroupMemberJpaEntity(
            UUID id,
            UUID tenantId,
            SavingsGroupJpaEntity group,
            UserJpaEntity user,
            String memberNumber,
            GroupRole role,
            ParticipationStatus status,
            Instant joinedAt,
            Instant removedAt) {
        super(id);
        update(tenantId, group, user, memberNumber, role, status, joinedAt, removedAt);
    }

    public final void update(
            UUID tenantId,
            SavingsGroupJpaEntity group,
            UserJpaEntity user,
            String memberNumber,
            GroupRole role,
            ParticipationStatus status,
            Instant joinedAt,
            Instant removedAt) {
        this.tenantId = tenantId;
        this.group = group;
        this.user = user;
        this.memberNumber = memberNumber;
        this.role = role;
        this.status = status;
        this.joinedAt = joinedAt;
        this.removedAt = removedAt;
    }

    public void replaceHistory(List<MembershipHistoryJpaEntity> desiredHistory) {
        history.clear();
        history.addAll(desiredHistory);
    }

    public void synchronizeFrom(GroupMemberJpaEntity desired) {
        update(
                desired.tenantId,
                desired.group,
                desired.user,
                desired.memberNumber,
                desired.role,
                desired.status,
                desired.joinedAt,
                desired.removedAt);
        Map<MembershipHistoryEventType, MembershipHistoryJpaEntity> existingByType = new HashMap<>();
        for (MembershipHistoryJpaEntity entry : history) {
            existingByType.put(entry.getEventType(), entry);
        }
        List<MembershipHistoryJpaEntity> synchronizedHistory = new ArrayList<>();
        for (MembershipHistoryJpaEntity desiredEntry : desired.history) {
            MembershipHistoryJpaEntity existingEntry = existingByType.get(desiredEntry.getEventType());
            synchronizedHistory.add(existingEntry == null ? desiredEntry.rebind(group, this) : existingEntry);
        }
        history.clear();
        history.addAll(synchronizedHistory);
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public SavingsGroupJpaEntity getGroup() {
        return group;
    }

    public UserJpaEntity getUser() {
        return user;
    }

    public String getMemberNumber() {
        return memberNumber;
    }

    public GroupRole getRole() {
        return role;
    }

    public ParticipationStatus getStatus() {
        return status;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public Instant getExitedAt() {
        return removedAt;
    }

    public List<MembershipHistoryJpaEntity> getHistory() {
        return Collections.unmodifiableList(history);
    }
}
