package in.bachatsetu.backend.infrastructure.persistence.adapter;

import in.bachatsetu.backend.infrastructure.persistence.entity.platform.AnnouncementJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.mapper.AnnouncementJpaMapper;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.AnnouncementSpringDataRepository;
import in.bachatsetu.backend.platformoperations.domain.model.Announcement;
import in.bachatsetu.backend.platformoperations.domain.port.AnnouncementRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Page;
import in.bachatsetu.backend.shared.domain.PageQuery;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@ConditionalOnPersistenceRepositories
@Transactional(readOnly = true)
public class AnnouncementRepositoryAdapter implements AnnouncementRepository {

    private final AnnouncementSpringDataRepository repository;
    private final AnnouncementJpaMapper mapper;

    public AnnouncementRepositoryAdapter(AnnouncementSpringDataRepository repository, AnnouncementJpaMapper mapper) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    @Transactional
    public void save(Announcement announcement) {
        RepositoryOperations.execute(() -> {
            Optional<AnnouncementJpaEntity> existing = repository.findById(announcement.id().value());
            AnnouncementJpaEntity candidate = mapper.toEntity(announcement);
            repository.save(RepositoryOperations.preserveState(candidate, existing));
            return null;
        });
    }

    @Override
    public Optional<Announcement> findById(AggregateId announcementId) {
        return repository.findByIdAndDeletedFalse(announcementId.value()).map(mapper::toDomain);
    }

    @Override
    public Page<Announcement> findAll(PageQuery pageQuery) {
        org.springframework.data.domain.Page<AnnouncementJpaEntity> page = repository.findAllByDeletedFalse(
                PageRequest.of(pageQuery.page(), pageQuery.size(), Sort.by(Sort.Direction.DESC, "createdAt")));
        return new Page<>(
                page.getContent().stream().map(mapper::toDomain).toList(), pageQuery.page(), pageQuery.size(),
                page.getTotalElements());
    }

    @Override
    public List<Announcement> findActive(Instant now) {
        return repository.findActive(now).stream().map(mapper::toDomain).toList();
    }
}
