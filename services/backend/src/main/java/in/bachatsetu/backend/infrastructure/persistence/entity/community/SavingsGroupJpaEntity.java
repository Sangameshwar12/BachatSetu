package in.bachatsetu.backend.infrastructure.persistence.entity.community;

import in.bachatsetu.backend.group.domain.model.ContributionFrequency;
import in.bachatsetu.backend.group.domain.model.GroupStatus;
import in.bachatsetu.backend.group.domain.model.GroupType;
import in.bachatsetu.backend.group.domain.model.PayoutMethod;
import in.bachatsetu.backend.infrastructure.persistence.entity.BaseJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.identity.UserJpaEntity;
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
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Canonical persistence representation of the Savings Group aggregate root. */
@Entity
@Table(
        name = "groups",
        schema = "community",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_groups_tenant_code", columnNames = {"tenant_id", "group_code"}),
        indexes = {
            @Index(name = "idx_groups_tenant_status", columnList = "tenant_id,status"),
            @Index(name = "idx_groups_organizer", columnList = "organizer_user_id")
        })
public class SavingsGroupJpaEntity extends BaseJpaEntity {

    @NotNull
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organizer_user_id", nullable = false, updatable = false)
    private UserJpaEntity organizer;

    @NotBlank
    @Size(max = 20)
    @Column(name = "group_code", nullable = false, length = 20)
    private String code;

    @NotBlank
    @Size(max = 100)
    @Column(name = "group_name", nullable = false, length = 100)
    private String name;

    @NotNull
    @Column(name = "description", nullable = false, columnDefinition = "text")
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "module_type", nullable = false, length = 40)
    private GroupType type;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private GroupStatus status;

    @Positive
    @Column(name = "contribution_amount_paise", nullable = false)
    private long contributionAmountPaise;

    @NotBlank
    @Size(min = 3, max = 3)
    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false, length = 20)
    private ContributionFrequency frequency;

    @NotNull
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Min(1)
    @Max(120)
    @Column(name = "duration_cycles", nullable = false)
    private int durationCycles;

    @Min(2)
    @Column(name = "minimum_members", nullable = false)
    private int minimumMembers;

    @Min(2)
    @Max(500)
    @Column(name = "maximum_members", nullable = false)
    private int maximumMembers;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "payout_method", nullable = false, length = 30)
    private PayoutMethod payoutMethod;

    @Column(name = "partial_payment_allowed", nullable = false)
    private boolean partialPaymentAllowed;

    @OneToMany(
            mappedBy = "group",
            fetch = FetchType.LAZY,
            cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @OrderBy("joinedAt ASC")
    private List<GroupMemberJpaEntity> members = new ArrayList<>();

    @OneToMany(mappedBy = "group", fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @OrderBy("cycleNumber ASC")
    private List<MonthlyCycleJpaEntity> cycles = new ArrayList<>();

    @OneToMany(mappedBy = "group", fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @OrderBy("scheduledAt ASC")
    private List<DrawJpaEntity> draws = new ArrayList<>();

    protected SavingsGroupJpaEntity() {
    }

    public SavingsGroupJpaEntity(
            UUID id,
            UUID tenantId,
            UserJpaEntity organizer,
            String code,
            String name,
            String description,
            GroupType type,
            GroupStatus status,
            long contributionAmountPaise,
            String currencyCode,
            ContributionFrequency frequency,
            LocalDate startDate,
            int durationCycles,
            int minimumMembers,
            int maximumMembers,
            PayoutMethod payoutMethod,
            boolean partialPaymentAllowed) {
        super(id);
        update(
                tenantId,
                organizer,
                code,
                name,
                description,
                type,
                status,
                contributionAmountPaise,
                currencyCode,
                frequency,
                startDate,
                durationCycles,
                minimumMembers,
                maximumMembers,
                payoutMethod,
                partialPaymentAllowed);
    }

    public final void update(
            UUID tenantId,
            UserJpaEntity organizer,
            String code,
            String name,
            String description,
            GroupType type,
            GroupStatus status,
            long contributionAmountPaise,
            String currencyCode,
            ContributionFrequency frequency,
            LocalDate startDate,
            int durationCycles,
            int minimumMembers,
            int maximumMembers,
            PayoutMethod payoutMethod,
            boolean partialPaymentAllowed) {
        this.tenantId = tenantId;
        this.organizer = organizer;
        this.code = code;
        this.name = name;
        this.description = description;
        this.type = type;
        this.status = status;
        this.contributionAmountPaise = contributionAmountPaise;
        this.currencyCode = currencyCode;
        this.frequency = frequency;
        this.startDate = startDate;
        this.durationCycles = durationCycles;
        this.minimumMembers = minimumMembers;
        this.maximumMembers = maximumMembers;
        this.payoutMethod = payoutMethod;
        this.partialPaymentAllowed = partialPaymentAllowed;
    }

    public void synchronizeMembers(List<GroupMemberJpaEntity> desiredMembers) {
        Map<UUID, GroupMemberJpaEntity> existingById = new HashMap<>();
        for (GroupMemberJpaEntity member : members) {
            existingById.put(member.getId(), member);
        }
        List<GroupMemberJpaEntity> synchronizedMembers = new ArrayList<>();
        for (GroupMemberJpaEntity desired : desiredMembers) {
            GroupMemberJpaEntity existing = existingById.get(desired.getId());
            if (existing == null) {
                synchronizedMembers.add(desired);
            } else {
                existing.synchronizeFrom(desired);
                synchronizedMembers.add(existing);
            }
        }
        members.clear();
        members.addAll(synchronizedMembers);
    }

    public Optional<UUID> membershipIdForUser(UUID userId) {
        return members.stream()
                .filter(member -> member.getUser().getId().equals(userId))
                .map(GroupMemberJpaEntity::getId)
                .findFirst();
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UserJpaEntity getOrganizer() {
        return organizer;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public GroupType getType() {
        return type;
    }

    public GroupStatus getStatus() {
        return status;
    }

    public long getContributionAmountPaise() {
        return contributionAmountPaise;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public ContributionFrequency getFrequency() {
        return frequency;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public int getDurationCycles() {
        return durationCycles;
    }

    public int getMinimumMembers() {
        return minimumMembers;
    }

    public int getMaximumMembers() {
        return maximumMembers;
    }

    public PayoutMethod getPayoutMethod() {
        return payoutMethod;
    }

    public boolean isPartialPaymentAllowed() {
        return partialPaymentAllowed;
    }

    public List<GroupMemberJpaEntity> getMembers() {
        return Collections.unmodifiableList(members);
    }

    public List<MonthlyCycleJpaEntity> getCycles() {
        return Collections.unmodifiableList(cycles);
    }

    public List<DrawJpaEntity> getDraws() {
        return Collections.unmodifiableList(draws);
    }
}
