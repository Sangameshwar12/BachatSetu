package in.bachatsetu.backend.payment.interfaces.rest.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import in.bachatsetu.backend.audit.application.usecase.CreateAuditEntryUseCase;
import in.bachatsetu.backend.payment.application.mapper.PaymentApplicationMapper;
import in.bachatsetu.backend.payment.application.port.ClockPort;
import in.bachatsetu.backend.payment.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.payment.application.port.TransactionPort;
import in.bachatsetu.backend.payment.application.service.CreatePaymentApplicationService;
import in.bachatsetu.backend.payment.application.service.GetPaymentApplicationService;
import in.bachatsetu.backend.payment.application.service.ListPaymentsApplicationService;
import in.bachatsetu.backend.payment.application.service.UpdatePaymentStatusApplicationService;
import in.bachatsetu.backend.payment.domain.factory.PaymentFactory;
import in.bachatsetu.backend.payment.domain.port.PaymentRepository;
import java.time.Clock;
import org.junit.jupiter.api.Test;

class PaymentApplicationConfigTest {

    private final PaymentApplicationConfig config = new PaymentApplicationConfig();
    private final PaymentApplicationMapper mapper = config.paymentApplicationMapper();
    private final PaymentRepository repository = mock(PaymentRepository.class);
    private final PaymentFactory paymentFactory = new PaymentFactory(Clock.systemUTC());
    private final DomainEventPublisherPort eventPublisher = mock(DomainEventPublisherPort.class);
    private final ClockPort clock = mock(ClockPort.class);
    private final TransactionPort transaction = mock(TransactionPort.class);
    private final CreateAuditEntryUseCase createAuditEntry = mock(CreateAuditEntryUseCase.class);

    @Test
    void composesCreatePaymentUseCase() {
        assertThat(config.createPaymentUseCase(repository, paymentFactory, eventPublisher, transaction, mapper))
                .isInstanceOf(CreatePaymentApplicationService.class);
    }

    @Test
    void composesGetPaymentUseCase() {
        assertThat(config.getPaymentUseCase(repository, transaction, mapper))
                .isInstanceOf(GetPaymentApplicationService.class);
    }

    @Test
    void composesListPaymentsUseCase() {
        assertThat(config.listPaymentsUseCase(repository, transaction, mapper))
                .isInstanceOf(ListPaymentsApplicationService.class);
    }

    @Test
    void composesUpdatePaymentStatusUseCase() {
        assertThat(config.updatePaymentStatusUseCase(
                        repository, eventPublisher, clock, transaction, mapper, createAuditEntry))
                .isInstanceOf(UpdatePaymentStatusApplicationService.class);
    }
}
