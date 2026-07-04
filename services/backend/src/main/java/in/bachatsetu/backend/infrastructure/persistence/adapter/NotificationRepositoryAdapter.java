package in.bachatsetu.backend.infrastructure.persistence.adapter;

import in.bachatsetu.backend.infrastructure.persistence.entity.notification.NotificationJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.mapper.JpaReferenceProvider;
import in.bachatsetu.backend.infrastructure.persistence.mapper.NotificationJpaMapper;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.NotificationSpringDataRepository;
import in.bachatsetu.backend.notification.domain.model.Notification;
import in.bachatsetu.backend.notification.domain.port.NotificationRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Optional;
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
