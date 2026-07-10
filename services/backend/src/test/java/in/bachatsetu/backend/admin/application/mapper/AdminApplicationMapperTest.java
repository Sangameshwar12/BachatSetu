package in.bachatsetu.backend.admin.application.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.admin.application.query.PlatformGroupResult;
import in.bachatsetu.backend.admin.application.query.PlatformStatisticsResult;
import in.bachatsetu.backend.admin.application.query.PlatformTenantResult;
import in.bachatsetu.backend.admin.application.query.PlatformUserResult;
import in.bachatsetu.backend.admin.domain.model.PlatformGroupStatus;
import in.bachatsetu.backend.admin.domain.model.PlatformGroupSummary;
import in.bachatsetu.backend.admin.domain.model.PlatformStatistics;
import in.bachatsetu.backend.admin.domain.model.PlatformTenantSummary;
import in.bachatsetu.backend.admin.domain.model.PlatformUserStatus;
import in.bachatsetu.backend.admin.domain.model.PlatformUserSummary;
import in.bachatsetu.backend.admin.domain.port.PlatformPage;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class AdminApplicationMapperTest {

    private static final Instant NOW = Instant.parse("2026-07-08T08:00:00Z");

    private final AdminApplicationMapper mapper = new AdminApplicationMapper();

    @Test
    void mapsAUserSummaryToAResult() {
        AggregateId userId = AggregateId.newId();
        AggregateId tenantId = AggregateId.newId();
        PlatformUserSummary summary = new PlatformUserSummary(
                userId, tenantId, "a@b.com", "+919876543210", "Asha", "Rao", PlatformUserStatus.ACTIVE, NOW);

        PlatformUserResult result = mapper.toResult(summary);

        assertThat(result.userId()).isEqualTo(userId.value());
        assertThat(result.tenantId()).isEqualTo(tenantId.value());
        assertThat(result.email()).isEqualTo("a@b.com");
        assertThat(result.status()).isEqualTo(PlatformUserStatus.ACTIVE);
    }

    @Test
    void mapsAUserPagePreservingPagingMetadata() {
        PlatformUserSummary summary = new PlatformUserSummary(
                AggregateId.newId(), AggregateId.newId(), null, null, null, null, PlatformUserStatus.ACTIVE, NOW);
        PlatformPage<PlatformUserSummary> page = new PlatformPage<>(List.of(summary), 0, 20, 1);

        PlatformPage<PlatformUserResult> result = mapper.toResultPage(page);

        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void mapsAGroupSummaryToAResult() {
        AggregateId groupId = AggregateId.newId();
        PlatformGroupSummary summary = new PlatformGroupSummary(
                groupId, AggregateId.newId(), "GRP-1", "name", PlatformGroupStatus.ACTIVE, 4, NOW);

        PlatformGroupResult result = mapper.toResult(summary);

        assertThat(result.groupId()).isEqualTo(groupId.value());
        assertThat(result.memberCount()).isEqualTo(4);
    }

    @Test
    void mapsAGroupPagePreservingPagingMetadata() {
        PlatformGroupSummary summary = new PlatformGroupSummary(
                AggregateId.newId(), AggregateId.newId(), "GRP-1", "name", PlatformGroupStatus.ACTIVE, 0, NOW);
        PlatformPage<PlatformGroupSummary> page = new PlatformPage<>(List.of(summary), 0, 20, 1);

        assertThat(mapper.toGroupResultPage(page).content()).hasSize(1);
    }

    @Test
    void mapsATenantSummaryToAResult() {
        AggregateId tenantId = AggregateId.newId();
        PlatformTenantSummary summary = new PlatformTenantSummary(tenantId, 5, 2);

        PlatformTenantResult result = mapper.toResult(summary);

        assertThat(result.tenantId()).isEqualTo(tenantId.value());
        assertThat(result.userCount()).isEqualTo(5);
    }

    @Test
    void mapsATenantPagePreservingPagingMetadata() {
        PlatformTenantSummary summary = new PlatformTenantSummary(AggregateId.newId(), 1, 1);
        PlatformPage<PlatformTenantSummary> page = new PlatformPage<>(List.of(summary), 0, 20, 1);

        assertThat(mapper.toTenantResultPage(page).content()).hasSize(1);
    }

    @Test
    void mapsStatisticsToAResult() {
        PlatformStatistics statistics = new PlatformStatistics(10, 8, 2, 5, 4, 20, 15, 15, 30, 7);

        PlatformStatisticsResult result = mapper.toResult(statistics);

        assertThat(result.totalUsers()).isEqualTo(10);
        assertThat(result.completedPayments()).isEqualTo(15);
    }

    @Test
    void rejectsNullArguments() {
        assertThatThrownBy(() -> mapper.toResult((PlatformUserSummary) null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> mapper.toResultPage(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> mapper.toResult((PlatformGroupSummary) null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> mapper.toGroupResultPage(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> mapper.toResult((PlatformTenantSummary) null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> mapper.toTenantResultPage(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> mapper.toResult((PlatformStatistics) null)).isInstanceOf(NullPointerException.class);
    }
}
