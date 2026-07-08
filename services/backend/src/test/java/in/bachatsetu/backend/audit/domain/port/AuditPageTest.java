package in.bachatsetu.backend.audit.domain.port;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class AuditPageTest {

    @Test
    void computesTotalPagesAndNavigationFlags() {
        AuditPage<String> page = new AuditPage<>(List.of("a", "b"), 0, 2, 5);

        assertThat(page.totalPages()).isEqualTo(3);
        assertThat(page.hasNext()).isTrue();
        assertThat(page.hasPrevious()).isFalse();
    }

    @Test
    void reportsNoNextPageOnTheLastPage() {
        AuditPage<String> page = new AuditPage<>(List.of("e"), 2, 2, 5);

        assertThat(page.hasNext()).isFalse();
        assertThat(page.hasPrevious()).isTrue();
    }

    @Test
    void reportsZeroTotalPagesWhenEmpty() {
        AuditPage<String> page = new AuditPage<>(List.of(), 0, 20, 0);

        assertThat(page.totalPages()).isZero();
    }

    @Test
    void rejectsANegativePage() {
        assertThatThrownBy(() -> new AuditPage<>(List.of(), -1, 20, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsANonPositiveSize() {
        assertThatThrownBy(() -> new AuditPage<>(List.of(), 0, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNegativeTotalElements() {
        assertThatThrownBy(() -> new AuditPage<>(List.of(), 0, 20, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
