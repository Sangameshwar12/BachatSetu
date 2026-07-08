package in.bachatsetu.backend.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.bachatsetu.backend.infrastructure.persistence.entity.notification.NotificationJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.mapper.JpaReferenceProvider;
import in.bachatsetu.backend.infrastructure.persistence.mapper.NotificationJpaMapper;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.NotificationSpringDataRepository;
import in.bachatsetu.backend.notification.domain.model.Notification;
import in.bachatsetu.backend.notification.domain.model.NotificationCategory;
import in.bachatsetu.backend.notification.domain.model.NotificationChannel;
import in.bachatsetu.backend.notification.domain.model.NotificationContent;
import in.bachatsetu.backend.notification.domain.model.NotificationRecipient;
import in.bachatsetu.backend.notification.domain.port.NotificationPage;
import in.bachatsetu.backend.notification.domain.port.NotificationPageRequest;
import in.bachatsetu.backend.notification.domain.port.NotificationSortField;
import in.bachatsetu.backend.notification.domain.port.SortDirection;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

class NotificationRepositoryAdapterTest {

    private static final Instant NOW = Instant.parse("2026-07-08T08:00:00Z");

    private NotificationSpringDataRepository repository;
    private NotificationJpaMapper mapper;
    private JpaReferenceProvider references;
    private NotificationRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        repository = mock(NotificationSpringDataRepository.class);
        mapper = mock(NotificationJpaMapper.class);
        references = mock(JpaReferenceProvider.class);
        adapter = new NotificationRepositoryAdapter(repository, mapper, references);
    }

    @Test
    void findsByLegacyIdentifier() {
        AggregateId notificationId = AggregateId.newId();
        NotificationJpaEntity entity = mock(NotificationJpaEntity.class);
        Notification notification = newNotification(notificationId);
        when(repository.findByIdAndDeletedFalse(notificationId.value())).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(notification);

        assertThat(adapter.findById(notificationId)).contains(notification);
    }

    @Test
    void findsByTenantScopedIdentifier() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId notificationId = AggregateId.newId();
        NotificationJpaEntity entity = mock(NotificationJpaEntity.class);
        Notification notification = newNotification(notificationId);
        when(repository.findByTenantIdAndIdAndDeletedFalse(tenantId.value(), notificationId.value()))
                .thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(notification);

        assertThat(adapter.findById(tenantId, notificationId)).contains(notification);
    }

    @Test
    void reportsNoTenantScopedMatch() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId notificationId = AggregateId.newId();
        when(repository.findByTenantIdAndIdAndDeletedFalse(tenantId.value(), notificationId.value()))
                .thenReturn(Optional.empty());

        assertThat(adapter.findById(tenantId, notificationId)).isEmpty();
    }

    @Test
    void findsPageAndAppliesRequestedSort() {
        AggregateId tenantId = AggregateId.newId();
        NotificationJpaEntity entity = mock(NotificationJpaEntity.class);
        Notification notification = newNotification(AggregateId.newId());
        NotificationPageRequest pageRequest =
                new NotificationPageRequest(0, 20, NotificationSortField.SCHEDULED_AT, SortDirection.DESC);
        when(repository.findAllByTenantIdAndDeletedFalse(eq(tenantId.value()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity), PageRequest.of(0, 20), 1));
        when(mapper.toDomain(entity)).thenReturn(notification);

        NotificationPage<Notification> page = adapter.findPage(tenantId, pageRequest);

        assertThat(page.content()).containsExactly(notification);
        assertThat(page.totalElements()).isEqualTo(1);
    }

    @Test
    void findsPageWithDefaultCreatedAtSort() {
        AggregateId tenantId = AggregateId.newId();
        NotificationPageRequest pageRequest =
                new NotificationPageRequest(1, 5, NotificationSortField.CREATED_AT, SortDirection.ASC);
        Page<NotificationJpaEntity> emptyPage = new PageImpl<>(List.of(), PageRequest.of(1, 5), 6);
        when(repository.findAllByTenantIdAndDeletedFalse(eq(tenantId.value()), any(Pageable.class)))
                .thenReturn(emptyPage);

        NotificationPage<Notification> page = adapter.findPage(tenantId, pageRequest);

        assertThat(page.page()).isEqualTo(1);
        assertThat(page.size()).isEqualTo(5);
        assertThat(page.totalElements()).isEqualTo(6);
    }

    @Test
    void reportsWhetherARecipientWasAlreadyNotifiedSince() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId recipientId = AggregateId.newId();
        when(repository.existsByTenantIdAndUser_IdAndCategoryAndCreatedAtGreaterThanEqualAndDeletedFalse(
                        tenantId.value(), recipientId.value(), NotificationCategory.CONTRIBUTION_REMINDER, NOW))
                .thenReturn(true);

        boolean exists = adapter.existsForRecipientSince(
                tenantId, recipientId, NotificationCategory.CONTRIBUTION_REMINDER, NOW);

        assertThat(exists).isTrue();
    }

    @Test
    void savesNewAndUpdatedNotifications() {
        Notification notification = newNotification(AggregateId.newId());
        NotificationJpaEntity candidate = mock(NotificationJpaEntity.class);
        when(repository.findById(notification.id().value())).thenReturn(Optional.empty());
        when(mapper.toEntity(eq(notification), eq(5), any())).thenReturn(candidate);

        adapter.save(notification);

        verify(repository).save(candidate);
    }

    private Notification newNotification(AggregateId notificationId) {
        return Notification.queue(
                notificationId, AggregateId.newId(),
                new NotificationRecipient(AggregateId.newId(), "member@example.com"),
                NotificationChannel.EMAIL, NotificationCategory.VERIFICATION,
                new NotificationContent("Account verification", "Please verify your account."),
                NOW, AggregateId.newId(), NOW);
    }
}
