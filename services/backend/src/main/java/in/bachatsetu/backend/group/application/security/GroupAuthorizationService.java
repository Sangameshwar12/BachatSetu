package in.bachatsetu.backend.group.application.security;

import in.bachatsetu.backend.group.application.exception.GroupAccessDeniedException;
import in.bachatsetu.backend.group.domain.model.SavingsGroup;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

/** Enforces which actor may perform owner-restricted Savings Group operations. */
public final class GroupAuthorizationService {

    public void requireOwner(SavingsGroup group, AggregateId actorId) {
        Objects.requireNonNull(group, "group must not be null");
        Objects.requireNonNull(actorId, "actor id must not be null");
        if (!group.ownerId().value().equals(actorId)) {
            throw new GroupAccessDeniedException("only the group owner may perform this operation");
        }
    }
}
