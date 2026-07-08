package in.bachatsetu.backend.automation.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class JobRunResultTest {

    @Test
    void emptyHasZeroForEveryCount() {
        JobRunResult empty = JobRunResult.empty();

        assertThat(empty).isEqualTo(new JobRunResult(0, 0, List.of()));
        assertThat(empty.failedCount()).isZero();
    }

    @Test
    void derivesFailedCountFromTheFailureMessageList() {
        JobRunResult result = new JobRunResult(1, 2, List.of("boom", "also boom"));

        assertThat(result.failedCount()).isEqualTo(2);
    }

    @Test
    void rejectsNegativeCounts() {
        assertThatThrownBy(() -> new JobRunResult(-1, 0, List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new JobRunResult(0, -1, List.of())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullFailureMessageList() {
        assertThatThrownBy(() -> new JobRunResult(0, 0, null)).isInstanceOf(NullPointerException.class);
    }
}
