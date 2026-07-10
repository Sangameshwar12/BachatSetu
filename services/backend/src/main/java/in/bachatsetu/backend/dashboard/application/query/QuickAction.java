package in.bachatsetu.backend.dashboard.application.query;

import java.util.Objects;

/** A real, callable organizer action — {@code path} is an actual REST route, not a placeholder. */
public record QuickAction(String label, String method, String path) {

    public QuickAction {
        Objects.requireNonNull(label, "label must not be null");
        Objects.requireNonNull(method, "method must not be null");
        Objects.requireNonNull(path, "path must not be null");
    }
}
