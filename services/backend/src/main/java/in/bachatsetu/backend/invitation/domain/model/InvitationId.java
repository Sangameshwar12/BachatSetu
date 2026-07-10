package in.bachatsetu.backend.invitation.domain.model;

import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

/** Strongly typed identity of a group invitation aggregate. */
public record InvitationId(AggregateId value) {

    public InvitationId {
        Objects.requireNonNull(value, "invitation id must not be null");
    }

    public static InvitationId newId() {
        return new InvitationId(AggregateId.newId());
    }
}
