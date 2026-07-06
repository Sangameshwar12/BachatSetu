package in.bachatsetu.backend.infrastructure.persistence.entity.finance;

import in.bachatsetu.backend.infrastructure.persistence.entity.BaseJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.community.SavingsGroupJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.identity.UserJpaEntity;
import in.bachatsetu.backend.payment.domain.model.PaymentMethod;
import in.bachatsetu.backend.payment.domain.model.PaymentStatus;
import in.bachatsetu.backend.payment.domain.model.ReconciliationStatus;
import jakarta.persistence.CascadeType;
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
import java.util.UUID;

@Entity
@Table(
        name = "payments",
        schema = "finance",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_payments_reference", columnNames = "payment_reference"),
            @UniqueConstraint(
                    name = "uk_payments_tenant_idempotency", columnNames = {"tenant_id", "idempotency_key_hash"})
        },
        indexes = @Index(name = "idx_payments_status", columnList = "tenant_id,status"))
public class PaymentJpaEntity extends BaseJpaEntity {

    @NotNull
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payer_user_id", nullable = false)
    private UserJpaEntity payer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private SavingsGroupJpaEntity group;

    @NotBlank
    @Size(max = 40)
    @Column(name = "payment_reference", nullable = false, length = 40)
    private String reference;

    @Positive
    @Column(name = "amount_paise", nullable = false)
    private long amountPaise;

    @NotBlank
    @Size(min = 3, max = 3)
    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod method;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PaymentStatus status;

    @NotBlank
    @Size(max = 128)
    @Column(name = "idempotency_key_hash", nullable = false, length = 128)
    private String idempotencyKeyHash;

    @Size(max = 50)
    @Column(name = "provider_name", length = 50)
    private String providerName;

    @Size(max = 120)
    @Column(name = "provider_payment_reference", length = 120)
    private String providerPaymentReference;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "reconciliation_status", nullable = false, length = 30)
    private ReconciliationStatus reconciliationStatus;

    @OneToOne(mappedBy = "payment", fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private ReceiptJpaEntity receipt;

    protected PaymentJpaEntity() {
    }

    public PaymentJpaEntity(
            UUID id, UUID tenantId, UserJpaEntity payer, SavingsGroupJpaEntity group,
            String reference, long amountPaise, String currencyCode,
            PaymentMethod method, PaymentStatus status, String idempotencyKeyHash,
            String providerName, String providerPaymentReference,
            ReconciliationStatus reconciliationStatus) {
        super(id);
        this.tenantId = tenantId;
        this.payer = payer;
        this.group = group;
        this.reference = reference;
        this.amountPaise = amountPaise;
        this.currencyCode = currencyCode;
        this.method = method;
        this.status = status;
        this.idempotencyKeyHash = idempotencyKeyHash;
        this.providerName = providerName;
        this.providerPaymentReference = providerPaymentReference;
        this.reconciliationStatus = reconciliationStatus;
    }

    public UUID getTenantId() { return tenantId; }
    public UserJpaEntity getPayer() { return payer; }
    public SavingsGroupJpaEntity getGroup() { return group; }
    public String getReference() { return reference; }
    public long getAmountPaise() { return amountPaise; }
    public String getCurrencyCode() { return currencyCode; }
    public PaymentMethod getMethod() { return method; }
    public PaymentStatus getStatus() { return status; }
    public String getIdempotencyKeyHash() { return idempotencyKeyHash; }
    public String getProviderName() { return providerName; }
    public String getProviderPaymentReference() { return providerPaymentReference; }
    public ReconciliationStatus getReconciliationStatus() { return reconciliationStatus; }
    public ReceiptJpaEntity getReceipt() { return receipt; }
}
