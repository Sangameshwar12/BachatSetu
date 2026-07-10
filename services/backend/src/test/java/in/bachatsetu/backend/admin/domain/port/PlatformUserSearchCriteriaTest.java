package in.bachatsetu.backend.admin.domain.port;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.admin.domain.model.PlatformUserStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PlatformUserSearchCriteriaTest {

    private static final Instant NOW = Instant.parse("2026-07-08T08:00:00Z");

    @Test
    void acceptsEveryFilterOmitted() {
        assertThatCode(() -> new PlatformUserSearchCriteria(
                        null, null, null, null, null, 0, 20, PlatformUserSortField.CREATED_AT, SortDirection.DESC))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsANegativePage() {
        assertThatThrownBy(() -> new PlatformUserSearchCriteria(
                        PlatformUserStatus.ACTIVE, null, null, null, null, -1, 20, PlatformUserSortField.CREATED_AT,
                        SortDirection.DESC))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsASizeBelowOne() {
        assertThatThrownBy(() -> new PlatformUserSearchCriteria(
                        null, null, null, null, null, 0, 0, PlatformUserSortField.CREATED_AT, SortDirection.DESC))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsASizeAboveTheMaximum() {
        assertThatThrownBy(() -> new PlatformUserSearchCriteria(
                        null, null, null, null, null, 0, PlatformUserSearchCriteria.MAXIMUM_SIZE + 1,
                        PlatformUserSortField.CREATED_AT, SortDirection.DESC))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsACreatedAfterLaterThanCreatedBefore() {
        assertThatThrownBy(() -> new PlatformUserSearchCriteria(
                        null, null, null, NOW, NOW.minusSeconds(60), 0, 20, PlatformUserSortField.CREATED_AT,
                        SortDirection.DESC))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsANullSortField() {
        assertThatThrownBy(() -> new PlatformUserSearchCriteria(
                        null, null, null, null, null, 0, 20, null, SortDirection.DESC))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsANullDirection() {
        assertThatThrownBy(() -> new PlatformUserSearchCriteria(
                        null, null, null, null, null, 0, 20, PlatformUserSortField.CREATED_AT, null))
                .isInstanceOf(NullPointerException.class);
    }
}
