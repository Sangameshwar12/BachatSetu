package in.bachatsetu.backend.infrastructure.persistence.entity.finance;

import in.bachatsetu.backend.infrastructure.persistence.entity.BaseJpaEntity;
import in.bachatsetu.backend.paymentgateway.domain.model.GatewayType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

@Entity
@Table(
        name = "payment_gateway_orders",
        schema = "finance",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_gateway_orders_payment", columnNames = "payment_id"),
            @UniqueConstraint(
                    name = "uk_gateway_orders_provider_order", columnNames = {"gateway_type", "provider_order_id"})
        },
        indexes = @Index(name = "idx_gateway_orders_tenant", columnList = "tenant_id"))
public class GatewayOrderJpaEntity extends BaseJpaEntity {

    @NotNull
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false, updatable = false)
    private PaymentJpaEntity payment;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "gateway_type", nullable = false, length = 20, updatable = false)
    private GatewayType gatewayType;

    @NotBlank
    @Size(max = 120)
    @Column(name = "provider_order_id", nullable = false, length = 120, updatable = false)
    private String providerOrderId;

    @Size(max = 500)
    @Column(name = "payment_link", length = 500, updatable = false)
    private String paymentLink;

    @Size(max = 60)
    @Column(name = "provider_status", length = 60)
    private String providerStatus;

    @Size(max = 120)
    @Column(name = "provider_refund_id", length = 120)
    private String providerRefundId;

    protected GatewayOrderJpaEntity() {
    }

    public GatewayOrderJpaEntity(
            UUID id, UUID tenantId, PaymentJpaEntity payment, GatewayType gatewayType,
            String providerOrderId, String paymentLink, String providerStatus, String providerRefundId) {
        super(id);
        this.tenantId = tenantId;
        this.payment = payment;
        this.gatewayType = gatewayType;
        this.providerOrderId = providerOrderId;
        this.paymentLink = paymentLink;
        this.providerStatus = providerStatus;
        this.providerRefundId = providerRefundId;
    }

    public UUID getTenantId() { return tenantId; }
    public PaymentJpaEntity getPayment() { return payment; }
    public GatewayType getGatewayType() { return gatewayType; }
    public String getProviderOrderId() { return providerOrderId; }
    public String getPaymentLink() { return paymentLink; }
    public String getProviderStatus() { return providerStatus; }
    public String getProviderRefundId() { return providerRefundId; }
}
