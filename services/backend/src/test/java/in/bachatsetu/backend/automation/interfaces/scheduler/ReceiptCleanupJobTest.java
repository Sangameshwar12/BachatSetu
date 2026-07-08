package in.bachatsetu.backend.automation.interfaces.scheduler;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.automation.application.query.JobRunResult;
import in.bachatsetu.backend.automation.application.usecase.RunReceiptCleanupUseCase;
import org.junit.jupiter.api.Test;

class ReceiptCleanupJobTest {

    @Test
    void invokesTheUseCaseExactlyOnceWhenTriggeredDirectly() {
        RunReceiptCleanupUseCase useCase = mock(RunReceiptCleanupUseCase.class);
        when(useCase.execute()).thenReturn(JobRunResult.empty());
        ReceiptCleanupJob job = new ReceiptCleanupJob(useCase);

        job.run();

        verify(useCase, times(1)).execute();
    }

    @Test
    void doesNotPropagateAnUnexpectedFailureFromTheUseCase() {
        RunReceiptCleanupUseCase useCase = mock(RunReceiptCleanupUseCase.class);
        when(useCase.execute()).thenThrow(new RuntimeException("unexpected"));
        ReceiptCleanupJob job = new ReceiptCleanupJob(useCase);

        assertThatCode(job::run).doesNotThrowAnyException();
    }

    @Test
    void rejectsNullConstructorArgument() {
        assertThatThrownBy(() -> new ReceiptCleanupJob(null)).isInstanceOf(NullPointerException.class);
    }
}
