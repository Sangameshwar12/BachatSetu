package in.bachatsetu.backend.infrastructure.persistence.entity.community;

public enum InstallmentStatus {
    PENDING,
    DUE,
    PARTIALLY_PAID,
    PAID,
    OVERDUE,
    WAIVED,
    DISPUTED,
    CANCELLED
}
