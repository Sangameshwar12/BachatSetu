package in.bachatsetu.backend.audit.domain.model;

/** Categorizes the business action a single {@link AuditEntry} records. */
public enum AuditEventType {
    LOGIN,
    LOGOUT,
    OTP_SENT,
    OTP_VERIFIED,
    GROUP_CREATED,
    GROUP_UPDATED,
    GROUP_CLOSED,
    MEMBER_ADDED,
    MEMBER_REMOVED,
    PAYMENT_CREATED,
    PAYMENT_VERIFIED,
    PAYMENT_REFUNDED,
    DRAW_CREATED,
    DRAW_COMPLETED,
    RECEIPT_GENERATED,
    PDF_DOWNLOADED,
    NOTIFICATION_SENT,
    FILE_UPLOADED,
    FILE_DELETED,
    GATEWAY_REFUND_INITIATED,
    GATEWAY_WEBHOOK_PROCESSED,
    SYSTEM_EVENT
}
