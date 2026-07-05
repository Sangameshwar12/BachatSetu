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
    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt;

    @NotNull
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TokenStatus status;

    protected RefreshTokenJpaEntity() {
    }

    public RefreshTokenJpaEntity(
            UUID id,
            UserJpaEntity user,
            Instant issuedAt,
            Instant expiresAt,
            TokenStatus status) {
        super(id);
        this.user = user;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.status = status;
    }

    public UserJpaEntity getUser() { return user; }
    public Instant getIssuedAt() { return issuedAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public TokenStatus getStatus() { return status; }

    public void updateLifecycle(Instant expiry, TokenStatus tokenStatus) {
        expiresAt = expiry;
        status = tokenStatus;
    }
}
