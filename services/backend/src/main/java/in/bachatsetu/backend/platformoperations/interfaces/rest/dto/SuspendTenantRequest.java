package in.bachatsetu.backend.platformoperations.interfaces.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SuspendTenantRequest(@NotBlank @Size(max = 500) String reason) {
}
