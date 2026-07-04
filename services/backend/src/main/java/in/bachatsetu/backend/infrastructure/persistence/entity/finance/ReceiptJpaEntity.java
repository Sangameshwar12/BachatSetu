package in.bachatsetu.backend.infrastructure.persistence.entity.finance;

import in.bachatsetu.backend.infrastructure.persistence.entity.BaseJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.identity.UserJpaEntity;
import in.bachatsetu.backend.receipt.domain.model.ReceiptStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "receipts",
        schema = "finance",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_receipts_tenant_number", columnNames = {"tenant_id", "receipt_number"}),
            @UniqueConstraint(name = "uk_receipts_payment", columnNames = "payment_id")
        },
        indexes = @Index(name = "idx_receipts_user_date", columnList = "user_id,receipt_date"))
public class ReceiptJpaEntity extends BaseJpaEntity {

    @NotNull
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false, unique = true)
    private PaymentJpaEntity payment;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserJpaEntity user;

    @NotBlank
    @Size(max = 40)
    @Column(name = "receipt_number", nullable = false, length = 40)
    private String number;

    @NotNull
    @Column(name = "receipt_date", nullable = false)
    private LocalDate receiptDate;

    @Positive
    @Column(name = "amount_paise", nullable = false)
    private long amountPaise;

    @NotBlank
    @Size(min = 3, max = 3)
    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false, length = 30)
    private ReceiptStatus status;

    @Size(max = 300)
    @Column(name = "cancellation_reason", length = 300)
    private String cancellationReason;

    protected ReceiptJpaEntity() {
    }

    public ReceiptJpaEntity(
            UUID id, UUID tenantId, PaymentJpaEntity payment, UserJpaEntity user,
            String number, LocalDate receiptDate, long amountPaise, String currencyCode,
            ReceiptStatus status, String cancellationReason) {
        super(id);
        this.tenantId = tenantId;
        this.payment = payment;
        this.user = user;
        this.number = number;
        this.receiptDate = receiptDate;
        this.amountPaise = amountPaise;
        this.currencyCode = currencyCode;
        this.status = status;
        this.cancellationReason = cancellationReason;
    }

    public UUID getTenantId() { return tenantId; }
    public PaymentJpaEntity getPayment() { return payment; }
    public UserJpaEntity getUser() { return user; }
    public String getNumber() { return number; }
    public LocalDate getReceiptDate() { return receiptDate; }
    public long getAmountPaise() { return amountPaise; }
    public String getCurrencyCode() { return currencyCode; }
    public ReceiptStatus getStatus() { return status; }
    public String getCancellationReason() { return cancellationReason; }
}
