package in.bachatsetu.backend.infrastructure.persistence.adapter;

import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.UserSpringDataRepository;
import in.bachatsetu.backend.platformoperations.domain.port.KnownTenantsRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Page;
import in.bachatsetu.backend.shared.domain.PageQuery;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Derives tenant existence from the distinct tenant identifiers already recorded on users — the same
 * underlying query {@code AdminTenantRepositoryAdapter} uses, kept as an independent adapter (rather than a
 * shared domain-port dependency) to avoid a module cycle between platformoperations and admin.
 */
@Repository
@ConditionalOnPersistenceRepositories
@Transactional(readOnly = true)
public class KnownTenantsRepositoryAdapter implements KnownTenantsRepository {

    private final UserSpringDataRepository userRepository;

    public KnownTenantsRepositoryAdapter(UserSpringDataRepository userRepository) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
    }

    @Override
    public Page<AggregateId> listKnownTenantIds(PageQuery pageQuery) {
        org.springframework.data.domain.Page<UUID> page = userRepository.findDistinctTenantIds(
                PageRequest.of(pageQuery.page(), pageQuery.size()));
        return new Page<>(
                page.getContent().stream().map(AggregateId::new).toList(), pageQuery.page(), pageQuery.size(),
                page.getTotalElements());
    }
}
