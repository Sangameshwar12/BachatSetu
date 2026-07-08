package in.bachatsetu.backend.storage.interfaces.rest.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

class StorageInfrastructureAdapterTest {

    private static final Instant NOW = Instant.parse("2026-07-08T08:00:00Z");

    @Test
    void systemClockDelegatesToInjectedJavaClock() {
        SystemStorageClockAdapter adapter = new SystemStorageClockAdapter(Clock.fixed(NOW, ZoneOffset.UTC));

        assertThat(adapter.now()).isEqualTo(NOW);
    }

    @Test
    void transactionAdapterExecutesOperationThroughTransactionTemplate() {
        TransactionTemplate template = mock(TransactionTemplate.class);
        when(template.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<String> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
        SpringStorageTransactionAdapter adapter = new SpringStorageTransactionAdapter(template);

        String result = adapter.execute(() -> "completed");

        assertThat(result).isEqualTo("completed");
        verify(template, times(1)).execute(any());
    }
}
