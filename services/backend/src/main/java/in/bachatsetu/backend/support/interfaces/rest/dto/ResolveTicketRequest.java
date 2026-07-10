package in.bachatsetu.backend.support.interfaces.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResolveTicketRequest(@NotBlank @Size(max = 4000) String resolution) {
}
