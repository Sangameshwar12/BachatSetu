package in.bachatsetu.backend.support.application.usecase;

import in.bachatsetu.backend.support.application.command.AssignTicketCommand;
import in.bachatsetu.backend.support.application.query.SupportTicketResult;

@FunctionalInterface
public interface AssignTicketUseCase {

    SupportTicketResult execute(AssignTicketCommand command);
}
