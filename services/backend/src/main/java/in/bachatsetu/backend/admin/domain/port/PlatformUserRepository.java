package in.bachatsetu.backend.admin.domain.port;

import in.bachatsetu.backend.admin.domain.model.PlatformUserStatus;
import in.bachatsetu.backend.admin.domain.model.PlatformUserSummary;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.Optional;

/**
 * Cross-tenant read (and status-mutation) access to platform users. Backed by the same underlying user
 * table the Auth and Profile modules already use, without depending on either module's repository — which
 * are both scoped to the caller's own tenant and therefore unusable for a platform-wide view.
 */
public interface PlatformUserRepository {

    PlatformPage<PlatformUserSummary> search(PlatformUserSearchCriteria criteria);

    Optional<PlatformUserSummary> findById(AggregateId userId);

    /** @return {@code true} if a user with {@code userId} was found and updated. */
    boolean updateStatus(AggregateId userId, PlatformUserStatus status, AggregateId administratorId, Instant at);
}
