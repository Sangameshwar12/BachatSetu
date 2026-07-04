package in.bachatsetu.backend.infrastructure.persistence.entity.community;

import in.bachatsetu.backend.infrastructure.persistence.entity.BaseJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.identity.UserJpaEntity;
import in.bachatsetu.backend.member.domain.model.GroupRole;
import in.bachatsetu.backend.member.domain.model.ParticipationStatus;
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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "group_members",
        schema = "community",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_group_members_group_user", columnNames = {"group_id", "user_id"}),
            @UniqueConstraint(name = "uk_group_members_number", columnNames = {"group_id", "member_number"})
        },
        indexes = @Index(name = "idx_group_members_user_status", columnList = "user_id,status"))
public class MemberJpaEntity extends BaseJpaEntity {

    @NotNull
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private GroupJpaEntity group;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
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
    private Instant exitedAt;

    protected MemberJpaEntity() {
    }

    public MemberJpaEntity(
            UUID id, UUID tenantId, GroupJpaEntity group, UserJpaEntity user,
            String memberNumber, GroupRole role, ParticipationStatus status,
            Instant joinedAt, Instant exitedAt) {
        super(id);
        this.tenantId = tenantId;
        this.group = group;
        this.user = user;
        this.memberNumber = memberNumber;
        this.role = role;
        this.status = status;
        this.joinedAt = joinedAt;
        this.exitedAt = exitedAt;
    }

    public UUID getTenantId() { return tenantId; }
    public GroupJpaEntity getGroup() { return group; }
    public UserJpaEntity getUser() { return user; }
    public String getMemberNumber() { return memberNumber; }
    public GroupRole getRole() { return role; }
    public ParticipationStatus getStatus() { return status; }
    public Instant getJoinedAt() { return joinedAt; }
    public Instant getExitedAt() { return exitedAt; }
}
