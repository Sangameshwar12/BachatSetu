package in.bachatsetu.backend.admin.interfaces.rest.config;

import in.bachatsetu.backend.admin.application.analytics.mapper.AnalyticsApplicationMapper;
import in.bachatsetu.backend.admin.application.analytics.service.GetGroupAnalyticsApplicationService;
import in.bachatsetu.backend.admin.application.analytics.service.GetNotificationAnalyticsApplicationService;
import in.bachatsetu.backend.admin.application.analytics.service.GetOverviewAnalyticsApplicationService;
import in.bachatsetu.backend.admin.application.analytics.service.GetPaymentAnalyticsApplicationService;
import in.bachatsetu.backend.admin.application.analytics.service.GetStorageAnalyticsApplicationService;
import in.bachatsetu.backend.admin.application.analytics.service.GetUserAnalyticsApplicationService;
import in.bachatsetu.backend.admin.application.analytics.usecase.GetGroupAnalyticsUseCase;
import in.bachatsetu.backend.admin.application.analytics.usecase.GetNotificationAnalyticsUseCase;
import in.bachatsetu.backend.admin.application.analytics.usecase.GetOverviewAnalyticsUseCase;
import in.bachatsetu.backend.admin.application.analytics.usecase.GetPaymentAnalyticsUseCase;
import in.bachatsetu.backend.admin.application.analytics.usecase.GetStorageAnalyticsUseCase;
import in.bachatsetu.backend.admin.application.analytics.usecase.GetUserAnalyticsUseCase;
import in.bachatsetu.backend.admin.application.port.TransactionPort;
import in.bachatsetu.backend.admin.domain.analytics.port.GroupAnalyticsRepository;
import in.bachatsetu.backend.admin.domain.analytics.port.NotificationAnalyticsRepository;
import in.bachatsetu.backend.admin.domain.analytics.port.OverviewAnalyticsRepository;
import in.bachatsetu.backend.admin.domain.analytics.port.PaymentAnalyticsRepository;
import in.bachatsetu.backend.admin.domain.analytics.port.StorageAnalyticsRepository;
import in.bachatsetu.backend.admin.domain.analytics.port.UserAnalyticsRepository;
import in.bachatsetu.backend.audit.application.usecase.CreateAuditEntryUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composes framework-free Analytics application services — additive to the Admin module, reusing its
 * existing {@link TransactionPort} adapter.
 *
 * <p>Gated on {@code bachatsetu.persistence.repositories.enabled}, matching every other module's application
 * config.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "bachatsetu.persistence.repositories",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class AnalyticsApplicationConfig {

    @Bean
    public AnalyticsApplicationMapper analyticsApplicationMapper() {
        return new AnalyticsApplicationMapper();
    }

    @Bean
    public GetOverviewAnalyticsUseCase getOverviewAnalyticsUseCase(
            OverviewAnalyticsRepository repository, TransactionPort transaction, AnalyticsApplicationMapper mapper,
            CreateAuditEntryUseCase createAuditEntry) {
        return new GetOverviewAnalyticsApplicationService(repository, transaction, mapper, createAuditEntry);
    }

    @Bean
    public GetPaymentAnalyticsUseCase getPaymentAnalyticsUseCase(
            PaymentAnalyticsRepository repository, TransactionPort transaction, AnalyticsApplicationMapper mapper,
            CreateAuditEntryUseCase createAuditEntry) {
        return new GetPaymentAnalyticsApplicationService(repository, transaction, mapper, createAuditEntry);
    }

    @Bean
    public GetGroupAnalyticsUseCase getGroupAnalyticsUseCase(
            GroupAnalyticsRepository repository, TransactionPort transaction, AnalyticsApplicationMapper mapper,
            CreateAuditEntryUseCase createAuditEntry) {
        return new GetGroupAnalyticsApplicationService(repository, transaction, mapper, createAuditEntry);
    }

    @Bean
    public GetUserAnalyticsUseCase getUserAnalyticsUseCase(
            UserAnalyticsRepository repository, TransactionPort transaction, AnalyticsApplicationMapper mapper,
            CreateAuditEntryUseCase createAuditEntry) {
        return new GetUserAnalyticsApplicationService(repository, transaction, mapper, createAuditEntry);
    }

    @Bean
    public GetNotificationAnalyticsUseCase getNotificationAnalyticsUseCase(
            NotificationAnalyticsRepository repository, TransactionPort transaction,
            AnalyticsApplicationMapper mapper, CreateAuditEntryUseCase createAuditEntry) {
        return new GetNotificationAnalyticsApplicationService(repository, transaction, mapper, createAuditEntry);
    }

    @Bean
    public GetStorageAnalyticsUseCase getStorageAnalyticsUseCase(
            StorageAnalyticsRepository repository, TransactionPort transaction, AnalyticsApplicationMapper mapper,
            CreateAuditEntryUseCase createAuditEntry) {
        return new GetStorageAnalyticsApplicationService(repository, transaction, mapper, createAuditEntry);
    }
}
