package in.bachatsetu.backend.platformoperations.interfaces.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BroadcastRequest(
        @NotNull String scope,
        String tenantId,
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 2000) String message) {
}
