package in.bachatsetu.backend.infrastructure.persistence.repository.jpa;

import in.bachatsetu.backend.auth.domain.model.UserStatus;
import in.bachatsetu.backend.infrastructure.persistence.entity.identity.UserJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.repository.BaseJpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserSpringDataRepository extends BaseJpaRepository<UserJpaEntity> {

    Optional<UserJpaEntity> findByEmailIgnoreCaseAndDeletedFalse(String email);

    Optional<UserJpaEntity> findByPhoneNumberAndDeletedFalse(String phoneNumber);

    Optional<UserJpaEntity> findByTenantIdAndEmailIgnoreCaseAndDeletedFalse(UUID tenantId, String email);

    Optional<UserJpaEntity> findByTenantIdAndPhoneNumberAndDeletedFalse(UUID tenantId, String phoneNumber);

    Optional<UserJpaEntity> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);

    long countByDeletedFalse();

    long countByAuthenticationStatusAndDeletedFalse(UserStatus authenticationStatus);

    long countByTenantIdAndDeletedFalse(UUID tenantId);

    /** Every distinct tenant known to the platform — a tenant "exists" by having at least one user. */
    @Query(
            value = "SELECT DISTINCT userEntity.tenantId FROM UserJpaEntity userEntity "
                    + "WHERE userEntity.deleted = false ORDER BY userEntity.tenantId ASC",
            countQuery = "SELECT COUNT(DISTINCT userEntity.tenantId) FROM UserJpaEntity userEntity "
                    + "WHERE userEntity.deleted = false")
    Page<UUID> findDistinctTenantIds(Pageable pageable);

    /**
     * Cross-tenant, filterable listing for platform administration only — every other query on this
     * repository stays scoped to a single tenant.
     */
    @Query("""
            SELECT userEntity FROM UserJpaEntity userEntity
             WHERE userEntity.deleted = false
               AND (:status IS NULL OR userEntity.authenticationStatus = :status)
               AND (:email IS NULL OR LOWER(userEntity.email) LIKE LOWER(CONCAT('%', :email, '%')))
               AND (:phoneNumber IS NULL OR userEntity.phoneNumber LIKE CONCAT('%', :phoneNumber, '%'))
               AND (:createdAfter IS NULL OR userEntity.createdAt >= :createdAfter)
               AND (:createdBefore IS NULL OR userEntity.createdAt <= :createdBefore)
            """)
    Page<UserJpaEntity> searchAcrossTenants(
            @Param("status") UserStatus status,
            @Param("email") String email,
            @Param("phoneNumber") String phoneNumber,
            @Param("createdAfter") Instant createdAfter,
            @Param("createdBefore") Instant createdBefore,
            Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE UserJpaEntity userEntity
               SET userEntity.authenticationStatus = :status,
                   userEntity.updatedAt = :updatedAt,
                   userEntity.updatedBy = :updatedBy,
                   userEntity.version = userEntity.version + 1
             WHERE userEntity.id = :userId
               AND userEntity.deleted = false
            """)
    int updateAuthenticationStatus(
            @Param("userId") UUID userId,
            @Param("status") UserStatus status,
            @Param("updatedBy") UUID updatedBy,
            @Param("updatedAt") Instant updatedAt);

    @Query("SELECT COUNT(DISTINCT userEntity.tenantId) FROM UserJpaEntity userEntity WHERE userEntity.deleted = false")
    long countDistinctTenantIds();

    /** One row per calendar month: {@code [year, month, count]}, for platform analytics only. */
    @Query("""
            SELECT EXTRACT(YEAR FROM u.createdAt), EXTRACT(MONTH FROM u.createdAt), COUNT(u)
              FROM UserJpaEntity u
             WHERE u.deleted = false
             GROUP BY EXTRACT(YEAR FROM u.createdAt), EXTRACT(MONTH FROM u.createdAt)
             ORDER BY EXTRACT(YEAR FROM u.createdAt), EXTRACT(MONTH FROM u.createdAt)
            """)
    List<Object[]> findMonthlyRegistrationCounts();

    /** One row per language: {@code [PreferredLanguage, count]}, for platform analytics only. */
    @Query("SELECT u.preferredLanguage, COUNT(u) FROM UserJpaEntity u WHERE u.deleted = false "
            + "GROUP BY u.preferredLanguage")
    List<Object[]> findPreferredLanguageDistribution();

    /** One row per tenant: {@code [tenantId, count]}, for platform analytics only. */
    @Query("SELECT u.tenantId, COUNT(u) FROM UserJpaEntity u WHERE u.deleted = false GROUP BY u.tenantId")
    List<Object[]> findUserCountsByTenant();
}
