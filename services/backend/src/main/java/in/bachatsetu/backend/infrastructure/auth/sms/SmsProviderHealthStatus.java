package in.bachatsetu.backend.infrastructure.auth.sms;

/** Reported by {@link SmsProviderHealthIndicator}: no send attempted yet, healthy, or failing. */
public enum SmsProviderHealthStatus {
    UNKNOWN,
    UP,
    DOWN
}
