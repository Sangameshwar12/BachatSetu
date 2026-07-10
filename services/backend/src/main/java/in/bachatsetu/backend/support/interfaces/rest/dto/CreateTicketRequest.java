package in.bachatsetu.backend.support.interfaces.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTicketRequest(
        @NotNull String category,
        @NotNull String priority,
        @NotBlank @Size(max = 200) String subject,
        @NotBlank @Size(max = 4000) String description) {
}
