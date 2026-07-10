package in.bachatsetu.backend.support.application.usecase;

import in.bachatsetu.backend.support.application.command.ResolveTicketCommand;
import in.bachatsetu.backend.support.application.query.SupportTicketResult;

@FunctionalInterface
public interface ResolveTicketUseCase {

    SupportTicketResult execute(ResolveTicketCommand command);
}
