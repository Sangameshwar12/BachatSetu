package in.bachatsetu.backend.member.application.usecase;

import in.bachatsetu.backend.member.application.query.MemberProfileSummary;
import in.bachatsetu.backend.member.domain.port.MemberPage;
import in.bachatsetu.backend.member.domain.port.MemberPageRequest;
import in.bachatsetu.backend.shared.domain.AggregateId;

/** Lists compact member profile views within a tenant, paginated at the persistence boundary. */
@FunctionalInterface
public interface ListMemberProfilesUseCase {

    MemberPage<MemberProfileSummary> execute(AggregateId tenantId, MemberPageRequest pageRequest);
}
