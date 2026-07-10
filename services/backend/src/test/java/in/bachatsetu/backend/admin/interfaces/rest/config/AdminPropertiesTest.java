package in.bachatsetu.backend.admin.interfaces.rest.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AdminPropertiesTest {

    @Test
    void bindsEveryField() {
        AdminProperties properties = new AdminProperties(true, 20, 100);

        assertThat(properties.enabled()).isTrue();
        assertThat(properties.pageSizeDefault()).isEqualTo(20);
        assertThat(properties.pageSizeMax()).isEqualTo(100);
    }

    @Test
    void rejectsANonPositiveDefaultPageSize() {
        assertThatThrownBy(() -> new AdminProperties(true, 0, 100)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsAMaximumSmallerThanTheDefault() {
        assertThatThrownBy(() -> new AdminProperties(true, 50, 20)).isInstanceOf(IllegalArgumentException.class);
    }
}
