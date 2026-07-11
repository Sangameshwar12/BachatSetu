package in.bachatsetu.backend.infrastructure.email;

/** Reported by {@link EmailProviderHealthIndicator}: no send attempted yet, healthy, or failing. */
public enum EmailProviderHealthStatus {
    UNKNOWN,
    UP,
    DOWN
}
