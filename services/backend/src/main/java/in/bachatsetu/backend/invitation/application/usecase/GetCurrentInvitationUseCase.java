package in.bachatsetu.backend.invitation.application.usecase;

import in.bachatsetu.backend.invitation.application.query.InvitationResult;
import in.bachatsetu.backend.shared.domain.AggregateId;

public interface GetCurrentInvitationUseCase {

    InvitationResult execute(AggregateId tenantId, AggregateId groupId, AggregateId actorId);
}
