package in.bachatsetu.backend.infrastructure.persistence.adapter;

import in.bachatsetu.backend.admin.domain.analytics.model.OverviewAnalytics;
import in.bachatsetu.backend.admin.domain.analytics.port.OverviewAnalyticsRepository;
import in.bachatsetu.backend.auth.domain.model.UserStatus;
import in.bachatsetu.backend.group.domain.model.GroupStatus;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.NotificationSpringDataRepository;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.PaymentSpringDataRepository;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.ReceiptSpringDataRepository;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.SavingsGroupSpringDataRepository;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.StoredFileSpringDataRepository;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.UserSpringDataRepository;
import in.bachatsetu.backend.payment.domain.model.PaymentStatus;
import java.util.Objects;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Computes the platform-wide overview snapshot from each existing module's own Spring Data repository — no
 * SQL view, no caching, no scheduled aggregation.
 */
@Repository
@ConditionalOnPersistenceRepositories
@Transactional(readOnly = true)
public class AdminOverviewAnalyticsRepositoryAdapter implements OverviewAnalyticsRepository {

    private final UserSpringDataRepository userRepository;
    private final SavingsGroupSpringDataRepository groupRepository;
    private final PaymentSpringDataRepository paymentRepository;
    private final ReceiptSpringDataRepository receiptRepository;
    private final NotificationSpringDataRepository notificationRepository;
    private final StoredFileSpringDataRepository storedFileRepository;

    public AdminOverviewAnalyticsRepositoryAdapter(
            UserSpringDataRepository userRepository,
            SavingsGroupSpringDataRepository groupRepository,
            PaymentSpringDataRepository paymentRepository,
            ReceiptSpringDataRepository receiptRepository,
            NotificationSpringDataRepository notificationRepository,
            StoredFileSpringDataRepository storedFileRepository) {
        this.userRepository = Objects.requireNonNull(userRepository, "user repository must not be null");
        this.groupRepository = Objects.requireNonNull(groupRepository, "group repository must not be null");
        this.paymentRepository = Objects.requireNonNull(paymentRepository, "payment repository must not be null");
        this.receiptRepository = Objects.requireNonNull(receiptRepository, "receipt repository must not be null");
        this.notificationRepository =
                Objects.requireNonNull(notificationRepository, "notification repository must not be null");
        this.storedFileRepository =
                Objects.requireNonNull(storedFileRepository, "stored file repository must not be null");
    }

    @Override
    public OverviewAnalytics compute() {
        long totalUsers = userRepository.countByDeletedFalse();
        long activeUsers = userRepository.countByAuthenticationStatusAndDeletedFalse(UserStatus.ACTIVE);
        return new OverviewAnalytics(
                totalUsers,
                activeUsers,
                totalUsers - activeUsers,
                userRepository.countDistinctTenantIds(),
                groupRepository.countByDeletedFalse(),
                groupRepository.countByStatusAndDeletedFalse(GroupStatus.ACTIVE),
                groupRepository.countByStatusAndDeletedFalse(GroupStatus.CLOSED),
                paymentRepository.countByDeletedFalse(),
                paymentRepository.countByStatusAndDeletedFalse(PaymentStatus.VERIFIED),
                paymentRepository.countByStatusAndDeletedFalse(PaymentStatus.FAILED),
                receiptRepository.countByDeletedFalse(),
                notificationRepository.countByDeletedFalse(),
                storedFileRepository.countByDeletedFalse());
    }
}
