package in.bachatsetu.backend.member.domain.port;

import in.bachatsetu.backend.member.domain.model.MemberNumber;
import in.bachatsetu.backend.member.domain.model.MemberProfile;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Optional;

public interface MemberRepository {

    Optional<MemberProfile> findById(AggregateId memberId);

    Optional<MemberProfile> findById(AggregateId tenantId, AggregateId memberId);

    Optional<MemberProfile> findByUserId(AggregateId tenantId, AggregateId userId);

    Optional<MemberProfile> findByMemberNumber(AggregateId tenantId, MemberNumber memberNumber);

    void save(MemberProfile member);
}
