package in.bachatsetu.backend.platformoperations.interfaces.rest.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

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
import org.junit.jupiter.api.Test;

class PlatformOperationsApplicationConfigTest {

    private final PlatformOperationsApplicationConfig config = new PlatformOperationsApplicationConfig();
    private final PlatformOperationsApplicationMapper mapper = new PlatformOperationsApplicationMapper();

    @Test
    void composesSuspendTenantUseCase() {
        assertThat(config.suspendTenantUseCase(
                        mock(TenantRepository.class), mock(TenantStatisticsRepository.class),
                        mock(DomainEventPublisherPort.class), mock(CreateAuditEntryUseCase.class),
                        mock(ClockPort.class), mock(TransactionPort.class), mapper))
                .isInstanceOf(SuspendTenantApplicationService.class);
    }

    @Test
    void composesActivateTenantUseCase() {
        assertThat(config.activateTenantUseCase(
                        mock(TenantRepository.class), mock(TenantStatisticsRepository.class),
                        mock(DomainEventPublisherPort.class), mock(CreateAuditEntryUseCase.class),
                        mock(ClockPort.class), mock(TransactionPort.class), mapper))
                .isInstanceOf(ActivateTenantApplicationService.class);
    }

    @Test
    void composesArchiveTenantUseCase() {
        assertThat(config.archiveTenantUseCase(
                        mock(TenantRepository.class), mock(TenantStatisticsRepository.class),
                        mock(DomainEventPublisherPort.class), mock(CreateAuditEntryUseCase.class),
                        mock(ClockPort.class), mock(TransactionPort.class), mapper))
                .isInstanceOf(ArchiveTenantApplicationService.class);
    }

    @Test
    void composesGetTenantUseCase() {
        assertThat(config.getTenantUseCase(
                        mock(TenantRepository.class), mock(TenantStatisticsRepository.class), mock(ClockPort.class),
                        mock(TransactionPort.class), mapper))
                .isInstanceOf(GetTenantApplicationService.class);
    }

    @Test
    void composesSearchTenantsUseCase() {
        assertThat(config.searchTenantsUseCase(
                        mock(KnownTenantsRepository.class), mock(TenantRepository.class),
                        mock(TenantStatisticsRepository.class), mock(ClockPort.class), mock(TransactionPort.class),
                        mapper))
                .isInstanceOf(SearchTenantsApplicationService.class);
    }

    @Test
    void composesGetPlatformOverviewUseCase() {
        assertThat(config.getPlatformOverviewUseCase(
                        mock(PlatformOverviewRepository.class), mock(ClockPort.class), mock(TransactionPort.class)))
                .isInstanceOf(GetPlatformOverviewApplicationService.class);
    }

    @Test
    void composesPublishAnnouncementUseCase() {
        assertThat(config.publishAnnouncementUseCase(
                        mock(AnnouncementRepository.class), mock(DomainEventPublisherPort.class),
                        mock(CreateAuditEntryUseCase.class), mock(ClockPort.class), mock(TransactionPort.class),
                        mapper))
                .isInstanceOf(PublishAnnouncementApplicationService.class);
    }

    @Test
    void composesListAnnouncementsUseCase() {
        assertThat(config.listAnnouncementsUseCase(
                        mock(AnnouncementRepository.class), mock(ClockPort.class), mock(TransactionPort.class),
                        mapper))
                .isInstanceOf(ListAnnouncementsApplicationService.class);
    }

    @Test
    void composesListActiveAnnouncementsUseCase() {
        assertThat(config.listActiveAnnouncementsUseCase(
                        mock(AnnouncementRepository.class), mock(ClockPort.class), mock(TransactionPort.class),
                        mapper))
                .isInstanceOf(ListActiveAnnouncementsApplicationService.class);
    }

    @Test
    void composesSendBroadcastNotificationUseCase() {
        assertThat(config.sendBroadcastNotificationUseCase(
                        mock(BroadcastRecipientRepository.class), mock(CreateNotificationUseCase.class),
                        mock(CreateAuditEntryUseCase.class), mock(TransactionPort.class)))
                .isInstanceOf(SendBroadcastNotificationApplicationService.class);
    }

    @Test
    void composesGetSystemHealthUseCase() {
        assertThat(config.getSystemHealthUseCase(
                        mock(DatabaseHealthPort.class), mock(StorageHealthPort.class),
                        mock(NotificationHealthPort.class), mock(SystemRuntimeInfoPort.class)))
                .isInstanceOf(GetSystemHealthApplicationService.class);
    }
}
