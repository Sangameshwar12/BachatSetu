package in.bachatsetu.backend.infrastructure.persistence.entity.community;

import in.bachatsetu.backend.infrastructure.persistence.entity.BaseJpaEntity;
import in.bachatsetu.backend.invitation.domain.model.InvitationStatus;
import in.bachatsetu.backend.invitation.domain.model.InvitationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

/** Canonical persistence representation of the GroupInvitation aggregate root. */
@Entity
@Table(
        name = "group_invitations",
        schema = "community",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_group_invitations_tenant_code", columnNames = {"tenant_id", "invitation_code"}),
            @UniqueConstraint(name = "uk_group_invitations_token", columnNames = "secure_token")
        })
public class GroupInvitationJpaEntity extends BaseJpaEntity {

    @NotNull
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @NotNull
    @Column(name = "group_id", nullable = false, updatable = false)
    private UUID groupId;

    @NotBlank
    @Size(max = 12)
    @Column(name = "invitation_code", nullable = false, length = 12, updatable = false)
    private String invitationCode;

    @NotBlank
    @Size(max = 64)
    @Column(name = "secure_token", nullable = false, length = 64, updatable = false)
    private String secureToken;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "invitation_type", nullable = false, length = 10, updatable = false)
    private InvitationType invitationType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InvitationStatus status;

    @NotNull
    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "accepted_by")
    private UUID acceptedBy;

    protected GroupInvitationJpaEntity() {
    }

    public GroupInvitationJpaEntity(
            UUID id,
            UUID tenantId,
            UUID groupId,
            String invitationCode,
            String secureToken,
            InvitationType invitationType,
            InvitationStatus status,
            Instant expiresAt,
            Instant acceptedAt,
            UUID acceptedBy) {
        super(id);
        this.tenantId = tenantId;
        this.groupId = groupId;
        this.invitationCode = invitationCode;
        this.secureToken = secureToken;
        this.invitationType = invitationType;
        this.status = status;
        this.expiresAt = expiresAt;
        this.acceptedAt = acceptedAt;
        this.acceptedBy = acceptedBy;
    }

    public UUID getTenantId() { return tenantId; }
    public UUID getGroupId() { return groupId; }
    public String getInvitationCode() { return invitationCode; }
    public String getSecureToken() { return secureToken; }
    public InvitationType getInvitationType() { return invitationType; }
    public InvitationStatus getStatus() { return status; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getAcceptedAt() { return acceptedAt; }
    public UUID getAcceptedBy() { return acceptedBy; }

    public void update(InvitationStatus newStatus, Instant newAcceptedAt, UUID newAcceptedBy) {
        this.status = newStatus;
        this.acceptedAt = newAcceptedAt;
        this.acceptedBy = newAcceptedBy;
    }
}
