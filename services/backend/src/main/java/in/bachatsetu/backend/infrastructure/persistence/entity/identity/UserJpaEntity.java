package in.bachatsetu.backend.infrastructure.persistence.entity.identity;

import in.bachatsetu.backend.infrastructure.persistence.entity.BaseJpaEntity;
import in.bachatsetu.backend.user.domain.model.PreferredLanguage;
import in.bachatsetu.backend.user.domain.model.UserStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

@Entity
@Table(
        name = "users",
        schema = "identity",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_users_tenant_email", columnNames = {"tenant_id", "email"}),
            @UniqueConstraint(name = "uk_users_tenant_phone", columnNames = {"tenant_id", "phone_number"})
        })
public class UserJpaEntity extends BaseJpaEntity {

    @NotNull
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @NotBlank
    @Size(max = 100)
    @Column(name = "given_name", nullable = false, length = 100)
    private String givenName;

    @Size(max = 100)
    @Column(name = "family_name", length = 100)
    private String familyName;

    @Email
    @Size(max = 254)
    @Column(name = "email", length = 254)
    private String email;

    @Pattern(regexp = "^\\+[1-9]\\d{7,14}$")
    @Size(max = 16)
    @Column(name = "phone_number", length = 16)
    private String phoneNumber;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private UserStatus status;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "language_code", nullable = false, length = 20)
    private PreferredLanguage preferredLanguage;

    protected UserJpaEntity() {
    }

    public UserJpaEntity(
            UUID id,
            UUID tenantId,
            String givenName,
            String familyName,
            String email,
            String phoneNumber,
            UserStatus status,
            PreferredLanguage preferredLanguage) {
        super(id);
        this.tenantId = tenantId;
        this.givenName = givenName;
        this.familyName = familyName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.status = status;
        this.preferredLanguage = preferredLanguage;
    }

    public UUID getTenantId() { return tenantId; }
    public String getGivenName() { return givenName; }
    public String getFamilyName() { return familyName; }
    public String getEmail() { return email; }
    public String getPhoneNumber() { return phoneNumber; }
    public UserStatus getStatus() { return status; }
    public PreferredLanguage getPreferredLanguage() { return preferredLanguage; }
}
