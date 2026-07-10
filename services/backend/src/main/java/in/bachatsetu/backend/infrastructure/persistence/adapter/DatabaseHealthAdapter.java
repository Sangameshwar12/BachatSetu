package in.bachatsetu.backend.infrastructure.persistence.adapter;

import in.bachatsetu.backend.platformoperations.domain.model.ComponentHealth;
import in.bachatsetu.backend.platformoperations.domain.model.HealthStatus;
import in.bachatsetu.backend.platformoperations.domain.port.DatabaseHealthPort;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import javax.sql.DataSource;
import org.springframework.stereotype.Component;

/** Checks database connectivity directly through the configured {@link DataSource} — no repository call. */
@Component
@ConditionalOnPersistenceRepositories
public class DatabaseHealthAdapter implements DatabaseHealthPort {

    private static final int VALIDATION_TIMEOUT_SECONDS = 2;

    private final DataSource dataSource;

    public DatabaseHealthAdapter(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
    }

    @Override
    public ComponentHealth check() {
        try (Connection connection = dataSource.getConnection()) {
            boolean valid = connection.isValid(VALIDATION_TIMEOUT_SECONDS);
            return new ComponentHealth(
                    "database", valid ? HealthStatus.UP : HealthStatus.DOWN,
                    valid ? "connection validated" : "connection reported invalid");
        } catch (SQLException exception) {
            return new ComponentHealth("database", HealthStatus.DOWN, exception.getMessage());
        }
    }
}
