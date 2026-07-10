package in.bachatsetu.backend.invitation.domain.model;

import in.bachatsetu.backend.invitation.domain.event.InvitationAccepted;
import in.bachatsetu.backend.invitation.domain.event.InvitationCreated;
import in.bachatsetu.backend.invitation.domain.event.InvitationRevoked;
import in.bachatsetu.backend.invitation.domain.exception.InvitationDomainException;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.AuditInfo;
import in.bachatsetu.backend.shared.domain.BaseAggregateRoot;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Invitation-based onboarding aggregate: one active invitation exists per group at a time. */
public final class GroupInvitation extends BaseAggregateRoot {

    private final AggregateId tenantId;
    private final AggregateId groupId;
    private final InvitationCode code;
    private final InvitationToken token;
    private final InvitationType type;
    private InvitationStatus status;
    private final Instant expiresAt;
    private Instant acceptedAt;
    private AggregateId acceptedBy;

    private GroupInvitation(
            AggregateId id,
            AggregateId tenantId,
            AggregateId groupId,
            InvitationCode code,
            InvitationToken token,
            InvitationType type,
            InvitationStatus status,
            Instant expiresAt,
            Instant acceptedAt,
            AggregateId acceptedBy,
            AuditInfo auditInfo,
            long version) {
        super(id, auditInfo, version);
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId must not be null");
        this.groupId = Objects.requireNonNull(groupId, "groupId must not be null");
        this.code = Objects.requireNonNull(code, "code must not be null");
        this.token = Objects.requireNonNull(token, "token must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        this.acceptedAt = acceptedAt;
        this.acceptedBy = acceptedBy;
    }

    public static GroupInvitation create(
            AggregateId id,
            AggregateId tenantId,
            AggregateId groupId,
            InvitationCode code,
            InvitationToken token,
            InvitationType type,
            Instant expiresAt,
            AggregateId actorId,
            Instant createdAt) {
        GroupInvitation invitation = new GroupInvitation(
                id, tenantId, groupId, code, token, type, InvitationStatus.ACTIVE, expiresAt, null, null,
                AuditInfo.createdBy(actorId, createdAt), 0);
        invitation.registerEvent(new InvitationCreated(UUID.randomUUID(), id, groupId, createdAt));
        return invitation;
    }

    public static GroupInvitation rehydrate(
            AggregateId id,
            AggregateId tenantId,
            AggregateId groupId,
            InvitationCode code,
            InvitationToken token,
            InvitationType type,
            InvitationStatus status,
            Instant expiresAt,
            Instant acceptedAt,
            AggregateId acceptedBy,
            AuditInfo auditInfo,
            long version) {
        return new GroupInvitation(
                id, tenantId, groupId, code, token, type, status, expiresAt, acceptedAt, acceptedBy, auditInfo,
                version);
    }

    public void revoke(AggregateId actorId, Instant revokedAt) {
        if (status != InvitationStatus.ACTIVE) {
            throw new InvitationDomainException("only an active invitation can be revoked");
        }
        status = InvitationStatus.CANCELLED;
        markChanged(actorId, revokedAt);
        registerEvent(new InvitationRevoked(UUID.randomUUID(), id(), groupId, revokedAt));
    }

    /** Accepts the invitation for the given joiner, enforcing expiry and single-use invariants. */
    public void accept(AggregateId acceptedByUserId, InvitationType channel, Instant now) {
        Objects.requireNonNull(acceptedByUserId, "acceptedByUserId must not be null");
        Objects.requireNonNull(channel, "channel must not be null");
        Objects.requireNonNull(now, "now must not be null");
        if (status != InvitationStatus.ACTIVE) {
            throw new InvitationDomainException("invitation is not active");
        }
        if (now.isAfter(expiresAt)) {
            throw new InvitationDomainException("invitation has expired");
        }
        status = InvitationStatus.USED;
        acceptedAt = now;
        acceptedBy = acceptedByUserId;
        markChanged(acceptedByUserId, now);
        registerEvent(new InvitationAccepted(UUID.randomUUID(), id(), groupId, acceptedByUserId, channel, now));
    }

    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }

    public AggregateId tenantId() {
        return tenantId;
    }

    public AggregateId groupId() {
        return groupId;
    }

    public InvitationCode code() {
        return code;
    }

    public InvitationToken token() {
        return token;
    }

    public InvitationType type() {
        return type;
    }

    public InvitationStatus status() {
        return status;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public Instant acceptedAt() {
        return acceptedAt;
    }

    public AggregateId acceptedBy() {
        return acceptedBy;
    }
}
