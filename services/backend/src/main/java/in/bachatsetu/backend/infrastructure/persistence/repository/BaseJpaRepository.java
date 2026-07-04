package in.bachatsetu.backend.infrastructure.persistence.repository;

import in.bachatsetu.backend.infrastructure.persistence.entity.BaseJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface BaseJpaRepository<E extends BaseJpaEntity>
        extends JpaRepository<E, UUID>, JpaSpecificationExecutor<E> {

    Optional<E> findByIdAndDeletedFalse(UUID id);

    Page<E> findAllByDeletedFalse(Pageable pageable);
}
