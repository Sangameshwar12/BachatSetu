package in.bachatsetu.backend.auth.domain.model;

import in.bachatsetu.backend.auth.domain.event.RefreshTokenCreated;
import in.bachatsetu.backend.auth.domain.event.RefreshTokenRevoked;
import in.bachatsetu.backend.auth.domain.exception.IdentityDomainException;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.AuditInfo;
import in.bachatsetu.backend.shared.domain.BaseAggregateRoot;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Refresh-token lifecycle aggregate; it contains no token credential material. */
public final class RefreshToken extends BaseAggregateRoot {

    private final RefreshTokenId refreshTokenId;
    private final UserId userId;
    private final Instant issuedAt;
    private final Instant expiresAt;
    private TokenStatus status;

    private RefreshToken(
            RefreshTokenId refreshTokenId,
            UserId userId,
            Instant issuedAt,
            Instant expiresAt,
            TokenStatus status,
            AuditInfo auditInfo) {
        super(refreshTokenId.toAggregateId(), auditInfo, 0);
        this.refreshTokenId = Objects.requireNonNull(refreshTokenId, "refresh token id must not be null");
        this.userId = Objects.requireNonNull(userId, "user id must not be null");
        this.issuedAt = Objects.requireNonNull(issuedAt, "issuedAt must not be null");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        if (!expiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException("refresh token expiry must follow issue time");
        }
    }

    public static RefreshToken issue(
            RefreshTokenId refreshTokenId,
            UserId userId,
            Instant issuedAt,
            Instant expiresAt,
            AggregateId actorId) {
        RefreshToken token = new RefreshToken(
                refreshTokenId,
                userId,
                issuedAt,
                expiresAt,
                TokenStatus.ACTIVE,
                AuditInfo.createdBy(actorId, issuedAt));
        token.registerEvent(new RefreshTokenCreated(
                UUID.randomUUID(), refreshTokenId, userId, expiresAt, issuedAt));
        return token;
    }

    public void revoke(AggregateId actorId, Instant revokedAt) {
        if (status != TokenStatus.ACTIVE || !revokedAt.isBefore(expiresAt)) {
            throw new IdentityDomainException("only an active, unexpired refresh token can be revoked");
        }
        status = TokenStatus.REVOKED;
        markChanged(actorId, revokedAt);
        registerEvent(new RefreshTokenRevoked(UUID.randomUUID(), refreshTokenId, userId, revokedAt));
    }

    public boolean expire(AggregateId actorId, Instant evaluatedAt) {
        if (status != TokenStatus.ACTIVE || evaluatedAt.isBefore(expiresAt)) {
            return false;
        }
        status = TokenStatus.EXPIRED;
        markChanged(actorId, evaluatedAt);
        return true;
    }

    public boolean isUsableAt(Instant evaluatedAt) {
        Objects.requireNonNull(evaluatedAt, "evaluatedAt must not be null");
        return status == TokenStatus.ACTIVE && evaluatedAt.isBefore(expiresAt);
    }

    public RefreshTokenId refreshTokenId() {
        return refreshTokenId;
    }

    public UserId userId() {
        return userId;
    }

    public Instant issuedAt() {
        return issuedAt;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public TokenStatus status() {
        return status;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof RefreshToken that
                && refreshTokenId.equals(that.refreshTokenId);
    }

    @Override
    public int hashCode() {
        return refreshTokenId.hashCode();
    }
}
