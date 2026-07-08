package in.bachatsetu.backend.audit.application.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.audit.application.query.AuditEntryResult;
import in.bachatsetu.backend.audit.domain.model.AuditEntry;
import in.bachatsetu.backend.audit.domain.model.AuditEventType;
import in.bachatsetu.backend.audit.domain.port.AuditPage;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class AuditApplicationMapperTest {

    private static final Instant NOW = Instant.parse("2026-07-08T08:00:00Z");

    private final AuditApplicationMapper mapper = new AuditApplicationMapper();

    @Test
    void mapsAnEntryToAResultPreservingEveryField() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId actorId = AggregateId.newId();
        AggregateId resourceId = AggregateId.newId();
        AuditEntry entry = AuditEntry.record(
                AggregateId.newId(), tenantId, actorId, AuditEventType.LOGIN, "auth", "User", resourceId, "LOGIN",
                "signed in", "127.0.0.1", "agent", "{}", NOW);

        AuditEntryResult result = mapper.toResult(entry);

        assertThat(result.auditId()).isEqualTo(entry.id().value());
        assertThat(result.tenantId()).isEqualTo(tenantId.value());
        assertThat(result.actorId()).isEqualTo(actorId.value());
        assertThat(result.eventType()).isEqualTo(AuditEventType.LOGIN);
        assertThat(result.moduleName()).isEqualTo("auth");
        assertThat(result.resourceType()).isEqualTo("User");
        assertThat(result.resourceId()).isEqualTo(resourceId.value());
        assertThat(result.action()).isEqualTo("LOGIN");
        assertThat(result.description()).isEqualTo("signed in");
        assertThat(result.ipAddress()).isEqualTo("127.0.0.1");
        assertThat(result.userAgent()).isEqualTo("agent");
        assertThat(result.metadata()).isEqualTo("{}");
        assertThat(result.createdAt()).isEqualTo(NOW);
    }

    @Test
    void mapsNullableTenantAndActorAndResourceToNull() {
        AuditEntry entry = AuditEntry.record(
                AggregateId.newId(), null, null, AuditEventType.SYSTEM_EVENT, "automation", null, null,
                "SYSTEM_EVENT", null, null, null, null, NOW);

        AuditEntryResult result = mapper.toResult(entry);

        assertThat(result.tenantId()).isNull();
        assertThat(result.actorId()).isNull();
        assertThat(result.resourceId()).isNull();
    }

    @Test
    void mapsAPageOfEntriesPreservingPagingMetadata() {
        AuditEntry entry = AuditEntry.record(
                AggregateId.newId(), AggregateId.newId(), AggregateId.newId(), AuditEventType.LOGIN, "auth", null,
                null, "LOGIN", null, null, null, null, NOW);
        AuditPage<AuditEntry> page = new AuditPage<>(List.of(entry), 0, 20, 1);

        AuditPage<AuditEntryResult> result = mapper.toResultPage(page);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).auditId()).isEqualTo(entry.id().value());
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void rejectsANullEntry() {
        assertThatThrownBy(() -> mapper.toResult(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsANullPage() {
        assertThatThrownBy(() -> mapper.toResultPage(null)).isInstanceOf(NullPointerException.class);
    }
}
