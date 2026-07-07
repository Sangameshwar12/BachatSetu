package in.bachatsetu.backend.member.application.command;

import in.bachatsetu.backend.member.domain.model.MemberStatus;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

/** Requests a lifecycle status change for an existing member profile. */
public record UpdateMemberProfileCommand(
        AggregateId tenantId,
        AggregateId memberId,
        MemberStatus status,
        AggregateId actorId) {

    public UpdateMemberProfileCommand {
        Objects.requireNonNull(tenantId, "tenant id must not be null");
        Objects.requireNonNull(memberId, "member id must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(actorId, "actor id must not be null");
    }
}
