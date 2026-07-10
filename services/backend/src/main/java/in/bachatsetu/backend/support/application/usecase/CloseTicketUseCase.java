package in.bachatsetu.backend.support.application.usecase;

import in.bachatsetu.backend.support.application.command.CloseTicketCommand;
import in.bachatsetu.backend.support.application.query.SupportTicketResult;

@FunctionalInterface
public interface CloseTicketUseCase {

    SupportTicketResult execute(CloseTicketCommand command);
}
