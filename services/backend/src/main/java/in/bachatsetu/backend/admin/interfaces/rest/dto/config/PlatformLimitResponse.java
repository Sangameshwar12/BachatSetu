package in.bachatsetu.backend.admin.interfaces.rest.dto.config;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A single configurable platform-wide ceiling.")
public record PlatformLimitResponse(String key, long value, long version, String updatedAt, String updatedBy) {
}
