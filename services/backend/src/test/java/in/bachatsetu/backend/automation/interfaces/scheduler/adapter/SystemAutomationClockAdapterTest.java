package in.bachatsetu.backend.automation.interfaces.scheduler.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class SystemAutomationClockAdapterTest {

    private static final Instant NOW = Instant.parse("2026-07-08T08:00:00Z");

    @Test
    void delegatesToInjectedJavaClock() {
        SystemAutomationClockAdapter adapter = new SystemAutomationClockAdapter(Clock.fixed(NOW, ZoneOffset.UTC));

        assertThat(adapter.now()).isEqualTo(NOW);
    }

    @Test
    void rejectsNullConstructorArgument() {
        assertThatThrownBy(() -> new SystemAutomationClockAdapter(null)).isInstanceOf(NullPointerException.class);
    }
}
