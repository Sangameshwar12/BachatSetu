package in.bachatsetu.backend.platformoperations.interfaces.rest.dto;

import java.time.Instant;

public record TenantResponse(
        String tenantId,
        String status,
        String suspensionReason,
        long users,
        long groups,
        long payments,
        long revenuePaise,
        long storageFiles,
        long storageBytes,
        long notifications,
        Instant lastActivityAt) {
}
