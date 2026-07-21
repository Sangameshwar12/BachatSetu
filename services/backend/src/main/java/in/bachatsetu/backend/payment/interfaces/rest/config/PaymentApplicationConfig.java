package in.bachatsetu.backend.payment.interfaces.rest.config;

import in.bachatsetu.backend.audit.application.usecase.CreateAuditEntryUseCase;
import in.bachatsetu.backend.group.application.port.SavingsGroupRepository;
import in.bachatsetu.backend.payment.application.mapper.PaymentApplicationMapper;
import in.bachatsetu.backend.payment.application.port.ClockPort;
import in.bachatsetu.backend.payment.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.payment.application.port.TransactionPort;
import in.bachatsetu.backend.payment.application.service.CreatePaymentApplicationService;
import in.bachatsetu.backend.payment.application.service.GetCollectionSummaryApplicationService;
import in.bachatsetu.backend.payment.application.service.GetPaymentApplicationService;
import in.bachatsetu.backend.payment.application.service.ListPaymentsApplicationService;
import in.bachatsetu.backend.payment.application.service.RecordManualPaymentApplicationService;
import in.bachatsetu.backend.payment.application.service.UpdatePaymentStatusApplicationService;
import in.bachatsetu.backend.payment.application.usecase.CreatePaymentUseCase;
import in.bachatsetu.backend.payment.application.usecase.GetCollectionSummaryUseCase;
import in.bachatsetu.backend.payment.application.usecase.GetPaymentUseCase;
import in.bachatsetu.backend.payment.application.usecase.ListPaymentsUseCase;
import in.bachatsetu.backend.payment.application.usecase.RecordManualPaymentUseCase;
import in.bachatsetu.backend.payment.application.usecase.UpdatePaymentStatusUseCase;
import in.bachatsetu.backend.payment.domain.factory.PaymentFactory;
import in.bachatsetu.backend.payment.domain.port.PaymentRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composes framework-free Payment application services when all outbound ports exist.
 *
 * <p>Gated on {@code bachatsetu.persistence.repositories.enabled} rather than a
 * cross-configuration-class {@code @ConditionalOnBean} check: regular (non-auto-configuration)
 * {@code @Configuration} classes discovered by component scanning have no guaranteed processing
 * order relative to one another, so a class-level {@code @ConditionalOnBean} referencing ports
 * defined by {@code PaymentInfrastructureConfig} was evaluated non-deterministically and could
 * skip this configuration even when every required port was actually present.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "bachatsetu.persistence.repositories",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
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
            PaymentApplicationMapper mapper,
            CreateAuditEntryUseCase createAuditEntry) {
        return new UpdatePaymentStatusApplicationService(
                repository, eventPublisher, clock, transaction, mapper, createAuditEntry);
    }

    @Bean
    public GetCollectionSummaryUseCase getCollectionSummaryUseCase(
            SavingsGroupRepository groupRepository,
            PaymentRepository repository,
            ClockPort clock,
            TransactionPort transaction) {
        return new GetCollectionSummaryApplicationService(groupRepository, repository, clock, transaction);
    }

    @Bean
    public RecordManualPaymentUseCase recordManualPaymentUseCase(
            SavingsGroupRepository groupRepository,
            PaymentRepository repository,
            PaymentFactory paymentFactory,
            DomainEventPublisherPort eventPublisher,
            ClockPort clock,
            TransactionPort transaction,
            PaymentApplicationMapper mapper) {
        return new RecordManualPaymentApplicationService(
                groupRepository, repository, paymentFactory, eventPublisher, clock, transaction, mapper);
    }
}
