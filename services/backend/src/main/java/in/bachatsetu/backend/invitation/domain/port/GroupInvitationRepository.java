package in.bachatsetu.backend.invitation.domain.port;

import in.bachatsetu.backend.invitation.domain.model.GroupInvitation;
import in.bachatsetu.backend.invitation.domain.model.InvitationCode;
import in.bachatsetu.backend.invitation.domain.model.InvitationToken;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Optional;

/** Tenant-scoped persistence boundary for {@link GroupInvitation}. */
public interface GroupInvitationRepository {

    void save(GroupInvitation invitation);

    Optional<GroupInvitation> findById(AggregateId tenantId, AggregateId invitationId);

    Optional<GroupInvitation> findActiveByGroup(AggregateId tenantId, AggregateId groupId);

    Optional<GroupInvitation> findByCode(AggregateId tenantId, InvitationCode code);

    Optional<GroupInvitation> findByToken(InvitationToken token);
}
