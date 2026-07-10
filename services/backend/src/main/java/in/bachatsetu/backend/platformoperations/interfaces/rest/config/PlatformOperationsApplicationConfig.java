package in.bachatsetu.backend.platformoperations.interfaces.rest.config;

import in.bachatsetu.backend.audit.application.usecase.CreateAuditEntryUseCase;
import in.bachatsetu.backend.notification.application.usecase.CreateNotificationUseCase;
import in.bachatsetu.backend.platformoperations.application.mapper.PlatformOperationsApplicationMapper;
import in.bachatsetu.backend.platformoperations.application.port.ClockPort;
import in.bachatsetu.backend.platformoperations.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.platformoperations.application.port.TransactionPort;
import in.bachatsetu.backend.platformoperations.application.service.ActivateTenantApplicationService;
import in.bachatsetu.backend.platformoperations.application.service.ArchiveTenantApplicationService;
import in.bachatsetu.backend.platformoperations.application.service.GetPlatformOverviewApplicationService;
import in.bachatsetu.backend.platformoperations.application.service.GetSystemHealthApplicationService;
import in.bachatsetu.backend.platformoperations.application.service.GetTenantApplicationService;
import in.bachatsetu.backend.platformoperations.application.service.ListActiveAnnouncementsApplicationService;
import in.bachatsetu.backend.platformoperations.application.service.ListAnnouncementsApplicationService;
import in.bachatsetu.backend.platformoperations.application.service.PublishAnnouncementApplicationService;
import in.bachatsetu.backend.platformoperations.application.service.SearchTenantsApplicationService;
import in.bachatsetu.backend.platformoperations.application.service.SendBroadcastNotificationApplicationService;
import in.bachatsetu.backend.platformoperations.application.service.SuspendTenantApplicationService;
import in.bachatsetu.backend.platformoperations.application.usecase.ActivateTenantUseCase;
import in.bachatsetu.backend.platformoperations.application.usecase.ArchiveTenantUseCase;
import in.bachatsetu.backend.platformoperations.application.usecase.GetPlatformOverviewUseCase;
import in.bachatsetu.backend.platformoperations.application.usecase.GetSystemHealthUseCase;
import in.bachatsetu.backend.platformoperations.application.usecase.GetTenantUseCase;
import in.bachatsetu.backend.platformoperations.application.usecase.ListActiveAnnouncementsUseCase;
import in.bachatsetu.backend.platformoperations.application.usecase.ListAnnouncementsUseCase;
import in.bachatsetu.backend.platformoperations.application.usecase.PublishAnnouncementUseCase;
import in.bachatsetu.backend.platformoperations.application.usecase.SearchTenantsUseCase;
import in.bachatsetu.backend.platformoperations.application.usecase.SendBroadcastNotificationUseCase;
import in.bachatsetu.backend.platformoperations.application.usecase.SuspendTenantUseCase;
import in.bachatsetu.backend.platformoperations.domain.port.AnnouncementRepository;
import in.bachatsetu.backend.platformoperations.domain.port.BroadcastRecipientRepository;
import in.bachatsetu.backend.platformoperations.domain.port.DatabaseHealthPort;
import in.bachatsetu.backend.platformoperations.domain.port.KnownTenantsRepository;
import in.bachatsetu.backend.platformoperations.domain.port.NotificationHealthPort;
import in.bachatsetu.backend.platformoperations.domain.port.PlatformOverviewRepository;
import in.bachatsetu.backend.platformoperations.domain.port.StorageHealthPort;
import in.bachatsetu.backend.platformoperations.domain.port.SystemRuntimeInfoPort;
import in.bachatsetu.backend.platformoperations.domain.port.TenantRepository;
import in.bachatsetu.backend.platformoperations.domain.port.TenantStatisticsRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composes framework-free Platform Operations application services.
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
public class PlatformOperationsApplicationConfig {

    @Bean
    public PlatformOperationsApplicationMapper platformOperationsApplicationMapper() {
        return new PlatformOperationsApplicationMapper();
    }

    @Bean
    public SuspendTenantUseCase suspendTenantUseCase(
            TenantRepository tenantRepository, TenantStatisticsRepository statisticsRepository,
            DomainEventPublisherPort eventPublisher, CreateAuditEntryUseCase createAuditEntry, ClockPort clock,
            TransactionPort transaction, PlatformOperationsApplicationMapper mapper) {
        return new SuspendTenantApplicationService(
                tenantRepository, statisticsRepository, eventPublisher, createAuditEntry, clock, transaction, mapper);
    }

    @Bean
    public ActivateTenantUseCase activateTenantUseCase(
            TenantRepository tenantRepository, TenantStatisticsRepository statisticsRepository,
            DomainEventPublisherPort eventPublisher, CreateAuditEntryUseCase createAuditEntry, ClockPort clock,
            TransactionPort transaction, PlatformOperationsApplicationMapper mapper) {
        return new ActivateTenantApplicationService(
                tenantRepository, statisticsRepository, eventPublisher, createAuditEntry, clock, transaction, mapper);
    }

    @Bean
    public ArchiveTenantUseCase archiveTenantUseCase(
            TenantRepository tenantRepository, TenantStatisticsRepository statisticsRepository,
            DomainEventPublisherPort eventPublisher, CreateAuditEntryUseCase createAuditEntry, ClockPort clock,
            TransactionPort transaction, PlatformOperationsApplicationMapper mapper) {
        return new ArchiveTenantApplicationService(
                tenantRepository, statisticsRepository, eventPublisher, createAuditEntry, clock, transaction, mapper);
    }

    @Bean
    public GetTenantUseCase getTenantUseCase(
            TenantRepository tenantRepository, TenantStatisticsRepository statisticsRepository, ClockPort clock,
            TransactionPort transaction, PlatformOperationsApplicationMapper mapper) {
        return new GetTenantApplicationService(tenantRepository, statisticsRepository, clock, transaction, mapper);
    }

    @Bean
    public SearchTenantsUseCase searchTenantsUseCase(
            KnownTenantsRepository knownTenantsRepository, TenantRepository tenantRepository,
            TenantStatisticsRepository statisticsRepository, ClockPort clock, TransactionPort transaction,
            PlatformOperationsApplicationMapper mapper) {
        return new SearchTenantsApplicationService(
                knownTenantsRepository, tenantRepository, statisticsRepository, clock, transaction, mapper);
    }

    @Bean
    public GetPlatformOverviewUseCase getPlatformOverviewUseCase(
            PlatformOverviewRepository repository, ClockPort clock, TransactionPort transaction) {
        return new GetPlatformOverviewApplicationService(repository, clock, transaction);
    }

    @Bean
    public PublishAnnouncementUseCase publishAnnouncementUseCase(
            AnnouncementRepository repository, DomainEventPublisherPort eventPublisher,
            CreateAuditEntryUseCase createAuditEntry, ClockPort clock, TransactionPort transaction,
            PlatformOperationsApplicationMapper mapper) {
        return new PublishAnnouncementApplicationService(
                repository, eventPublisher, createAuditEntry, clock, transaction, mapper);
    }

    @Bean
    public ListAnnouncementsUseCase listAnnouncementsUseCase(
            AnnouncementRepository repository, ClockPort clock, TransactionPort transaction,
            PlatformOperationsApplicationMapper mapper) {
        return new ListAnnouncementsApplicationService(repository, clock, transaction, mapper);
    }

    @Bean
    public ListActiveAnnouncementsUseCase listActiveAnnouncementsUseCase(
            AnnouncementRepository repository, ClockPort clock, TransactionPort transaction,
            PlatformOperationsApplicationMapper mapper) {
        return new ListActiveAnnouncementsApplicationService(repository, clock, transaction, mapper);
    }

    @Bean
    public SendBroadcastNotificationUseCase sendBroadcastNotificationUseCase(
            BroadcastRecipientRepository recipientRepository, CreateNotificationUseCase createNotification,
            CreateAuditEntryUseCase createAuditEntry, TransactionPort transaction) {
        return new SendBroadcastNotificationApplicationService(
                recipientRepository, createNotification, createAuditEntry, transaction);
    }

    @Bean
    public GetSystemHealthUseCase getSystemHealthUseCase(
            DatabaseHealthPort databaseHealth, StorageHealthPort storageHealth,
            NotificationHealthPort notificationHealth, SystemRuntimeInfoPort runtimeInfo) {
        return new GetSystemHealthApplicationService(databaseHealth, storageHealth, notificationHealth, runtimeInfo);
    }
}
