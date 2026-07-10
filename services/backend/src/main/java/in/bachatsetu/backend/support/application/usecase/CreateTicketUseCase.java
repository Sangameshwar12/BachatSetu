package in.bachatsetu.backend.support.application.usecase;

import in.bachatsetu.backend.support.application.command.CreateTicketCommand;
import in.bachatsetu.backend.support.application.query.SupportTicketResult;

@FunctionalInterface
public interface CreateTicketUseCase {

    SupportTicketResult execute(CreateTicketCommand command);
}
