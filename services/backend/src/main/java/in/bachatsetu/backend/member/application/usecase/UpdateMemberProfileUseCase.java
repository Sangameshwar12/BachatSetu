package in.bachatsetu.backend.member.application.usecase;

import in.bachatsetu.backend.member.application.command.UpdateMemberProfileCommand;
import in.bachatsetu.backend.member.application.query.MemberProfileResult;

/** Updates the lifecycle status of an existing member profile and returns its current state. */
@FunctionalInterface
public interface UpdateMemberProfileUseCase {

    MemberProfileResult execute(UpdateMemberProfileCommand command);
}
