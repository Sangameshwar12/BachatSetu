package in.bachatsetu.backend.admin.application.analytics.command;

import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

/**
 * Requests one analytics view. {@code administratorId} is the acting platform administrator, carried only so
 * the best-effort {@code ADMIN_ANALYTICS_VIEWED} audit entry can record who viewed it — analytics themselves
 * never filter or scope by administrator.
 */
public record ViewAnalyticsCommand(AggregateId administratorId) {

    public ViewAnalyticsCommand {
        Objects.requireNonNull(administratorId, "administratorId must not be null");
    }
}
