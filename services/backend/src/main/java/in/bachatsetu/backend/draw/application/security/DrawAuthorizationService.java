package in.bachatsetu.backend.draw.application.security;

import in.bachatsetu.backend.draw.application.exception.DrawAccessDeniedException;
import in.bachatsetu.backend.group.domain.model.SavingsGroup;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

/** Enforces which actor may perform owner-restricted Draw operations. */
public final class DrawAuthorizationService {

    public void requireOwner(SavingsGroup group, AggregateId actorId) {
        Objects.requireNonNull(group, "group must not be null");
        Objects.requireNonNull(actorId, "actor id must not be null");
        if (!group.ownerId().value().equals(actorId)) {
            throw new DrawAccessDeniedException("only the group owner may perform this operation");
        }
    }
}
