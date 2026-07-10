package in.bachatsetu.backend.platformoperations.application.usecase;

import in.bachatsetu.backend.platformoperations.application.command.SendBroadcastNotificationCommand;
import in.bachatsetu.backend.platformoperations.application.query.BroadcastResult;

@FunctionalInterface
public interface SendBroadcastNotificationUseCase {

    BroadcastResult execute(SendBroadcastNotificationCommand command);
}
