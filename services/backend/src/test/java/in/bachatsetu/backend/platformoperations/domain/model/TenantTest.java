package in.bachatsetu.backend.platformoperations.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.platformoperations.domain.exception.PlatformOperationsDomainException;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class TenantTest {

    private static final Instant NOW = Instant.parse("2026-07-10T08:00:00Z");

    @Test
    void createsAnActiveTenant() {
        Tenant tenant = Tenant.createActive(AggregateId.newId(), AggregateId.newId(), NOW);

        assertThat(tenant.status()).isEqualTo(TenantStatus.ACTIVE);
        assertThat(tenant.suspensionReason()).isNull();
    }

    @Test
    void suspendsAnActiveTenant() {
        Tenant tenant = Tenant.createActive(AggregateId.newId(), AggregateId.newId(), NOW);
        AggregateId admin = AggregateId.newId();

        tenant.suspend("Fraud investigation", admin, NOW.plusSeconds(60));

        assertThat(tenant.status()).isEqualTo(TenantStatus.SUSPENDED);
        assertThat(tenant.suspensionReason()).isEqualTo("Fraud investigation");
        assertThat(tenant.domainEvents()).hasSize(1);
    }

    @Test
    void rejectsSuspendingAnAlreadySuspendedTenant() {
        Tenant tenant = Tenant.createActive(AggregateId.newId(), AggregateId.newId(), NOW);
        AggregateId admin = AggregateId.newId();
        tenant.suspend("Reason", admin, NOW.plusSeconds(60));

        assertThatThrownBy(() -> tenant.suspend("Again", admin, NOW.plusSeconds(120)))
                .isInstanceOf(PlatformOperationsDomainException.class);
    }

    @Test
    void activatesASuspendedTenant() {
        Tenant tenant = Tenant.createActive(AggregateId.newId(), AggregateId.newId(), NOW);
        AggregateId admin = AggregateId.newId();
        tenant.suspend("Reason", admin, NOW.plusSeconds(60));

        tenant.activate(admin, NOW.plusSeconds(120));

        assertThat(tenant.status()).isEqualTo(TenantStatus.ACTIVE);
        assertThat(tenant.suspensionReason()).isNull();
    }

    @Test
    void rejectsActivatingAnActiveTenant() {
        Tenant tenant = Tenant.createActive(AggregateId.newId(), AggregateId.newId(), NOW);
        AggregateId admin = AggregateId.newId();

        assertThatThrownBy(() -> tenant.activate(admin, NOW.plusSeconds(60)))
                .isInstanceOf(PlatformOperationsDomainException.class);
    }

    @Test
    void archivesAnActiveTenant() {
        Tenant tenant = Tenant.createActive(AggregateId.newId(), AggregateId.newId(), NOW);
        AggregateId admin = AggregateId.newId();

        tenant.archive(admin, NOW.plusSeconds(60));

        assertThat(tenant.status()).isEqualTo(TenantStatus.ARCHIVED);
    }

    @Test
    void rejectsArchivingAnAlreadyArchivedTenant() {
        Tenant tenant = Tenant.createActive(AggregateId.newId(), AggregateId.newId(), NOW);
        AggregateId admin = AggregateId.newId();
        tenant.archive(admin, NOW.plusSeconds(60));

        assertThatThrownBy(() -> tenant.archive(admin, NOW.plusSeconds(120)))
                .isInstanceOf(PlatformOperationsDomainException.class);
    }
}
