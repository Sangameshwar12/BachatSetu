package in.bachatsetu.backend.payment.interfaces.rest.config;

import in.bachatsetu.backend.payment.application.mapper.PaymentApplicationMapper;
import in.bachatsetu.backend.payment.application.port.ClockPort;
import in.bachatsetu.backend.payment.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.payment.application.port.TransactionPort;
import in.bachatsetu.backend.payment.application.service.CreatePaymentApplicationService;
import in.bachatsetu.backend.payment.application.service.GetPaymentApplicationService;
import in.bachatsetu.backend.payment.application.service.ListPaymentsApplicationService;
import in.bachatsetu.backend.payment.application.service.UpdatePaymentStatusApplicationService;
import in.bachatsetu.backend.payment.application.usecase.CreatePaymentUseCase;
import in.bachatsetu.backend.payment.application.usecase.GetPaymentUseCase;
import in.bachatsetu.backend.payment.application.usecase.ListPaymentsUseCase;
import in.bachatsetu.backend.payment.application.usecase.UpdatePaymentStatusUseCase;
import in.bachatsetu.backend.payment.domain.factory.PaymentFactory;
import in.bachatsetu.backend.payment.domain.port.PaymentRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Composes framework-free Payment application services when all outbound ports exist. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBean({
    PaymentRepository.class,
    PaymentFactory.class,
    DomainEventPublisherPort.class,
    ClockPort.class,
    TransactionPort.class
})
public class PaymentApplicationConfig {

    @Bean
    public PaymentApplicationMapper paymentApplicationMapper() {
        return new PaymentApplicationMapper();
    }

    @Bean
    public CreatePaymentUseCase createPaymentUseCase(
            PaymentRepository repository,
            PaymentFactory paymentFactory,
            DomainEventPublisherPort eventPublisher,
            TransactionPort transaction,
            PaymentApplicationMapper mapper) {
        return new CreatePaymentApplicationService(repository, paymentFactory, eventPublisher, transaction, mapper);
    }

    @Bean
    public GetPaymentUseCase getPaymentUseCase(
            PaymentRepository repository,
            TransactionPort transaction,
            PaymentApplicationMapper mapper) {
        return new GetPaymentApplicationService(repository, transaction, mapper);
    }

    @Bean
    public ListPaymentsUseCase listPaymentsUseCase(
            PaymentRepository repository,
            TransactionPort transaction,
            PaymentApplicationMapper mapper) {
        return new ListPaymentsApplicationService(repository, transaction, mapper);
    }

    @Bean
    public UpdatePaymentStatusUseCase updatePaymentStatusUseCase(
            PaymentRepository repository,
            DomainEventPublisherPort eventPublisher,
            ClockPort clock,
            TransactionPort transaction,
            PaymentApplicationMapper mapper) {
        return new UpdatePaymentStatusApplicationService(repository, eventPublisher, clock, transaction, mapper);
    }
}
