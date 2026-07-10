package in.bachatsetu.backend.invitation.application.query;

import in.bachatsetu.backend.invitation.domain.model.InvitationStatus;
import in.bachatsetu.backend.invitation.domain.model.InvitationType;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.Objects;

public record InvitationResult(
        AggregateId invitationId,
        AggregateId groupId,
        String code,
        String token,
        InvitationType type,
        InvitationStatus status,
        Instant expiresAt) {

    public InvitationResult {
        Objects.requireNonNull(invitationId, "invitationId must not be null");
        Objects.requireNonNull(groupId, "groupId must not be null");
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(token, "token must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(expiresAt, "expiresAt must not be null");
    }
}
