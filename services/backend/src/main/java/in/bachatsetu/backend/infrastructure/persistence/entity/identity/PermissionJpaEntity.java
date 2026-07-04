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
        name = "permissions",
        schema = "identity",
        uniqueConstraints = @UniqueConstraint(name = "uk_permissions_code", columnNames = "permission_code"))
public class PermissionJpaEntity extends BaseJpaEntity {

    @NotBlank
    @Size(max = 100)
    @Column(name = "permission_code", nullable = false, length = 100)
    private String code;

    @NotBlank
    @Size(max = 60)
    @Column(name = "module_code", nullable = false, length = 60)
    private String moduleCode;

    @NotBlank
    @Size(max = 60)
    @Column(name = "action_code", nullable = false, length = 60)
    private String actionCode;

    @Size(max = 500)
    @Column(name = "description", length = 500)
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PersistenceRecordStatus status;

    protected PermissionJpaEntity() {
    }

    public PermissionJpaEntity(
            UUID id,
            String code,
            String moduleCode,
            String actionCode,
            String description,
            PersistenceRecordStatus status) {
        super(id);
        this.code = code;
        this.moduleCode = moduleCode;
        this.actionCode = actionCode;
        this.description = description;
        this.status = status;
    }

    public String getCode() { return code; }
    public String getModuleCode() { return moduleCode; }
    public String getActionCode() { return actionCode; }
    public String getDescription() { return description; }
    public PersistenceRecordStatus getStatus() { return status; }
}
