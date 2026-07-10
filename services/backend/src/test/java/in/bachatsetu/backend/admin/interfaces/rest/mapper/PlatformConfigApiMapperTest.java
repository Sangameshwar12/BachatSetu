package in.bachatsetu.backend.admin.interfaces.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.admin.application.configuration.command.UpdateConfigurationCommand;
import in.bachatsetu.backend.admin.application.configuration.command.UpdateFeatureFlagsCommand;
import in.bachatsetu.backend.admin.application.configuration.command.UpdateSystemLimitsCommand;
import in.bachatsetu.backend.admin.application.configuration.query.FeatureFlagResult;
import in.bachatsetu.backend.admin.application.configuration.query.PlatformConfigurationResult;
import in.bachatsetu.backend.admin.application.configuration.query.PlatformLimitResult;
import in.bachatsetu.backend.admin.domain.configuration.model.FeatureKey;
import in.bachatsetu.backend.admin.domain.configuration.model.LimitKey;
import in.bachatsetu.backend.admin.interfaces.rest.dto.config.PlatformConfigurationResponse;
import in.bachatsetu.backend.admin.interfaces.rest.dto.config.UpdateConfigurationRequest;
import in.bachatsetu.backend.admin.interfaces.rest.dto.config.UpdateFeatureFlagsRequest;
import in.bachatsetu.backend.admin.interfaces.rest.dto.config.UpdateSystemLimitsRequest;
import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlatformConfigApiMapperTest {

    private final PlatformConfigApiMapper mapper = new PlatformConfigApiMapper();

    @Test
    void mapsConfigurationIncludingInstantsAsIsoStrings() {
        UUID updatedBy = UUID.randomUUID();
        PlatformConfigurationResult result = new PlatformConfigurationResult(
                "ENGLISH", 300, "LOCAL", "RAZORPAY", 3, 10_485_760L, 100, 20, true, "down",
                Instant.parse("2026-07-09T08:00:00Z"), Instant.parse("2026-07-09T09:00:00Z"), 2,
                Instant.parse("2026-07-09T08:00:00Z"), updatedBy);

        PlatformConfigurationResponse response = mapper.toResponse(result);

        assertThat(response.maintenanceStartAt()).isEqualTo("2026-07-09T08:00:00Z");
        assertThat(response.updatedBy()).isEqualTo(updatedBy.toString());
    }

    @Test
    void toCommandDerivesAdministratorIdFromCurrentUser() {
        UpdateConfigurationRequest request = new UpdateConfigurationRequest(
                "ENGLISH", 300, "LOCAL", "RAZORPAY", 3, 10_485_760L, 100, 20, false, null, null, null);
        AuthenticatedUser currentUser = authenticatedUser();

        UpdateConfigurationCommand command = mapper.toCommand(request, currentUser);

        assertThat(command.administratorId()).isEqualTo(currentUser.userId().toAggregateId());
        assertThat(command.defaultLanguage()).isEqualTo("ENGLISH");
    }

    @Test
    void toCommandParsesMaintenanceInstants() {
        UpdateConfigurationRequest request = new UpdateConfigurationRequest(
                "ENGLISH", 300, "LOCAL", "RAZORPAY", 3, 10_485_760L, 100, 20, true, "down",
                "2026-07-09T08:00:00Z", "2026-07-09T09:00:00Z");

        UpdateConfigurationCommand command = mapper.toCommand(request, authenticatedUser());

        assertThat(command.maintenanceStartAt()).isEqualTo(Instant.parse("2026-07-09T08:00:00Z"));
    }

    @Test
    void toCommandRejectsAnInvalidInstantString() {
        UpdateConfigurationRequest request = new UpdateConfigurationRequest(
                "ENGLISH", 300, "LOCAL", "RAZORPAY", 3, 10_485_760L, 100, 20, true, "down", "not-an-instant", null);

        assertThatThrownBy(() -> mapper.toCommand(request, authenticatedUser()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void mapsFeatureFlagUpdateRequestToCommand() {
        UpdateFeatureFlagsRequest request = new UpdateFeatureFlagsRequest(Map.of("PAYMENTS", false));

        UpdateFeatureFlagsCommand command = mapper.toCommand(request, authenticatedUser());

        assertThat(command.changes()).containsEntry(FeatureKey.PAYMENTS, false);
    }

    @Test
    void rejectsAnUnknownFeatureKey() {
        UpdateFeatureFlagsRequest request = new UpdateFeatureFlagsRequest(Map.of("NOT_A_FEATURE", false));

        assertThatThrownBy(() -> mapper.toCommand(request, authenticatedUser()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void mapsFeatureFlagResult() {
        FeatureFlagResult result = new FeatureFlagResult("STORAGE", true, 1, Instant.parse("2026-07-09T08:00:00Z"),
                null);

        assertThat(mapper.toResponse(result).key()).isEqualTo("STORAGE");
    }

    @Test
    void mapsSystemLimitsUpdateRequestToCommand() {
        UpdateSystemLimitsRequest request = new UpdateSystemLimitsRequest(Map.of("MAX_GROUPS", 500L));

        UpdateSystemLimitsCommand command = mapper.toCommand(request, authenticatedUser());

        assertThat(command.changes()).containsEntry(LimitKey.MAX_GROUPS, 500L);
    }

    @Test
    void rejectsAnUnknownLimitKey() {
        UpdateSystemLimitsRequest request = new UpdateSystemLimitsRequest(Map.of("NOT_A_LIMIT", 1L));

        assertThatThrownBy(() -> mapper.toCommand(request, authenticatedUser()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void mapsPlatformLimitResult() {
        PlatformLimitResult result = new PlatformLimitResult(
                "MAX_MEMBERS", 500, 1, Instant.parse("2026-07-09T08:00:00Z"), null);

        assertThat(mapper.toResponse(result).value()).isEqualTo(500);
    }

    private AuthenticatedUser authenticatedUser() {
        return new AuthenticatedUser(
                UserId.newId(), MobileNumber.of("+919876543210"), AggregateId.newId(), Set.of("PLATFORM_ADMIN"),
                Set.of());
    }
}
