package in.bachatsetu.backend.member.application.usecase;

import in.bachatsetu.backend.member.application.query.MemberProfileResult;
import in.bachatsetu.backend.shared.domain.AggregateId;

/** Retrieves one tenant-scoped member profile. */
@FunctionalInterface
public interface GetMemberProfileUseCase {

    MemberProfileResult execute(AggregateId tenantId, AggregateId memberId);
}
