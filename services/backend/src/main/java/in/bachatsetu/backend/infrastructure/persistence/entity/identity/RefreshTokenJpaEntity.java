package in.bachatsetu.backend.infrastructure.persistence.entity.identity;

import in.bachatsetu.backend.auth.domain.model.TokenStatus;
import in.bachatsetu.backend.infrastructure.persistence.entity.BaseJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "refresh_tokens",
        schema = "identity",
        indexes = {
            @Index(name = "idx_refresh_tokens_user_status", columnList = "user_id,status"),
            @Index(name = "idx_refresh_tokens_user_session", columnList = "user_id,session_id,status"),
            @Index(name = "idx_refresh_tokens_expires_at", columnList = "expires_at")
        })
public class RefreshTokenJpaEntity extends BaseJpaEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_refresh_tokens_user"))
    private UserJpaEntity user;

    @NotNull
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @NotNull
    @Column(name = "session_id", nullable = false, updatable = false)
    private UUID sessionId;

    @NotNull
    @Column(name = "token_hash", nullable = false, updatable = false, length = 255)
    private String tokenHash;

    @NotNull
    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt;

    @NotNull
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TokenStatus status;

    @Column(name = "replaced_by_token_id")
    private UUID replacedByTokenId;

    protected RefreshTokenJpaEntity() {
    }

    public RefreshTokenJpaEntity(
            UUID id,
            UserJpaEntity user,
            UUID tenantId,
            UUID sessionId,
            String tokenHash,
            Instant issuedAt,
            Instant expiresAt,
            TokenStatus status,
            UUID replacedByTokenId) {
        super(id);
        this.user = user;
        this.tenantId = tenantId;
        this.sessionId = sessionId;
        this.tokenHash = tokenHash;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.status = status;
        this.replacedByTokenId = replacedByTokenId;
    }

    public UserJpaEntity getUser() { return user; }
    public UUID getTenantId() { return tenantId; }
    public UUID getSessionId() { return sessionId; }
    public String getTokenHash() { return tokenHash; }
    public Instant getIssuedAt() { return issuedAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public TokenStatus getStatus() { return status; }
    public UUID getReplacedByTokenId() { return replacedByTokenId; }

    public void updateLifecycle(Instant expiry, TokenStatus tokenStatus, UUID replacementId) {
        expiresAt = expiry;
        status = tokenStatus;
        replacedByTokenId = replacementId;
    }
}
