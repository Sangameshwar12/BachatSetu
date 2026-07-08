package in.bachatsetu.backend.notification.application.usecase;

import in.bachatsetu.backend.notification.application.command.MarkNotificationDeliveredCommand;
import in.bachatsetu.backend.notification.application.query.NotificationResult;

/** Transitions an existing notification to DELIVERED. */
@FunctionalInterface
public interface MarkNotificationDeliveredUseCase {

    NotificationResult execute(MarkNotificationDeliveredCommand command);
}
