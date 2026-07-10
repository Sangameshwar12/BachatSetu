package in.bachatsetu.backend.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.platformoperations.domain.model.ComponentHealth;
import in.bachatsetu.backend.platformoperations.domain.model.HealthStatus;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;

class DatabaseHealthAdapterTest {

    @Test
    void reportsUpWhenTheConnectionIsValid() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        try (Connection connection = mock(Connection.class)) {
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.isValid(2)).thenReturn(true);
            DatabaseHealthAdapter adapter = new DatabaseHealthAdapter(dataSource);

            ComponentHealth health = adapter.check();

            assertThat(health.status()).isEqualTo(HealthStatus.UP);
        }
    }

    @Test
    void reportsDownWhenTheConnectionIsInvalid() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        try (Connection connection = mock(Connection.class)) {
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.isValid(2)).thenReturn(false);
            DatabaseHealthAdapter adapter = new DatabaseHealthAdapter(dataSource);

            ComponentHealth health = adapter.check();

            assertThat(health.status()).isEqualTo(HealthStatus.DOWN);
        }
    }

    @Test
    void reportsDownWhenTheConnectionAttemptFails() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenThrow(new SQLException("connection refused"));
        DatabaseHealthAdapter adapter = new DatabaseHealthAdapter(dataSource);

        ComponentHealth health = adapter.check();

        assertThat(health.status()).isEqualTo(HealthStatus.DOWN);
        assertThat(health.detail()).isEqualTo("connection refused");
    }
}
