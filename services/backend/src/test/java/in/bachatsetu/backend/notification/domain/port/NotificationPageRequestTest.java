package in.bachatsetu.backend.notification.domain.port;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class NotificationPageRequestTest {

    @Test
    void acceptsAValidRequest() {
        NotificationPageRequest request =
                new NotificationPageRequest(0, 20, NotificationSortField.CREATED_AT, SortDirection.ASC);

        assertThat(request.page()).isZero();
        assertThat(request.size()).isEqualTo(20);
        assertThat(request.sortField()).isEqualTo(NotificationSortField.CREATED_AT);
        assertThat(request.direction()).isEqualTo(SortDirection.ASC);
    }

    @Test
    void rejectsNegativePage() {
        assertThatThrownBy(() -> new NotificationPageRequest(-1, 20, NotificationSortField.SCHEDULED_AT, SortDirection.ASC))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsSizeOutsideTheSupportedRange() {
        assertThatThrownBy(() -> new NotificationPageRequest(0, 0, NotificationSortField.SCHEDULED_AT, SortDirection.ASC))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new NotificationPageRequest(
                        0, NotificationPageRequest.MAXIMUM_SIZE + 1, NotificationSortField.SCHEDULED_AT, SortDirection.ASC))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingSortFieldOrDirection() {
        assertThatThrownBy(() -> new NotificationPageRequest(0, 20, null, SortDirection.ASC))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new NotificationPageRequest(0, 20, NotificationSortField.SCHEDULED_AT, null))
                .isInstanceOf(NullPointerException.class);
    }
}
