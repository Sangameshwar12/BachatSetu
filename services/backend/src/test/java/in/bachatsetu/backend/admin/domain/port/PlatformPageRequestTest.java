package in.bachatsetu.backend.admin.domain.port;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PlatformPageRequestTest {

    @Test
    void acceptsAValidRequest() {
        assertThatCode(() -> new PlatformPageRequest(0, 20)).doesNotThrowAnyException();
    }

    @Test
    void rejectsANegativePage() {
        assertThatThrownBy(() -> new PlatformPageRequest(-1, 20)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsASizeBelowOne() {
        assertThatThrownBy(() -> new PlatformPageRequest(0, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsASizeAboveTheMaximum() {
        assertThatThrownBy(() -> new PlatformPageRequest(0, PlatformPageRequest.MAXIMUM_SIZE + 1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
