package in.bachatsetu.backend.draw.domain.port;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class DrawPageRequestTest {

    @Test
    void acceptsAValidRequest() {
        DrawPageRequest request = new DrawPageRequest(0, 20, DrawSortField.CREATED_AT, SortDirection.ASC);

        assertThat(request.page()).isZero();
        assertThat(request.size()).isEqualTo(20);
        assertThat(request.sortField()).isEqualTo(DrawSortField.CREATED_AT);
        assertThat(request.direction()).isEqualTo(SortDirection.ASC);
    }

    @Test
    void rejectsNegativePage() {
        assertThatThrownBy(() -> new DrawPageRequest(-1, 20, DrawSortField.SCHEDULED_AT, SortDirection.ASC))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsSizeOutsideTheSupportedRange() {
        assertThatThrownBy(() -> new DrawPageRequest(0, 0, DrawSortField.SCHEDULED_AT, SortDirection.ASC))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DrawPageRequest(
                        0, DrawPageRequest.MAXIMUM_SIZE + 1, DrawSortField.SCHEDULED_AT, SortDirection.ASC))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingSortFieldOrDirection() {
        assertThatThrownBy(() -> new DrawPageRequest(0, 20, null, SortDirection.ASC))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new DrawPageRequest(0, 20, DrawSortField.SCHEDULED_AT, null))
                .isInstanceOf(NullPointerException.class);
    }
}
