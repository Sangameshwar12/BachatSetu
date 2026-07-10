package in.bachatsetu.backend.infrastructure.persistence.adapter;

import in.bachatsetu.backend.infrastructure.persistence.entity.identity.UserJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.NotificationSpringDataRepository;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.PaymentSpringDataRepository;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.SavingsGroupSpringDataRepository;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.StoredFileSpringDataRepository;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.UserSpringDataRepository;
import in.bachatsetu.backend.payment.domain.model.PaymentStatus;
import in.bachatsetu.backend.platformoperations.domain.model.TenantStatistics;
import in.bachatsetu.backend.platformoperations.domain.port.TenantStatisticsRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Computes one tenant's totals on demand, composing every existing module's own Spring Data repository — no
 * dedicated tenant analytics table. {@code lastActivityAt} is a proxy: the most recently updated user record
 * in the tenant (see {@link TenantStatistics}'s Javadoc).
 */
@Repository
@ConditionalOnPersistenceRepositories
@Transactional(readOnly = true)
public class TenantStatisticsRepositoryAdapter implements TenantStatisticsRepository {

    private final UserSpringDataRepository userRepository;
    private final SavingsGroupSpringDataRepository groupRepository;
    private final PaymentSpringDataRepository paymentRepository;
    private final StoredFileSpringDataRepository storedFileRepository;
    private final NotificationSpringDataRepository notificationRepository;

    public TenantStatisticsRepositoryAdapter(
            UserSpringDataRepository userRepository,
            SavingsGroupSpringDataRepository groupRepository,
            PaymentSpringDataRepository paymentRepository,
            StoredFileSpringDataRepository storedFileRepository,
            NotificationSpringDataRepository notificationRepository) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.groupRepository = Objects.requireNonNull(groupRepository, "groupRepository must not be null");
        this.paymentRepository = Objects.requireNonNull(paymentRepository, "paymentRepository must not be null");
        this.storedFileRepository = Objects.requireNonNull(storedFileRepository, "storedFileRepository must not be null");
        this.notificationRepository = Objects.requireNonNull(notificationRepository, "notificationRepository must not be null");
    }

    @Override
    public TenantStatistics computeFor(AggregateId tenantId) {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        UUID id = tenantId.value();
        Instant lastActivityAt = userRepository.findFirstByTenantIdAndDeletedFalseOrderByUpdatedAtDesc(id)
                .map(UserJpaEntity::getUpdatedAt)
                .orElse(null);
        return new TenantStatistics(
                userRepository.countByTenantIdAndDeletedFalse(id),
                groupRepository.countByTenantIdAndDeletedFalse(id),
                paymentRepository.countByTenantIdAndDeletedFalse(id),
                paymentRepository.sumAmountPaiseByTenantIdAndStatus(id, PaymentStatus.VERIFIED),
                storedFileRepository.countByTenantIdAndDeletedFalse(id),
                storedFileRepository.sumSizeByTenantId(id),
                notificationRepository.countByTenantIdAndDeletedFalse(id),
                lastActivityAt);
    }
}
