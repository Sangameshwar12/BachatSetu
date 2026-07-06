package in.bachatsetu.backend.member.application.usecase;

import in.bachatsetu.backend.member.application.command.JoinGroupParticipationCommand;
import in.bachatsetu.backend.member.application.query.MemberProfileResult;

/** Adds a further group participation to an already-persisted member profile. */
@FunctionalInterface
public interface JoinGroupParticipationUseCase {

    MemberProfileResult execute(JoinGroupParticipationCommand command);
}
