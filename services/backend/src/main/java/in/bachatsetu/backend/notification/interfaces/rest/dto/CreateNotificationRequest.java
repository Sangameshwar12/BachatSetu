package in.bachatsetu.backend.notification.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Map;

/** Request to create and immediately dispatch a new notification. */
public record CreateNotificationRequest(

        @NotBlank
        @Size(min = 36, max = 36)
        @Pattern(regexp = ValidationPatterns.UUID, message = "must be a valid UUID")
        @Schema(description = "Identifier of the recipient user", example = "123e4567-e89b-12d3-a456-426614174000")
        String recipientUserId,

        @NotBlank
        @Size(max = 254)
        @Schema(description = "Channel-specific destination (email address, phone number, or device reference)",
                example = "member@example.com")
        String destination,

        @NotBlank
        @Pattern(regexp = ValidationPatterns.CHANNEL)
        @Schema(description = "Delivery channel", example = "EMAIL")
        String channel,

        @NotBlank
        @Pattern(regexp = ValidationPatterns.CATEGORY)
        @Schema(description = "Notification category, selecting the message template", example = "PAYMENT_RECEIPT")
        String category,

        @Schema(description = "Placeholder values substituted into the category's template, e.g. memberName, "
                + "groupName, amount, drawNumber, receiptNumber")
        Map<String, String> placeholders) {

    public CreateNotificationRequest {
        placeholders = placeholders == null ? Map.of() : Map.copyOf(placeholders);
    }
}
