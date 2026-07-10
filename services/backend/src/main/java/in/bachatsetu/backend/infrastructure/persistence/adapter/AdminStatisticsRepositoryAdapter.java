package in.bachatsetu.backend.infrastructure.persistence.adapter;

import in.bachatsetu.backend.admin.domain.model.PlatformStatistics;
import in.bachatsetu.backend.admin.domain.port.PlatformStatisticsRepository;
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
 * Computes platform-wide totals on demand by delegating a handful of {@code count} queries to each
 * existing module's own Spring Data repository — no SQL view, no materialized view, no caching.
 */
@Repository
@ConditionalOnPersistenceRepositories
@Transactional(readOnly = true)
public class AdminStatisticsRepositoryAdapter implements PlatformStatisticsRepository {

    private final UserSpringDataRepository userRepository;
    private final SavingsGroupSpringDataRepository groupRepository;
    private final PaymentSpringDataRepository paymentRepository;
    private final ReceiptSpringDataRepository receiptRepository;
    private final NotificationSpringDataRepository notificationRepository;
    private final StoredFileSpringDataRepository storedFileRepository;

    public AdminStatisticsRepositoryAdapter(
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
    public PlatformStatistics compute() {
        return new PlatformStatistics(
                userRepository.countByDeletedFalse(),
                userRepository.countByAuthenticationStatusAndDeletedFalse(UserStatus.ACTIVE),
                userRepository.countByAuthenticationStatusAndDeletedFalse(UserStatus.DISABLED),
                groupRepository.countByDeletedFalse(),
                groupRepository.countByStatusAndDeletedFalse(GroupStatus.ACTIVE),
                paymentRepository.countByDeletedFalse(),
                paymentRepository.countByStatusAndDeletedFalse(PaymentStatus.VERIFIED),
                receiptRepository.countByDeletedFalse(),
                notificationRepository.countByDeletedFalse(),
                storedFileRepository.countByDeletedFalse());
    }
}
