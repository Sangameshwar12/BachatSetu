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
    private final AggregateId tenantId;
    private final TokenSessionId sessionId;
    private final RefreshTokenHash tokenHash;
    private final Instant issuedAt;
    private final Instant expiresAt;
    private TokenStatus status;
    private RefreshTokenId replacedByTokenId;

    private RefreshToken(
            RefreshTokenId refreshTokenId,
            UserId userId,
            AggregateId tenantId,
            TokenSessionId sessionId,
            RefreshTokenHash tokenHash,
            Instant issuedAt,
            Instant expiresAt,
            TokenStatus status,
            RefreshTokenId replacedByTokenId,
            AuditInfo auditInfo,
            long version) {
        super(refreshTokenId.toAggregateId(), auditInfo, version);
        this.refreshTokenId = Objects.requireNonNull(refreshTokenId, "refresh token id must not be null");
        this.userId = Objects.requireNonNull(userId, "user id must not be null");
        this.tenantId = Objects.requireNonNull(tenantId, "tenant id must not be null");
        this.sessionId = Objects.requireNonNull(sessionId, "token session id must not be null");
        this.tokenHash = Objects.requireNonNull(tokenHash, "refresh token hash must not be null");
        this.issuedAt = Objects.requireNonNull(issuedAt, "issuedAt must not be null");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.replacedByTokenId = replacedByTokenId;
        if (!expiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException("refresh token expiry must follow issue time");
        }
        boolean replacementRequired = status == TokenStatus.ROTATED || status == TokenStatus.REUSED;
        if (replacementRequired != (replacedByTokenId != null)) {
            throw new IllegalArgumentException("rotated refresh token replacement state is invalid");
        }
    }

    public static RefreshToken issue(
            RefreshTokenId refreshTokenId,
            UserId userId,
            AggregateId tenantId,
            TokenSessionId sessionId,
            RefreshTokenHash tokenHash,
            Instant issuedAt,
            Instant expiresAt,
            AggregateId actorId) {
        RefreshToken token = new RefreshToken(
                refreshTokenId,
                userId,
                tenantId,
                sessionId,
                tokenHash,
                issuedAt,
                expiresAt,
                TokenStatus.ACTIVE,
                null,
                AuditInfo.createdBy(actorId, issuedAt),
                0);
        token.registerEvent(new RefreshTokenCreated(
                UUID.randomUUID(), refreshTokenId, userId, expiresAt, issuedAt));
        return token;
    }

    /** Reconstructs persisted refresh-token state without emitting domain events. */
    public static RefreshToken rehydrate(
            RefreshTokenId refreshTokenId,
            UserId userId,
            AggregateId tenantId,
            TokenSessionId sessionId,
            RefreshTokenHash tokenHash,
            Instant issuedAt,
            Instant expiresAt,
            TokenStatus status,
            RefreshTokenId replacedByTokenId,
            AuditInfo auditInfo,
            long version) {
        return new RefreshToken(
                refreshTokenId,
                userId,
                tenantId,
                sessionId,
                tokenHash,
                issuedAt,
                expiresAt,
                status,
                replacedByTokenId,
                auditInfo,
                version);
    }

    /** Marks this credential as replaced and immediately unusable. */
    public void rotate(RefreshTokenId replacementId, AggregateId actorId, Instant rotatedAt) {
        Objects.requireNonNull(replacementId, "replacement token id must not be null");
        if (!isUsableAt(rotatedAt) || refreshTokenId.equals(replacementId)) {
            throw new IdentityDomainException("only an active token can be rotated to a new token");
        }
        status = TokenStatus.ROTATED;
        replacedByTokenId = replacementId;
        markChanged(actorId, rotatedAt);
    }

    /** Records presentation of an already rotated credential. */
    public void markReused(AggregateId actorId, Instant detectedAt) {
        if (status != TokenStatus.ROTATED) {
            throw new IdentityDomainException("only a rotated refresh token can be marked reused");
        }
        status = TokenStatus.REUSED;
        markChanged(actorId, detectedAt);
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

    public AggregateId tenantId() {
        return tenantId;
    }

    public TokenSessionId sessionId() {
        return sessionId;
    }

    public RefreshTokenHash tokenHash() {
        return tokenHash;
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

    public RefreshTokenId replacedByTokenId() {
        return replacedByTokenId;
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
