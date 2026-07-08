package in.bachatsetu.backend.automation.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import in.bachatsetu.backend.automation.application.query.JobRunResult;
import org.junit.jupiter.api.Test;

class ReceiptCleanupApplicationServiceTest {

    @Test
    void isADeliberateNoOpBecauseNoReceiptPdfCacheExists() {
        ReceiptCleanupApplicationService service = new ReceiptCleanupApplicationService();

        JobRunResult result = service.execute();

        assertThat(result).isEqualTo(JobRunResult.empty());
    }

    @Test
    void isSafeToRunRepeatedly() {
        ReceiptCleanupApplicationService service = new ReceiptCleanupApplicationService();

        JobRunResult first = service.execute();
        JobRunResult second = service.execute();

        assertThat(first).isEqualTo(second);
    }
}
