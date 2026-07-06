package in.bachatsetu.backend.group.application.port;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class GroupPageTest {

    @Test
    void derivesTotalPagesAndNavigationFlagsFromASingleSourceOfTruth() {
        GroupPage<String> firstOfThree = new GroupPage<>(List.of("a", "b"), 0, 2, 5);
        assertThat(firstOfThree.totalPages()).isEqualTo(3);
        assertThat(firstOfThree.hasNext()).isTrue();
        assertThat(firstOfThree.hasPrevious()).isFalse();

        GroupPage<String> lastOfThree = new GroupPage<>(List.of("e"), 2, 2, 5);
        assertThat(lastOfThree.totalPages()).isEqualTo(3);
        assertThat(lastOfThree.hasNext()).isFalse();
        assertThat(lastOfThree.hasPrevious()).isTrue();
    }

    @Test
    void reportsZeroTotalPagesAndNoNavigationForAnEmptyResult() {
        GroupPage<String> empty = new GroupPage<>(List.of(), 0, 20, 0);

        assertThat(empty.totalPages()).isZero();
        assertThat(empty.hasNext()).isFalse();
        assertThat(empty.hasPrevious()).isFalse();
    }

    @Test
    void contentIsImmutable() {
        GroupPage<String> page = new GroupPage<>(List.of("a"), 0, 20, 1);

        assertThatThrownBy(() -> page.content().add("b")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsInvalidConstructionArguments() {
        assertThatThrownBy(() -> new GroupPage<>(null, 0, 20, 0)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new GroupPage<>(List.of(), -1, 20, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GroupPage<>(List.of(), 0, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GroupPage<>(List.of(), 0, 20, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
