package in.bachatsetu.backend.admin.domain.port;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.admin.domain.model.PlatformGroupStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PlatformGroupSearchCriteriaTest {

    private static final Instant NOW = Instant.parse("2026-07-08T08:00:00Z");

    @Test
    void acceptsEveryFilterOmitted() {
        assertThatCode(() -> new PlatformGroupSearchCriteria(null, null, null, 0, 20, SortDirection.DESC))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsANegativePage() {
        assertThatThrownBy(() -> new PlatformGroupSearchCriteria(
                        PlatformGroupStatus.ACTIVE, null, null, -1, 20, SortDirection.DESC))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsASizeAboveTheMaximum() {
        assertThatThrownBy(() -> new PlatformGroupSearchCriteria(
                        null, null, null, 0, PlatformGroupSearchCriteria.MAXIMUM_SIZE + 1, SortDirection.DESC))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsACreatedAfterLaterThanCreatedBefore() {
        assertThatThrownBy(() -> new PlatformGroupSearchCriteria(
                        null, NOW, NOW.minusSeconds(60), 0, 20, SortDirection.DESC))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsANullDirection() {
        assertThatThrownBy(() -> new PlatformGroupSearchCriteria(null, null, null, 0, 20, null))
                .isInstanceOf(NullPointerException.class);
    }
}
