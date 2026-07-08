package in.bachatsetu.backend.audit.domain.port;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.audit.domain.model.AuditEventType;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class AuditSearchCriteriaTest {

    private static final Instant NOW = Instant.parse("2026-07-08T08:00:00Z");

    @Test
    void acceptsEveryFilterOmitted() {
        assertThatCode(() -> new AuditSearchCriteria(
                        AggregateId.newId(), null, null, null, null, null, 0, 20, AuditSortField.CREATED_AT,
                        SortDirection.DESC))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsANegativePage() {
        assertThatThrownBy(() -> new AuditSearchCriteria(
                        AggregateId.newId(), null, null, null, null, null, -1, 20, AuditSortField.CREATED_AT,
                        SortDirection.DESC))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsASizeBelowOne() {
        assertThatThrownBy(() -> new AuditSearchCriteria(
                        AggregateId.newId(), null, null, null, null, null, 0, 0, AuditSortField.CREATED_AT,
                        SortDirection.DESC))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsASizeAboveTheMaximum() {
        assertThatThrownBy(() -> new AuditSearchCriteria(
                        AggregateId.newId(), null, null, null, null, null, 0, AuditSearchCriteria.MAXIMUM_SIZE + 1,
                        AuditSortField.CREATED_AT, SortDirection.DESC))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsADateFromAfterDateTo() {
        assertThatThrownBy(() -> new AuditSearchCriteria(
                        AggregateId.newId(), null, null, AuditEventType.LOGIN, NOW, NOW.minusSeconds(60), 0, 20,
                        AuditSortField.CREATED_AT, SortDirection.DESC))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsANullSortField() {
        assertThatThrownBy(() -> new AuditSearchCriteria(
                        AggregateId.newId(), null, null, null, null, null, 0, 20, null, SortDirection.DESC))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsANullDirection() {
        assertThatThrownBy(() -> new AuditSearchCriteria(
                        AggregateId.newId(), null, null, null, null, null, 0, 20, AuditSortField.CREATED_AT, null))
                .isInstanceOf(NullPointerException.class);
    }
}
