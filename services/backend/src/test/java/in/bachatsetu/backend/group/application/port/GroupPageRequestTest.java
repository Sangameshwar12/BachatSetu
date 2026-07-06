package in.bachatsetu.backend.group.application.port;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.group.domain.model.GroupStatus;
import org.junit.jupiter.api.Test;

class GroupPageRequestTest {

    @Test
    void acceptsAValidRequestWithoutAStatusFilter() {
        GroupPageRequest request = new GroupPageRequest(0, 20, GroupSortField.CREATED_AT, SortDirection.ASC, null);

        assertThat(request.page()).isZero();
        assertThat(request.size()).isEqualTo(20);
        assertThat(request.statusFilter()).isNull();
    }

    @Test
    void acceptsAnOptionalStatusFilter() {
        GroupPageRequest request = new GroupPageRequest(0, 20, GroupSortField.NAME, SortDirection.DESC, GroupStatus.ACTIVE);

        assertThat(request.statusFilter()).isEqualTo(GroupStatus.ACTIVE);
    }

    @Test
    void rejectsNegativePage() {
        assertThatThrownBy(() -> new GroupPageRequest(-1, 20, GroupSortField.NAME, SortDirection.ASC, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsSizeOutsideTheSupportedRange() {
        assertThatThrownBy(() -> new GroupPageRequest(0, 0, GroupSortField.NAME, SortDirection.ASC, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GroupPageRequest(
                        0, GroupPageRequest.MAXIMUM_SIZE + 1, GroupSortField.NAME, SortDirection.ASC, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingSortFieldOrDirection() {
        assertThatThrownBy(() -> new GroupPageRequest(0, 20, null, SortDirection.ASC, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new GroupPageRequest(0, 20, GroupSortField.NAME, null, null))
                .isInstanceOf(NullPointerException.class);
    }
}
