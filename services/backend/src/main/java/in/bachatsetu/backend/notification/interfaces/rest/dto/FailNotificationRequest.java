package in.bachatsetu.backend.notification.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request to mark an existing notification as failed. */
public record FailNotificationRequest(

        @NotBlank
        @Size(max = 80)
        @Schema(description = "Short code describing why dispatch failed", example = "provider-unreachable")
        String failureCode) {
}
