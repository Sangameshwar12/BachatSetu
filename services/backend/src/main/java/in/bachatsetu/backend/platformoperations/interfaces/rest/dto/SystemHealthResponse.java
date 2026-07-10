package in.bachatsetu.backend.platformoperations.interfaces.rest.dto;

public record SystemHealthResponse(
        ComponentHealthResponse database,
        ComponentHealthResponse storage,
        ComponentHealthResponse notification,
        long uptimeSeconds,
        String javaVersion,
        String applicationVersion,
        String buildTimestamp,
        long usedMemoryBytes,
        long totalMemoryBytes,
        long maxMemoryBytes,
        long usableDiskBytes,
        long totalDiskBytes) {

    public record ComponentHealthResponse(String name, String status, String detail) {
    }
}
