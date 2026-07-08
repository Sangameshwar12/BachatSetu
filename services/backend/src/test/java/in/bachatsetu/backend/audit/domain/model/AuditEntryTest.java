package in.bachatsetu.backend.audit.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class AuditEntryTest {

    private static final Instant NOW = Instant.parse("2026-07-08T08:00:00Z");

    @Test
    void recordsAnEntryWithEveryFieldPreserved() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId actorId = AggregateId.newId();
        AggregateId resourceId = AggregateId.newId();

        AuditEntry entry = AuditEntry.record(
                AggregateId.newId(), tenantId, actorId, AuditEventType.PAYMENT_VERIFIED, "payment", "Payment",
                resourceId, "PAYMENT_VERIFIED", "payment verified", "127.0.0.1", "test-agent", "{\"k\":\"v\"}",
                NOW);

        assertThat(entry.tenantId()).isEqualTo(tenantId);
        assertThat(entry.actorId()).isEqualTo(actorId);
        assertThat(entry.eventType()).isEqualTo(AuditEventType.PAYMENT_VERIFIED);
        assertThat(entry.moduleName()).isEqualTo("payment");
        assertThat(entry.resourceType()).isEqualTo("Payment");
        assertThat(entry.resourceId()).isEqualTo(resourceId);
        assertThat(entry.action()).isEqualTo("PAYMENT_VERIFIED");
        assertThat(entry.description()).isEqualTo("payment verified");
        assertThat(entry.ipAddress()).isEqualTo("127.0.0.1");
        assertThat(entry.userAgent()).isEqualTo("test-agent");
        assertThat(entry.metadata()).isEqualTo("{\"k\":\"v\"}");
        assertThat(entry.createdAt()).isEqualTo(NOW);
        assertThat(entry.auditInfo().createdBy()).isEqualTo(actorId);
    }

    @Test
    void allowsANullTenantForATenantLessEvent() {
        AuditEntry entry = AuditEntry.record(
                AggregateId.newId(), null, AggregateId.newId(), AuditEventType.LOGIN, "auth", "User",
                AggregateId.newId(), "LOGIN", null, null, null, null, NOW);

        assertThat(entry.tenantId()).isNull();
    }

    @Test
    void allowsANullActorForASystemEventAndUsesAPlaceholderForAuditInfo() {
        AuditEntry entry = AuditEntry.record(
                AggregateId.newId(), AggregateId.newId(), null, AuditEventType.SYSTEM_EVENT, "automation", null,
                null, "SYSTEM_EVENT", null, null, null, null, NOW);

        assertThat(entry.actorId()).isNull();
        assertThat(entry.auditInfo().createdBy()).isNotNull();
    }

    @Test
    void rejectsABlankModuleName() {
        assertThatThrownBy(() -> AuditEntry.record(
                        AggregateId.newId(), AggregateId.newId(), AggregateId.newId(), AuditEventType.LOGIN, " ",
                        null, null, "LOGIN", null, null, null, null, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsABlankAction() {
        assertThatThrownBy(() -> AuditEntry.record(
                        AggregateId.newId(), AggregateId.newId(), AggregateId.newId(), AuditEventType.LOGIN, "auth",
                        null, null, " ", null, null, null, null, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsANullEventType() {
        assertThatThrownBy(() -> AuditEntry.record(
                        AggregateId.newId(), AggregateId.newId(), AggregateId.newId(), null, "auth", null, null,
                        "LOGIN", null, null, null, null, NOW))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsANullCreatedAt() {
        assertThatThrownBy(() -> AuditEntry.record(
                        AggregateId.newId(), AggregateId.newId(), AggregateId.newId(), AuditEventType.LOGIN, "auth",
                        null, null, "LOGIN", null, null, null, null, null))
                .isInstanceOf(NullPointerException.class);
    }
}
