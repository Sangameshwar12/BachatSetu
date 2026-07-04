package in.bachatsetu.backend.infrastructure.persistence.entity.identity;

import in.bachatsetu.backend.infrastructure.persistence.entity.BaseJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.PersistenceRecordStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

@Entity
@Table(
        name = "roles",
        schema = "identity",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_roles_tenant_code", columnNames = {"tenant_id", "role_code"}))
public class RoleJpaEntity extends BaseJpaEntity {

    @Column(name = "tenant_id", updatable = false)
    private UUID tenantId;

    @NotBlank
    @Size(max = 60)
    @Column(name = "role_code", nullable = false, length = 60)
    private String code;

    @NotBlank
    @Size(max = 120)
    @Column(name = "role_name", nullable = false, length = 120)
    private String name;

    @NotBlank
    @Size(max = 30)
    @Column(name = "role_scope", nullable = false, length = 30)
    private String scope;

    @Size(max = 500)
    @Column(name = "description", length = 500)
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PersistenceRecordStatus status;

    protected RoleJpaEntity() {
    }

    public RoleJpaEntity(
            UUID id,
            UUID tenantId,
            String code,
            String name,
            String scope,
            String description,
            PersistenceRecordStatus status) {
        super(id);
        this.tenantId = tenantId;
        this.code = code;
        this.name = name;
        this.scope = scope;
        this.description = description;
        this.status = status;
    }

    public UUID getTenantId() { return tenantId; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getScope() { return scope; }
    public String getDescription() { return description; }
    public PersistenceRecordStatus getStatus() { return status; }
}
