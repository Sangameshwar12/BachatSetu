package in.bachatsetu.backend.admin.interfaces.rest.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import in.bachatsetu.backend.admin.application.analytics.mapper.AnalyticsApplicationMapper;
import in.bachatsetu.backend.admin.application.analytics.service.GetGroupAnalyticsApplicationService;
import in.bachatsetu.backend.admin.application.analytics.service.GetNotificationAnalyticsApplicationService;
import in.bachatsetu.backend.admin.application.analytics.service.GetOverviewAnalyticsApplicationService;
import in.bachatsetu.backend.admin.application.analytics.service.GetPaymentAnalyticsApplicationService;
import in.bachatsetu.backend.admin.application.analytics.service.GetStorageAnalyticsApplicationService;
import in.bachatsetu.backend.admin.application.analytics.service.GetUserAnalyticsApplicationService;
import in.bachatsetu.backend.admin.application.port.TransactionPort;
import in.bachatsetu.backend.admin.domain.analytics.port.GroupAnalyticsRepository;
import in.bachatsetu.backend.admin.domain.analytics.port.NotificationAnalyticsRepository;
import in.bachatsetu.backend.admin.domain.analytics.port.OverviewAnalyticsRepository;
import in.bachatsetu.backend.admin.domain.analytics.port.PaymentAnalyticsRepository;
import in.bachatsetu.backend.admin.domain.analytics.port.StorageAnalyticsRepository;
import in.bachatsetu.backend.admin.domain.analytics.port.UserAnalyticsRepository;
import in.bachatsetu.backend.audit.application.usecase.CreateAuditEntryUseCase;
import org.junit.jupiter.api.Test;

class AnalyticsApplicationConfigTest {

    private final AnalyticsApplicationConfig config = new AnalyticsApplicationConfig();
    private final AnalyticsApplicationMapper mapper = config.analyticsApplicationMapper();
    private final TransactionPort transaction = mock(TransactionPort.class);
    private final CreateAuditEntryUseCase createAuditEntry = mock(CreateAuditEntryUseCase.class);

    @Test
    void composesGetOverviewAnalyticsUseCase() {
        assertThat(config.getOverviewAnalyticsUseCase(
                        mock(OverviewAnalyticsRepository.class), transaction, mapper, createAuditEntry))
                .isInstanceOf(GetOverviewAnalyticsApplicationService.class);
    }

    @Test
    void composesGetPaymentAnalyticsUseCase() {
        assertThat(config.getPaymentAnalyticsUseCase(
                        mock(PaymentAnalyticsRepository.class), transaction, mapper, createAuditEntry))
                .isInstanceOf(GetPaymentAnalyticsApplicationService.class);
    }

    @Test
    void composesGetGroupAnalyticsUseCase() {
        assertThat(config.getGroupAnalyticsUseCase(
                        mock(GroupAnalyticsRepository.class), transaction, mapper, createAuditEntry))
                .isInstanceOf(GetGroupAnalyticsApplicationService.class);
    }

    @Test
    void composesGetUserAnalyticsUseCase() {
        assertThat(config.getUserAnalyticsUseCase(
                        mock(UserAnalyticsRepository.class), transaction, mapper, createAuditEntry))
                .isInstanceOf(GetUserAnalyticsApplicationService.class);
    }

    @Test
    void composesGetNotificationAnalyticsUseCase() {
        assertThat(config.getNotificationAnalyticsUseCase(
                        mock(NotificationAnalyticsRepository.class), transaction, mapper, createAuditEntry))
                .isInstanceOf(GetNotificationAnalyticsApplicationService.class);
    }

    @Test
    void composesGetStorageAnalyticsUseCase() {
        assertThat(config.getStorageAnalyticsUseCase(
                        mock(StorageAnalyticsRepository.class), transaction, mapper, createAuditEntry))
                .isInstanceOf(GetStorageAnalyticsApplicationService.class);
    }
}
