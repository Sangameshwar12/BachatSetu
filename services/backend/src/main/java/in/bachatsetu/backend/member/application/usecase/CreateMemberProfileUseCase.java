package in.bachatsetu.backend.member.application.usecase;

import in.bachatsetu.backend.member.application.command.CreateMemberProfileCommand;
import in.bachatsetu.backend.member.application.query.MemberProfileResult;

/** Creates a member profile together with its first group participation. */
@FunctionalInterface
public interface CreateMemberProfileUseCase {

    MemberProfileResult execute(CreateMemberProfileCommand command);
}
