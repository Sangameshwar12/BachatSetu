package in.bachatsetu.backend.infrastructure.persistence.adapter;

import in.bachatsetu.backend.group.domain.model.GroupCode;
import in.bachatsetu.backend.group.domain.model.SavingsGroup;
import in.bachatsetu.backend.group.domain.port.GroupRepository;
import in.bachatsetu.backend.infrastructure.persistence.entity.community.GroupJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.mapper.GroupJpaMapper;
import in.bachatsetu.backend.infrastructure.persistence.mapper.JpaReferenceProvider;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.GroupSpringDataRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@ConditionalOnPersistenceRepositories
@Transactional(readOnly = true)
public class GroupRepositoryAdapter implements GroupRepository {

    private final GroupSpringDataRepository repository;
    private final GroupJpaMapper mapper;
    private final JpaReferenceProvider references;

    public GroupRepositoryAdapter(
            GroupSpringDataRepository repository,
            GroupJpaMapper mapper,
            JpaReferenceProvider references) {
        this.repository = repository;
        this.mapper = mapper;
        this.references = references;
    }

    @Override
    public Optional<SavingsGroup> findById(AggregateId groupId) {
        return repository.findByIdAndDeletedFalse(groupId.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<SavingsGroup> findByCode(AggregateId tenantId, GroupCode code) {
        return repository.findByTenantIdAndCodeAndDeletedFalse(tenantId.value(), code.value())
                .map(mapper::toDomain);
    }

    @Override
    @Transactional
    public void save(SavingsGroup group) {
        RepositoryOperations.execute(() -> {
            Optional<GroupJpaEntity> existing = repository.findById(group.id().value());
            GroupJpaEntity candidate = mapper.toEntity(group, references);
            repository.save(RepositoryOperations.preserveState(candidate, existing));
            return null;
        });
    }
}
