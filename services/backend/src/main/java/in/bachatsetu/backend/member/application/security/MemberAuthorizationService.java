package in.bachatsetu.backend.member.application.security;

import in.bachatsetu.backend.member.application.exception.MemberAccessDeniedException;
import in.bachatsetu.backend.member.domain.model.MemberProfile;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

/** Enforces which actor may perform self-restricted Member operations. */
public final class MemberAuthorizationService {

    public void requireSelf(MemberProfile member, AggregateId actorId) {
        Objects.requireNonNull(member, "member must not be null");
        Objects.requireNonNull(actorId, "actor id must not be null");
        if (!member.userId().equals(actorId)) {
            throw new MemberAccessDeniedException("only the member themselves may perform this operation");
        }
    }

    /** Overload for operations where no {@link MemberProfile} is loaded yet (e.g. creation). */
    public void requireSelf(AggregateId targetUserId, AggregateId actorId) {
        Objects.requireNonNull(targetUserId, "target user id must not be null");
        Objects.requireNonNull(actorId, "actor id must not be null");
        if (!targetUserId.equals(actorId)) {
            throw new MemberAccessDeniedException("only the member themselves may perform this operation");
        }
    }
}
