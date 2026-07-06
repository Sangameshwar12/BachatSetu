package in.bachatsetu.backend.infrastructure.persistence.entity.community;

import in.bachatsetu.backend.infrastructure.persistence.entity.BaseJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "installments",
        schema = "community",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_installments_cycle_member", columnNames = {"cycle_id", "group_member_id"}),
        indexes = @Index(name = "idx_installments_due_status", columnList = "due_date,status"))
public class InstallmentJpaEntity extends BaseJpaEntity {

    @NotNull
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private SavingsGroupJpaEntity group;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cycle_id", nullable = false)
    private MonthlyCycleJpaEntity cycle;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_member_id", nullable = false)
    private GroupMemberJpaEntity member;

    @PositiveOrZero
    @Column(name = "expected_amount_paise", nullable = false)
    private long expectedAmountPaise;

    @PositiveOrZero
    @Column(name = "paid_amount_paise", nullable = false)
    private long paidAmountPaise;

    @PositiveOrZero
    @Column(name = "penalty_amount_paise", nullable = false)
    private long penaltyAmountPaise;

    @NotBlank
    @Size(min = 3, max = 3)
    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @NotNull
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private InstallmentStatus status;

    @Column(name = "paid_at")
    private Instant paidAt;

    protected InstallmentJpaEntity() {
    }

    public InstallmentJpaEntity(
            UUID id, UUID tenantId, SavingsGroupJpaEntity group, MonthlyCycleJpaEntity cycle,
            GroupMemberJpaEntity member, long expectedAmountPaise, long paidAmountPaise,
            long penaltyAmountPaise, String currencyCode, LocalDate dueDate,
            InstallmentStatus status, Instant paidAt) {
        super(id);
        this.tenantId = tenantId;
        this.group = group;
        this.cycle = cycle;
        this.member = member;
        this.expectedAmountPaise = expectedAmountPaise;
        this.paidAmountPaise = paidAmountPaise;
        this.penaltyAmountPaise = penaltyAmountPaise;
        this.currencyCode = currencyCode;
        this.dueDate = dueDate;
        this.status = status;
        this.paidAt = paidAt;
    }

    public UUID getTenantId() { return tenantId; }
    public SavingsGroupJpaEntity getGroup() { return group; }
    public MonthlyCycleJpaEntity getCycle() { return cycle; }
    public GroupMemberJpaEntity getMember() { return member; }
    public long getExpectedAmountPaise() { return expectedAmountPaise; }
    public long getPaidAmountPaise() { return paidAmountPaise; }
    public long getPenaltyAmountPaise() { return penaltyAmountPaise; }
    public String getCurrencyCode() { return currencyCode; }
    public LocalDate getDueDate() { return dueDate; }
    public InstallmentStatus getStatus() { return status; }
    public Instant getPaidAt() { return paidAt; }
}
