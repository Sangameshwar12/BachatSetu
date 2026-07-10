package in.bachatsetu.backend.platformoperations.domain.model;

/** Read-only JVM/host runtime facts, for the System Health endpoint only. */
public record SystemRuntimeInfo(
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
