package in.bachatsetu.backend.member.domain.port;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class MemberPageRequestTest {

    @Test
    void acceptsAValidRequest() {
        MemberPageRequest request = new MemberPageRequest(0, 20, MemberSortField.CREATED_AT, SortDirection.ASC);

        assertThat(request.page()).isZero();
        assertThat(request.size()).isEqualTo(20);
        assertThat(request.sortField()).isEqualTo(MemberSortField.CREATED_AT);
        assertThat(request.direction()).isEqualTo(SortDirection.ASC);
    }

    @Test
    void rejectsNegativePage() {
        assertThatThrownBy(() -> new MemberPageRequest(-1, 20, MemberSortField.MEMBER_NUMBER, SortDirection.ASC))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsSizeOutsideTheSupportedRange() {
        assertThatThrownBy(() -> new MemberPageRequest(0, 0, MemberSortField.MEMBER_NUMBER, SortDirection.ASC))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MemberPageRequest(
                        0, MemberPageRequest.MAXIMUM_SIZE + 1, MemberSortField.MEMBER_NUMBER, SortDirection.ASC))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingSortFieldOrDirection() {
        assertThatThrownBy(() -> new MemberPageRequest(0, 20, null, SortDirection.ASC))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new MemberPageRequest(0, 20, MemberSortField.MEMBER_NUMBER, null))
                .isInstanceOf(NullPointerException.class);
    }
}
