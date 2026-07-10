package in.bachatsetu.backend.platformoperations.application.query;

import in.bachatsetu.backend.platformoperations.domain.model.ComponentHealth;

public record SystemHealthResult(
        ComponentHealth database,
        ComponentHealth storage,
        ComponentHealth notification,
        long uptimeSeconds,
        String javaVersion,
        String applicationVersion,
        String buildTimestamp,
        long usedMemoryBytes,
        long totalMemoryBytes,
        long maxMemoryBytes,
        long usableDiskBytes,
        long totalDiskBytes) {
}
