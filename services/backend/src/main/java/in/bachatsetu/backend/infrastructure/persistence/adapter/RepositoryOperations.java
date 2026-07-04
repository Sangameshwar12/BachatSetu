package in.bachatsetu.backend.infrastructure.persistence.adapter;

import in.bachatsetu.backend.infrastructure.persistence.entity.BaseJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.exception.PersistenceConflictException;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;

final class RepositoryOperations {

    private RepositoryOperations() {
    }

    static <T> T execute(Supplier<T> operation) {
        try {
            return operation.get();
        } catch (OptimisticLockingFailureException exception) {
            throw new PersistenceConflictException("concurrent persistence update detected", exception);
        } catch (DataIntegrityViolationException exception) {
            throw new PersistenceConflictException("persistence constraint was violated", exception);
        }
    }

    static <E extends BaseJpaEntity> E preserveState(E candidate, Optional<E> existing) {
        existing.ifPresent(candidate::copyPersistenceStateFrom);
        return candidate;
    }
}
