package in.bachatsetu.backend.infrastructure.persistence.entity.identity;

import in.bachatsetu.backend.auth.domain.model.OtpPurpose;
import in.bachatsetu.backend.auth.domain.model.OtpStatus;
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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "otp_verifications",
        schema = "identity",
        indexes = {
            @Index(name = "idx_otp_user_purpose_status", columnList = "user_id,purpose,status"),
            @Index(name = "idx_otp_expires_at", columnList = "expires_at")
        })
public class OtpVerificationJpaEntity extends BaseJpaEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_otp_verifications_user"))
    private UserJpaEntity user;

    @NotBlank
    @Pattern(regexp = "^\\d{6}$")
    @Column(name = "otp_code", nullable = false, updatable = false, length = 6)
    private String code;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, updatable = false, length = 30)
    private OtpPurpose purpose;

    @NotNull
    @Column(name = "generated_at", nullable = false, updatable = false)
    private Instant generatedAt;

    @NotNull
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OtpStatus status;

    protected OtpVerificationJpaEntity() {
    }

    public OtpVerificationJpaEntity(
            UUID id,
            UserJpaEntity user,
            String code,
            OtpPurpose purpose,
            Instant generatedAt,
            Instant expiresAt,
            OtpStatus status) {
        super(id);
        this.user = user;
        this.code = code;
        this.purpose = purpose;
        this.generatedAt = generatedAt;
        this.expiresAt = expiresAt;
        this.status = status;
    }

    public UserJpaEntity getUser() { return user; }
    public String getCode() { return code; }
    public OtpPurpose getPurpose() { return purpose; }
    public Instant getGeneratedAt() { return generatedAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public OtpStatus getStatus() { return status; }

    public void updateLifecycle(Instant expiry, OtpStatus otpStatus) {
        expiresAt = expiry;
        status = otpStatus;
    }
}
