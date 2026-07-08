CREATE TABLE finance.payment_gateway_orders (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    payment_id UUID NOT NULL,
    gateway_type VARCHAR(20) NOT NULL,
    provider_order_id VARCHAR(120) NOT NULL,
    payment_link VARCHAR(500),
    provider_status VARCHAR(60),
    provider_refund_id VARCHAR(120),
    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID,
    CONSTRAINT uk_gateway_orders_payment UNIQUE (payment_id),
    CONSTRAINT uk_gateway_orders_provider_order UNIQUE (gateway_type, provider_order_id),
    CONSTRAINT fk_gateway_orders_payment FOREIGN KEY (payment_id)
        REFERENCES finance.payments (id) ON DELETE RESTRICT,
    CONSTRAINT ck_gateway_orders_type CHECK (gateway_type IN ('RAZORPAY', 'STRIPE', 'CASHFREE')),
    CONSTRAINT ck_gateway_orders_audit_time CHECK (updated_at >= created_at),
    CONSTRAINT ck_gateway_orders_soft_delete CHECK (
        (is_deleted = FALSE AND deleted_at IS NULL AND deleted_by IS NULL)
        OR (is_deleted = TRUE AND deleted_at IS NOT NULL AND deleted_by IS NOT NULL)
    )
);

CREATE INDEX idx_gateway_orders_tenant ON finance.payment_gateway_orders (tenant_id);
