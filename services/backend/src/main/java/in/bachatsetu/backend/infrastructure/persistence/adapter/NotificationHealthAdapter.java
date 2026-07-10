package in.bachatsetu.backend.infrastructure.persistence.adapter;

import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.NotificationSpringDataRepository;
import in.bachatsetu.backend.platformoperations.domain.model.ComponentHealth;
import in.bachatsetu.backend.platformoperations.domain.model.HealthStatus;
import in.bachatsetu.backend.platformoperations.domain.port.NotificationHealthPort;
import java.util.Objects;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Checks notification-subsystem health with a lightweight count query against its own table. */
@Component
@ConditionalOnPersistenceRepositories
@Transactional(readOnly = true)
public class NotificationHealthAdapter implements NotificationHealthPort {

    private final NotificationSpringDataRepository repository;

    public NotificationHealthAdapter(NotificationSpringDataRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public ComponentHealth check() {
        try {
            repository.countByDeletedFalse();
            return new ComponentHealth("notification", HealthStatus.UP, "notification store reachable");
        } catch (DataAccessException exception) {
            return new ComponentHealth("notification", HealthStatus.DOWN, exception.getMessage());
        }
    }
}
