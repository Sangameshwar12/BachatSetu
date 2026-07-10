package in.bachatsetu.backend.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.admin.domain.model.PlatformUserStatus;
import in.bachatsetu.backend.admin.domain.model.PlatformUserSummary;
import in.bachatsetu.backend.admin.domain.port.PlatformPage;
import in.bachatsetu.backend.admin.domain.port.PlatformUserSearchCriteria;
import in.bachatsetu.backend.admin.domain.port.PlatformUserSortField;
import in.bachatsetu.backend.admin.domain.port.SortDirection;
import in.bachatsetu.backend.auth.domain.model.UserStatus;
import in.bachatsetu.backend.infrastructure.persistence.entity.identity.UserJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.UserSpringDataRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

class AdminUserRepositoryAdapterTest {

    private static final Instant NOW = Instant.parse("2026-07-08T08:00:00Z");

    private UserSpringDataRepository repository;
    private AdminUserRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        repository = mock(UserSpringDataRepository.class);
        adapter = new AdminUserRepositoryAdapter(repository);
    }

    @Test
    void searchesAcrossTenantsAndMapsResults() {
        UserJpaEntity entity = newEntity();
        Page<UserJpaEntity> page = new PageImpl<>(List.of(entity));
        when(repository.searchAcrossTenants(
                        eq(UserStatus.ACTIVE), eq("a@b.com"), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);
        PlatformUserSearchCriteria criteria = new PlatformUserSearchCriteria(
                PlatformUserStatus.ACTIVE, "a@b.com", null, null, null, 0, 20, PlatformUserSortField.CREATED_AT,
                SortDirection.DESC);

        PlatformPage<PlatformUserSummary> result = adapter.search(criteria);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).status()).isEqualTo(PlatformUserStatus.ACTIVE);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void treatsANullAuthenticationStatusAsPendingVerification() {
        UserJpaEntity entity = mock(UserJpaEntity.class);
        when(entity.getId()).thenReturn(UUID.randomUUID());
        when(entity.getTenantId()).thenReturn(UUID.randomUUID());
        when(entity.getAuthenticationStatus()).thenReturn(null);
        when(entity.getCreatedAt()).thenReturn(NOW);
        Page<UserJpaEntity> page = new PageImpl<>(List.of(entity));
        when(repository.searchAcrossTenants(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);
        PlatformUserSearchCriteria criteria = new PlatformUserSearchCriteria(
                null, null, null, null, null, 0, 20, PlatformUserSortField.CREATED_AT, SortDirection.DESC);

        PlatformPage<PlatformUserSummary> result = adapter.search(criteria);

        assertThat(result.content().get(0).status()).isEqualTo(PlatformUserStatus.PENDING_VERIFICATION);
    }

    @Test
    void findsAnExistingUserById() {
        AggregateId userId = AggregateId.newId();
        UserJpaEntity entity = newEntity();
        when(repository.findByIdAndDeletedFalse(userId.value())).thenReturn(Optional.of(entity));

        assertThat(adapter.findById(userId)).isPresent();
    }

    @Test
    void reportsNoMatchWhenTheUserDoesNotExist() {
        AggregateId userId = AggregateId.newId();
        when(repository.findByIdAndDeletedFalse(userId.value())).thenReturn(Optional.empty());

        assertThat(adapter.findById(userId)).isEmpty();
    }

    @Test
    void updatesTheAuthenticationStatusAndReportsSuccess() {
        AggregateId userId = AggregateId.newId();
        AggregateId administratorId = AggregateId.newId();
        when(repository.updateAuthenticationStatus(userId.value(), UserStatus.ACTIVE, administratorId.value(), NOW))
                .thenReturn(1);

        boolean updated = adapter.updateStatus(userId, PlatformUserStatus.ACTIVE, administratorId, NOW);

        assertThat(updated).isTrue();
        verify(repository).updateAuthenticationStatus(userId.value(), UserStatus.ACTIVE, administratorId.value(), NOW);
    }

    @Test
    void reportsFailureWhenNoRowWasUpdated() {
        when(repository.updateAuthenticationStatus(any(), any(), any(), any())).thenReturn(0);

        boolean updated = adapter.updateStatus(
                AggregateId.newId(), PlatformUserStatus.DISABLED, AggregateId.newId(), NOW);

        assertThat(updated).isFalse();
    }

    private UserJpaEntity newEntity() {
        UserJpaEntity entity = mock(UserJpaEntity.class);
        when(entity.getId()).thenReturn(UUID.randomUUID());
        when(entity.getTenantId()).thenReturn(UUID.randomUUID());
        when(entity.getEmail()).thenReturn("a@b.com");
        when(entity.getPhoneNumber()).thenReturn("+919876543210");
        when(entity.getGivenName()).thenReturn("Asha");
        when(entity.getFamilyName()).thenReturn("Rao");
        when(entity.getAuthenticationStatus()).thenReturn(UserStatus.ACTIVE);
        when(entity.getCreatedAt()).thenReturn(NOW);
        return entity;
    }
}
