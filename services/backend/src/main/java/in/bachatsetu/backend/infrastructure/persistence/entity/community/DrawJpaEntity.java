package in.bachatsetu.backend.infrastructure.persistence.entity.community;

import in.bachatsetu.backend.draw.domain.model.DrawStatus;
import in.bachatsetu.backend.draw.domain.model.DrawType;
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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "draws",
        schema = "community",
        uniqueConstraints = @UniqueConstraint(name = "uk_draws_cycle", columnNames = "cycle_id"),
        indexes = @Index(name = "idx_draws_group_status", columnList = "group_id,status"))
public class DrawJpaEntity extends BaseJpaEntity {

    @NotNull
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private SavingsGroupJpaEntity group;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cycle_id", nullable = false, unique = true)
    private MonthlyCycleJpaEntity cycle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_group_member_id")
    private GroupMemberJpaEntity selectedMember;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "draw_type", nullable = false, length = 30)
    private DrawType type;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private DrawStatus status;

    @NotNull
    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @PositiveOrZero
    @Column(name = "payout_amount_paise", nullable = false)
    private long payoutAmountPaise;

    @NotBlank
    @Size(min = 3, max = 3)
    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @OneToMany(mappedBy = "draw", fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @OrderBy("submittedAt ASC")
    private List<AuctionBidJpaEntity> bids = new ArrayList<>();

    protected DrawJpaEntity() {
    }

    public DrawJpaEntity(
            UUID id, UUID tenantId, SavingsGroupJpaEntity group, MonthlyCycleJpaEntity cycle,
            GroupMemberJpaEntity selectedMember, DrawType type, DrawStatus status,
            Instant scheduledAt, Instant completedAt, long payoutAmountPaise,
            String currencyCode) {
        super(id);
        this.tenantId = tenantId;
        this.group = group;
        this.cycle = cycle;
        this.selectedMember = selectedMember;
        this.type = type;
        this.status = status;
        this.scheduledAt = scheduledAt;
        this.completedAt = completedAt;
        this.payoutAmountPaise = payoutAmountPaise;
        this.currencyCode = currencyCode;
    }

    public UUID getTenantId() { return tenantId; }
    public SavingsGroupJpaEntity getGroup() { return group; }
    public MonthlyCycleJpaEntity getCycle() { return cycle; }
    public GroupMemberJpaEntity getSelectedMember() { return selectedMember; }
    public DrawType getType() { return type; }
    public DrawStatus getStatus() { return status; }
    public Instant getScheduledAt() { return scheduledAt; }
    public Instant getCompletedAt() { return completedAt; }
    public long getPayoutAmountPaise() { return payoutAmountPaise; }
    public String getCurrencyCode() { return currencyCode; }
    public List<AuctionBidJpaEntity> getBids() { return Collections.unmodifiableList(bids); }
}
