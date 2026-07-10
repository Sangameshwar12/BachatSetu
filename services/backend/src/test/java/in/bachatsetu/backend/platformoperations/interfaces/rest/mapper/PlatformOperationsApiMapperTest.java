package in.bachatsetu.backend.platformoperations.interfaces.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.platformoperations.application.command.PublishAnnouncementCommand;
import in.bachatsetu.backend.platformoperations.application.command.SendBroadcastNotificationCommand;
import in.bachatsetu.backend.platformoperations.application.command.SuspendTenantCommand;
import in.bachatsetu.backend.platformoperations.application.query.AnnouncementResult;
import in.bachatsetu.backend.platformoperations.application.query.PlatformOverviewResult;
import in.bachatsetu.backend.platformoperations.application.query.SystemHealthResult;
import in.bachatsetu.backend.platformoperations.application.query.TenantResult;
import in.bachatsetu.backend.platformoperations.application.usecase.GetTenantUseCase;
import in.bachatsetu.backend.platformoperations.domain.model.AnnouncementSeverity;
import in.bachatsetu.backend.platformoperations.domain.model.BroadcastScope;
import in.bachatsetu.backend.platformoperations.domain.model.ComponentHealth;
import in.bachatsetu.backend.platformoperations.domain.model.HealthStatus;
import in.bachatsetu.backend.platformoperations.domain.model.TenantStatistics;
import in.bachatsetu.backend.platformoperations.domain.model.TenantStatus;
import in.bachatsetu.backend.platformoperations.interfaces.rest.dto.BroadcastRequest;
import in.bachatsetu.backend.platformoperations.interfaces.rest.dto.PublishAnnouncementRequest;
import in.bachatsetu.backend.platformoperations.interfaces.rest.dto.SuspendTenantRequest;
import in.bachatsetu.backend.platformoperations.interfaces.rest.dto.SystemHealthResponse;
import in.bachatsetu.backend.platformoperations.interfaces.rest.dto.TenantResponse;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PlatformOperationsApiMapperTest {

    private static final Instant NOW = Instant.parse("2026-07-10T08:00:00Z");

    private final PlatformOperationsApiMapper mapper = new PlatformOperationsApiMapper();

    @Test
    void mapsASuspendRequestToACommand() {
        AggregateId tenantId = AggregateId.newId();
        AuthenticatedUser currentUser = authenticatedUser();

        SuspendTenantCommand command =
                mapper.toSuspendCommand(tenantId.toString(), currentUser, new SuspendTenantRequest("Fraud"));

        assertThat(command.tenantId()).isEqualTo(tenantId);
        assertThat(command.reason()).isEqualTo("Fraud");
    }

    @Test
    void resolvesATenantThroughTheUseCase() {
        AggregateId tenantId = AggregateId.newId();
        GetTenantUseCase useCase = mock(GetTenantUseCase.class);
        when(useCase.execute(tenantId)).thenReturn(new TenantResult(
                tenantId, TenantStatus.ACTIVE, null, new TenantStatistics(0, 0, 0, 0, 0, 0, 0, null)));

        TenantResponse response = mapper.get(useCase, tenantId.toString());

        assertThat(response.tenantId()).isEqualTo(tenantId.toString());
        assertThat(response.status()).isEqualTo("ACTIVE");
    }

    @Test
    void mapsAnOverviewResultToAResponse() {
        PlatformOverviewResult result = new PlatformOverviewResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 100, 1, 2, 3, 4, 5);

        assertThat(mapper.toResponse(result).totalUsers()).isEqualTo(1);
    }

    @Test
    void mapsAPublishRequestToACommand() {
        Instant start = NOW;
        Instant end = NOW.plusSeconds(3600);

        PublishAnnouncementCommand command = mapper.toPublishCommand(
                authenticatedUser(), new PublishAnnouncementRequest("Title", "Message", start, end, "WARNING"));

        assertThat(command.severity()).isEqualTo(AnnouncementSeverity.WARNING);
    }

    @Test
    void mapsAnAnnouncementResultToAResponse() {
        AnnouncementResult result = new AnnouncementResult(
                AggregateId.newId(), "Title", "Message", NOW, NOW.plusSeconds(60), AnnouncementSeverity.INFO, true);

        assertThat(mapper.toResponse(result).active()).isTrue();
    }

    @Test
    void mapsABroadcastRequestToACommand() {
        SendBroadcastNotificationCommand command = mapper.toBroadcastCommand(
                authenticatedUser(), new BroadcastRequest("ALL_USERS", null, "Title", "Message"));

        assertThat(command.scope()).isEqualTo(BroadcastScope.ALL_USERS);
    }

    @Test
    void mapsASystemHealthResultToAResponse() {
        SystemHealthResult result = new SystemHealthResult(
                new ComponentHealth("database", HealthStatus.UP, "ok"),
                new ComponentHealth("storage", HealthStatus.UP, "ok"),
                new ComponentHealth("notification", HealthStatus.UP, "ok"), 100, "21", "1.0.0", null, 1, 2, 3, 4, 5);

        SystemHealthResponse response = mapper.toResponse(result);

        assertThat(response.database().status()).isEqualTo("UP");
    }

    private AuthenticatedUser authenticatedUser() {
        return new AuthenticatedUser(
                UserId.newId(), MobileNumber.of("+919876543210"), AggregateId.newId(), Set.of(), Set.of());
    }
}
