package in.bachatsetu.backend.admin.interfaces.rest.dto.config;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Whether a single platform feature is currently enabled.")
public record FeatureFlagResponse(String key, boolean enabled, long version, String updatedAt, String updatedBy) {
}
