package in.bachatsetu.backend.payment.domain.model;

public enum PaymentStatus {
    INITIATED,
    PENDING_PROVIDER,
    VERIFIED,
    FAILED,
    CANCELLED,
    REFUNDED,
    DISPUTED
}
