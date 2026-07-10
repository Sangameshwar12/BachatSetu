package in.bachatsetu.backend.admin.domain.configuration.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LimitKeyTest {

    @Test
    void containsExactlyTheFiveSpecifiedLimits() {
        assertThat(LimitKey.values()).containsExactlyInAnyOrder(
                LimitKey.MAX_GROUPS, LimitKey.MAX_MEMBERS, LimitKey.MAX_UPLOADS, LimitKey.MAX_RECEIPTS,
                LimitKey.MAX_NOTIFICATIONS);
    }
}
