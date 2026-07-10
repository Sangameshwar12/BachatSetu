package in.bachatsetu.backend.support.interfaces.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record AssignTicketRequest(@NotBlank String assigneeId) {
}
