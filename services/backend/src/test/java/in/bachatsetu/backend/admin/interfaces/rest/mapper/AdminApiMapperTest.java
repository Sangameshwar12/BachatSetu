package in.bachatsetu.backend.admin.interfaces.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.admin.application.command.DisableUserCommand;
import in.bachatsetu.backend.admin.application.command.EnableUserCommand;
import in.bachatsetu.backend.admin.application.query.PlatformGroupResult;
import in.bachatsetu.backend.admin.application.query.PlatformStatisticsResult;
import in.bachatsetu.backend.admin.application.query.PlatformTenantResult;
import in.bachatsetu.backend.admin.application.query.PlatformUserResult;
import in.bachatsetu.backend.admin.domain.model.PlatformGroupStatus;
import in.bachatsetu.backend.admin.domain.model.PlatformUserStatus;
import in.bachatsetu.backend.admin.domain.port.PlatformGroupSearchCriteria;
import in.bachatsetu.backend.admin.domain.port.PlatformPage;
import in.bachatsetu.backend.admin.domain.port.PlatformPageRequest;
import in.bachatsetu.backend.admin.domain.port.PlatformUserSearchCriteria;
import in.bachatsetu.backend.admin.domain.port.PlatformUserSortField;
import in.bachatsetu.backend.admin.domain.port.SortDirection;
import in.bachatsetu.backend.admin.interfaces.rest.config.AdminProperties;
import in.bachatsetu.backend.admin.interfaces.rest.dto.PageResponse;
import in.bachatsetu.backend.admin.interfaces.rest.dto.PlatformGroupResponse;
import in.bachatsetu.backend.admin.interfaces.rest.dto.PlatformStatisticsResponse;
import in.bachatsetu.backend.admin.interfaces.rest.dto.PlatformTenantResponse;
import in.bachatsetu.backend.admin.interfaces.rest.dto.PlatformUserResponse;
import in.bachatsetu.backend.auth.application.security.AuthenticatedUser;
import in.bachatsetu.backend.auth.domain.model.MobileNumber;
import in.bachatsetu.backend.auth.domain.model.UserId;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AdminApiMapperTest {

    private static final Instant NOW = Instant.parse("2026-07-08T08:00:00Z");

    private final AdminApiMapper mapper = new AdminApiMapper(new AdminProperties(true, 20, 100));

    @Test
    void mapsUserSearchParametersApplyingConfiguredDefaults() {
        PlatformUserSearchCriteria criteria = mapper.toUserSearchCriteria(
                "ACTIVE", "a@b.com", "+91", NOW.minusSeconds(60), NOW, null, null, "createdAt", "asc");

        assertThat(criteria.status()).isEqualTo(PlatformUserStatus.ACTIVE);
        assertThat(criteria.email()).isEqualTo("a@b.com");
        assertThat(criteria.page()).isZero();
        assertThat(criteria.size()).isEqualTo(20);
        assertThat(criteria.sortField()).isEqualTo(PlatformUserSortField.CREATED_AT);
        assertThat(criteria.direction()).isEqualTo(SortDirection.ASC);
    }

    @Test
    void mapsUserSearchParametersWithEveryOptionalFilterOmitted() {
        PlatformUserSearchCriteria criteria = mapper.toUserSearchCriteria(
                null, null, null, null, null, 2, 10, "email", "desc");

        assertThat(criteria.status()).isNull();
        assertThat(criteria.page()).isEqualTo(2);
        assertThat(criteria.size()).isEqualTo(10);
        assertThat(criteria.sortField()).isEqualTo(PlatformUserSortField.EMAIL);
    }

    @Test
    void rejectsASizeAboveTheConfiguredMaximum() {
        assertThatThrownBy(() -> mapper.toUserSearchCriteria(
                        null, null, null, null, null, 0, 500, "createdAt", "desc"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsAnUnsupportedSortField() {
        assertThatThrownBy(() -> mapper.toUserSearchCriteria(
                        null, null, null, null, null, 0, 20, "unsupported", "desc"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsAnUnsupportedSortDirection() {
        assertThatThrownBy(() -> mapper.toUserSearchCriteria(
                        null, null, null, null, null, 0, 20, "createdAt", "sideways"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void mapsGroupSearchParameters() {
        PlatformGroupSearchCriteria criteria =
                mapper.toGroupSearchCriteria("ACTIVE", null, null, 1, 5, "asc");

        assertThat(criteria.status()).isEqualTo(PlatformGroupStatus.ACTIVE);
        assertThat(criteria.page()).isEqualTo(1);
        assertThat(criteria.size()).isEqualTo(5);
        assertThat(criteria.direction()).isEqualTo(SortDirection.ASC);
    }

    @Test
    void mapsAPageRequestApplyingConfiguredDefaults() {
        PlatformPageRequest pageRequest = mapper.toPageRequest(null, null);

        assertThat(pageRequest.page()).isZero();
        assertThat(pageRequest.size()).isEqualTo(20);
    }

    @Test
    void mapsAnEnableCommandUsingTheCallersIdentity() {
        AggregateId userId = AggregateId.newId();
        AuthenticatedUser currentUser = authenticatedUser();

        EnableUserCommand command = mapper.toEnableCommand(userId.toString(), currentUser);

        assertThat(command.userId()).isEqualTo(userId);
        assertThat(command.administratorId()).isEqualTo(currentUser.userId().toAggregateId());
    }

    @Test
    void mapsADisableCommandUsingTheCallersIdentity() {
        AggregateId userId = AggregateId.newId();
        AuthenticatedUser currentUser = authenticatedUser();

        DisableUserCommand command = mapper.toDisableCommand(userId.toString(), currentUser);

        assertThat(command.userId()).isEqualTo(userId);
        assertThat(command.administratorId()).isEqualTo(currentUser.userId().toAggregateId());
    }

    @Test
    void mapsAUserResultToAResponse() {
        PlatformUserResult result = new PlatformUserResult(
                UUID.randomUUID(), UUID.randomUUID(), "a@b.com", "+919876543210", "Asha", "Rao",
                PlatformUserStatus.ACTIVE, NOW);

        PlatformUserResponse response = mapper.toResponse(result);

        assertThat(response.email()).isEqualTo("a@b.com");
        assertThat(response.status()).isEqualTo("ACTIVE");
    }

    @Test
    void mapsAGroupResultToAResponse() {
        PlatformGroupResult result = new PlatformGroupResult(
                UUID.randomUUID(), UUID.randomUUID(), "GRP-1", "name", PlatformGroupStatus.ACTIVE, 3, NOW);

        PlatformGroupResponse response = mapper.toResponse(result);

        assertThat(response.code()).isEqualTo("GRP-1");
        assertThat(response.memberCount()).isEqualTo(3);
    }

    @Test
    void mapsATenantResultToAResponse() {
        PlatformTenantResult result = new PlatformTenantResult(UUID.randomUUID(), 5, 2);

        PlatformTenantResponse response = mapper.toResponse(result);

        assertThat(response.userCount()).isEqualTo(5);
        assertThat(response.groupCount()).isEqualTo(2);
    }

    @Test
    void mapsAStatisticsResultToAResponse() {
        PlatformStatisticsResult result =
                new PlatformStatisticsResult(10, 8, 2, 5, 4, 20, 15, 15, 30, 7);

        PlatformStatisticsResponse response = mapper.toResponse(result);

        assertThat(response.totalUsers()).isEqualTo(10);
        assertThat(response.completedPayments()).isEqualTo(15);
    }

    @Test
    void mapsAUserPageToAPageResponse() {
        PlatformUserResult result = new PlatformUserResult(
                UUID.randomUUID(), UUID.randomUUID(), null, null, null, null, PlatformUserStatus.ACTIVE, NOW);
        PlatformPage<PlatformUserResult> page = new PlatformPage<>(List.of(result), 0, 20, 1);

        PageResponse<PlatformUserResponse> response = mapper.toUserPageResponse(page);

        assertThat(response.content()).hasSize(1);
        assertThat(response.totalElements()).isEqualTo(1);
    }

    @Test
    void mapsAGroupPageToAPageResponse() {
        PlatformGroupResult result = new PlatformGroupResult(
                UUID.randomUUID(), UUID.randomUUID(), "GRP-1", "name", PlatformGroupStatus.ACTIVE, 0, NOW);
        PlatformPage<PlatformGroupResult> page = new PlatformPage<>(List.of(result), 0, 20, 1);

        assertThat(mapper.toGroupPageResponse(page).content()).hasSize(1);
    }

    @Test
    void mapsATenantPageToAPageResponse() {
        PlatformTenantResult result = new PlatformTenantResult(UUID.randomUUID(), 1, 1);
        PlatformPage<PlatformTenantResult> page = new PlatformPage<>(List.of(result), 0, 20, 1);

        assertThat(mapper.toTenantPageResponse(page).content()).hasSize(1);
    }

    private AuthenticatedUser authenticatedUser() {
        return new AuthenticatedUser(
                UserId.newId(), MobileNumber.of("+919876543210"), AggregateId.newId(), Set.of("PLATFORM_ADMIN"),
                Set.of());
    }
}
