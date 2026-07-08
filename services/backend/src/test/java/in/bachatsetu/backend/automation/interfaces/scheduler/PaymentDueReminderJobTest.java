package in.bachatsetu.backend.automation.interfaces.scheduler;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.automation.application.query.JobRunResult;
import in.bachatsetu.backend.automation.application.usecase.RunPaymentDueReminderUseCase;
import org.junit.jupiter.api.Test;

class PaymentDueReminderJobTest {

    @Test
    void invokesTheUseCaseExactlyOnceWhenTriggeredDirectly() {
        RunPaymentDueReminderUseCase useCase = mock(RunPaymentDueReminderUseCase.class);
        when(useCase.execute()).thenReturn(JobRunResult.empty());
        PaymentDueReminderJob job = new PaymentDueReminderJob(useCase);

        job.run();

        verify(useCase, times(1)).execute();
    }

    @Test
    void doesNotPropagateAnUnexpectedFailureFromTheUseCase() {
        RunPaymentDueReminderUseCase useCase = mock(RunPaymentDueReminderUseCase.class);
        when(useCase.execute()).thenThrow(new RuntimeException("unexpected"));
        PaymentDueReminderJob job = new PaymentDueReminderJob(useCase);

        assertThatCode(job::run).doesNotThrowAnyException();
    }

    @Test
    void rejectsNullConstructorArgument() {
        assertThatThrownBy(() -> new PaymentDueReminderJob(null)).isInstanceOf(NullPointerException.class);
    }
}
