package in.bachatsetu.backend.user.domain.model;

import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Objects;

public final class ContactPreference {

    private final AggregateId id;
    private final ContactChannel channel;
    private boolean enabled;

    public ContactPreference(AggregateId id, ContactChannel channel, boolean enabled) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.channel = Objects.requireNonNull(channel, "channel must not be null");
        this.enabled = enabled;
    }

    public void enable() {
        enabled = true;
    }

    public void disable() {
        enabled = false;
    }

    public AggregateId id() {
        return id;
    }

    public ContactChannel channel() {
        return channel;
    }

    public boolean enabled() {
        return enabled;
    }
}
