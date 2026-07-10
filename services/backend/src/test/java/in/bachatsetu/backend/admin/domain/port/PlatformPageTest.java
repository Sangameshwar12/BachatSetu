package in.bachatsetu.backend.admin.domain.port;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class PlatformPageTest {

    @Test
    void computesTotalPagesAndNavigationFlags() {
        PlatformPage<String> page = new PlatformPage<>(List.of("a", "b"), 0, 2, 5);

        assertThat(page.totalPages()).isEqualTo(3);
        assertThat(page.hasNext()).isTrue();
        assertThat(page.hasPrevious()).isFalse();
    }

    @Test
    void reportsZeroTotalPagesWhenEmpty() {
        assertThat(new PlatformPage<>(List.of(), 0, 20, 0).totalPages()).isZero();
    }

    @Test
    void rejectsANegativePage() {
        assertThatThrownBy(() -> new PlatformPage<>(List.of(), -1, 20, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsANonPositiveSize() {
        assertThatThrownBy(() -> new PlatformPage<>(List.of(), 0, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNegativeTotalElements() {
        assertThatThrownBy(() -> new PlatformPage<>(List.of(), 0, 20, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
