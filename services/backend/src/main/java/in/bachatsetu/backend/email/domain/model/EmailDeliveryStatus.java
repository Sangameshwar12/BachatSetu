package in.bachatsetu.backend.email.domain.model;

/** Outcome of one send attempt, after every configured retry has been exhausted. */
public enum EmailDeliveryStatus {
    SENT,
    FAILED
}
