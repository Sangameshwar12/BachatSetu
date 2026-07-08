package in.bachatsetu.backend.notification.application.usecase;

import in.bachatsetu.backend.notification.application.command.MarkNotificationFailedCommand;
import in.bachatsetu.backend.notification.application.query.NotificationResult;

/** Transitions an existing notification to FAILED. */
@FunctionalInterface
public interface MarkNotificationFailedUseCase {

    NotificationResult execute(MarkNotificationFailedCommand command);
}
