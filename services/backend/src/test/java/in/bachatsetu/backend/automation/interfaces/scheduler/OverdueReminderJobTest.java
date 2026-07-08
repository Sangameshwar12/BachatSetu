package in.bachatsetu.backend.automation.interfaces.scheduler;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.automation.application.query.JobRunResult;
import in.bachatsetu.backend.automation.application.usecase.RunOverdueReminderUseCase;
import org.junit.jupiter.api.Test;

class OverdueReminderJobTest {

    @Test
    void invokesTheUseCaseExactlyOnceWhenTriggeredDirectly() {
        RunOverdueReminderUseCase useCase = mock(RunOverdueReminderUseCase.class);
        when(useCase.execute()).thenReturn(JobRunResult.empty());
        OverdueReminderJob job = new OverdueReminderJob(useCase);

        job.run();

        verify(useCase, times(1)).execute();
    }

    @Test
    void doesNotPropagateAnUnexpectedFailureFromTheUseCase() {
        RunOverdueReminderUseCase useCase = mock(RunOverdueReminderUseCase.class);
        when(useCase.execute()).thenThrow(new RuntimeException("unexpected"));
        OverdueReminderJob job = new OverdueReminderJob(useCase);

        assertThatCode(job::run).doesNotThrowAnyException();
    }

    @Test
    void rejectsNullConstructorArgument() {
        assertThatThrownBy(() -> new OverdueReminderJob(null)).isInstanceOf(NullPointerException.class);
    }
}
