package in.bachatsetu.backend.infrastructure.persistence.entity.community;

import in.bachatsetu.backend.infrastructure.persistence.entity.BaseJpaEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "monthly_cycles",
        schema = "community",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_monthly_cycles_group_number", columnNames = {"group_id", "cycle_number"}),
        indexes = @Index(name = "idx_monthly_cycles_due_status", columnList = "due_date,status"))
public class MonthlyCycleJpaEntity extends BaseJpaEntity {

    @NotNull
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private GroupJpaEntity group;

    @Min(1)
    @Column(name = "cycle_number", nullable = false)
    private int cycleNumber;

    @NotNull
    @Column(name = "cycle_month", nullable = false)
    private LocalDate cycleMonth;

    @NotNull
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private CycleStatus status;

    @Column(name = "opened_at")
    private Instant openedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @OneToMany(mappedBy = "cycle", fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @OrderBy("dueDate ASC")
    private List<InstallmentJpaEntity> installments = new ArrayList<>();

    protected MonthlyCycleJpaEntity() {
    }

    public MonthlyCycleJpaEntity(
            UUID id, UUID tenantId, GroupJpaEntity group, int cycleNumber,
            LocalDate cycleMonth, LocalDate dueDate, CycleStatus status,
            Instant openedAt, Instant closedAt) {
        super(id);
        this.tenantId = tenantId;
        this.group = group;
        this.cycleNumber = cycleNumber;
        this.cycleMonth = cycleMonth;
        this.dueDate = dueDate;
        this.status = status;
        this.openedAt = openedAt;
        this.closedAt = closedAt;
    }

    public UUID getTenantId() { return tenantId; }
    public GroupJpaEntity getGroup() { return group; }
    public int getCycleNumber() { return cycleNumber; }
    public LocalDate getCycleMonth() { return cycleMonth; }
    public LocalDate getDueDate() { return dueDate; }
    public CycleStatus getStatus() { return status; }
    public Instant getOpenedAt() { return openedAt; }
    public Instant getClosedAt() { return closedAt; }
    public List<InstallmentJpaEntity> getInstallments() { return Collections.unmodifiableList(installments); }
}
