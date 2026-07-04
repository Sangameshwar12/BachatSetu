package in.bachatsetu.backend.infrastructure.persistence.repository;

import in.bachatsetu.backend.infrastructure.persistence.entity.BaseJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

@NoRepositoryBean
public interface ReadOnlyJpaRepository<E extends BaseJpaEntity> extends Repository<E, UUID> {

    Optional<E> findById(UUID id);

    Optional<E> findByIdAndDeletedFalse(UUID id);

    boolean existsById(UUID id);

    Page<E> findAll(Pageable pageable);

    Page<E> findAllByDeletedFalse(Pageable pageable);

    long count();
}
