package in.bachatsetu.backend.infrastructure.persistence.entity.community;

import in.bachatsetu.backend.draw.domain.model.BidStatus;
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
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "auction_bids",
        schema = "community",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_auction_bids_draw_member", columnNames = {"draw_id", "group_member_id"}),
        indexes = @Index(name = "idx_auction_bids_draw_status", columnList = "draw_id,status"))
public class AuctionBidJpaEntity extends BaseJpaEntity {

    @NotNull
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private GroupJpaEntity group;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "draw_id", nullable = false)
    private DrawJpaEntity draw;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_member_id", nullable = false)
    private MemberJpaEntity member;

    @Positive
    @Column(name = "bid_amount_paise", nullable = false)
    private long bidAmountPaise;

    @PositiveOrZero
    @Column(name = "discount_amount_paise", nullable = false)
    private long discountAmountPaise;

    @NotBlank
    @Size(min = 3, max = 3)
    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Min(1)
    @Column(name = "bid_rank")
    private Integer rank;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private BidStatus status;

    @NotNull
    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt;

    protected AuctionBidJpaEntity() {
    }

    public AuctionBidJpaEntity(
            UUID id, UUID tenantId, GroupJpaEntity group, DrawJpaEntity draw,
            MemberJpaEntity member, long bidAmountPaise, long discountAmountPaise,
            String currencyCode, Integer rank, BidStatus status, Instant submittedAt) {
        super(id);
        this.tenantId = tenantId;
        this.group = group;
        this.draw = draw;
        this.member = member;
        this.bidAmountPaise = bidAmountPaise;
        this.discountAmountPaise = discountAmountPaise;
        this.currencyCode = currencyCode;
        this.rank = rank;
        this.status = status;
        this.submittedAt = submittedAt;
    }

    public UUID getTenantId() { return tenantId; }
    public GroupJpaEntity getGroup() { return group; }
    public DrawJpaEntity getDraw() { return draw; }
    public MemberJpaEntity getMember() { return member; }
    public long getBidAmountPaise() { return bidAmountPaise; }
    public long getDiscountAmountPaise() { return discountAmountPaise; }
    public String getCurrencyCode() { return currencyCode; }
    public Integer getRank() { return rank; }
    public BidStatus getStatus() { return status; }
    public Instant getSubmittedAt() { return submittedAt; }
}
