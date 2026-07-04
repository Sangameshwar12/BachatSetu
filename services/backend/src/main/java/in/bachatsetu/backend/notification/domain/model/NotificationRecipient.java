package in.bachatsetu.backend.notification.domain.model;

import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

public record NotificationRecipient(AggregateId userId, String destination) {

    public NotificationRecipient {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(destination, "destination must not be null");
        destination = destination.strip();
        if (destination.isEmpty() || destination.length() > 254) {
            throw new IllegalArgumentException("notification destination length is invalid");
        }
    }
}
