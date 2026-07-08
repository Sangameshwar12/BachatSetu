package in.bachatsetu.backend.automation.interfaces.scheduler;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.automation.application.query.JobRunResult;
import in.bachatsetu.backend.automation.application.usecase.RunDrawSchedulerUseCase;
import java.util.List;
import org.junit.jupiter.api.Test;

class DrawSchedulerJobTest {

    @Test
    void invokesTheUseCaseExactlyOnceWhenTriggeredDirectly() {
        RunDrawSchedulerUseCase useCase = mock(RunDrawSchedulerUseCase.class);
        when(useCase.execute()).thenReturn(new JobRunResult(2, 0, List.of()));
        DrawSchedulerJob job = new DrawSchedulerJob(useCase);

        job.run();

        verify(useCase, times(1)).execute();
    }

    @Test
    void doesNotPropagateAnUnexpectedFailureFromTheUseCase() {
        RunDrawSchedulerUseCase useCase = mock(RunDrawSchedulerUseCase.class);
        when(useCase.execute()).thenThrow(new RuntimeException("unexpected"));
        DrawSchedulerJob job = new DrawSchedulerJob(useCase);

        assertThatCode(job::run).doesNotThrowAnyException();
    }

    @Test
    void rejectsNullConstructorArgument() {
        assertThatThrownBy(() -> new DrawSchedulerJob(null)).isInstanceOf(NullPointerException.class);
    }
}
