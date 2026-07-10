package in.bachatsetu.backend.infrastructure.persistence.adapter;

import in.bachatsetu.backend.infrastructure.persistence.entity.notification.NotificationJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.mapper.JpaReferenceProvider;
import in.bachatsetu.backend.infrastructure.persistence.mapper.NotificationJpaMapper;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.NotificationSpringDataRepository;
import in.bachatsetu.backend.notification.domain.model.Notification;
import in.bachatsetu.backend.notification.domain.model.NotificationCategory;
import in.bachatsetu.backend.notification.domain.port.NotificationPage;
import in.bachatsetu.backend.notification.domain.port.NotificationPageRequest;
import in.bachatsetu.backend.notification.domain.port.NotificationRepository;
import in.bachatsetu.backend.notification.domain.port.NotificationSortField;
import in.bachatsetu.backend.notification.domain.port.SortDirection;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@ConditionalOnPersistenceRepositories
@Transactional(readOnly = true)
public class NotificationRepositoryAdapter implements NotificationRepository {

    private static final int DEFAULT_PRIORITY = 5;

    private final NotificationSpringDataRepository repository;
    private final NotificationJpaMapper mapper;
    private final JpaReferenceProvider references;

    public NotificationRepositoryAdapter(
            NotificationSpringDataRepository repository,
            NotificationJpaMapper mapper,
            JpaReferenceProvider references) {
        this.repository = repository;
        this.mapper = mapper;
        this.references = references;
    }

    @Override
    public Optional<Notification> findById(AggregateId notificationId) {
        return repository.findByIdAndDeletedFalse(notificationId.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<Notification> findById(AggregateId tenantId, AggregateId notificationId) {
        return repository.findByTenantIdAndIdAndDeletedFalse(tenantId.value(), notificationId.value())
                .map(mapper::toDomain);
    }

    @Override
    public NotificationPage<Notification> findPage(AggregateId tenantId, NotificationPageRequest pageRequest) {
        Pageable pageable = PageRequest.of(pageRequest.page(), pageRequest.size(), toSort(pageRequest));
        Page<NotificationJpaEntity> page = repository.findAllByTenantIdAndDeletedFalse(tenantId.value(), pageable);
        List<Notification> content = page.getContent().stream().map(mapper::toDomain).toList();
        return new NotificationPage<>(content, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    private Sort toSort(NotificationPageRequest pageRequest) {
        String property = toSortProperty(pageRequest.sortField());
        Sort.Direction direction = pageRequest.direction() == SortDirection.DESC
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return Sort.by(direction, property);
    }

    private String toSortProperty(NotificationSortField sortField) {
        if (sortField == NotificationSortField.SCHEDULED_AT) {
            return "scheduledAt";
        }
        return "createdAt";
    }

    @Override
    public boolean existsForRecipientSince(
            AggregateId tenantId, AggregateId recipientUserId, NotificationCategory category, Instant since) {
        return repository.existsByTenantIdAndUser_IdAndCategoryAndCreatedAtGreaterThanEqualAndDeletedFalse(
                tenantId.value(), recipientUserId.value(), category, since);
    }

    @Override
    public List<Notification> findRecentForRecipient(AggregateId tenantId, AggregateId recipientUserId) {
        return repository.findTop5ByTenantIdAndUser_IdAndDeletedFalseOrderByCreatedAtDesc(
                        tenantId.value(), recipientUserId.value())
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional
    public void save(Notification notification) {
        RepositoryOperations.execute(() -> {
            Optional<NotificationJpaEntity> existing = repository.findById(notification.id().value());
            NotificationJpaEntity candidate = mapper.toEntity(notification, DEFAULT_PRIORITY, references);
            repository.save(RepositoryOperations.preserveState(candidate, existing));
            return null;
        });
    }
}
