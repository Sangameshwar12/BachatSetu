package in.bachatsetu.backend.infrastructure.persistence.adapter;

import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.MemberSpringDataRepository;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.NotificationSpringDataRepository;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.PaymentSpringDataRepository;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.ReceiptSpringDataRepository;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.SavingsGroupSpringDataRepository;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.StoredFileSpringDataRepository;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.TenantSpringDataRepository;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.UserSpringDataRepository;
import in.bachatsetu.backend.member.domain.model.GroupRole;
import in.bachatsetu.backend.payment.domain.model.PaymentStatus;
import in.bachatsetu.backend.platformoperations.domain.model.PlatformOverviewSnapshot;
import in.bachatsetu.backend.platformoperations.domain.model.TenantStatus;
import in.bachatsetu.backend.platformoperations.domain.port.PlatformOverviewRepository;
import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Computes the platform-wide dashboard snapshot from each existing module's own Spring Data repository — no
 * SQL view, no caching, no scheduled aggregation.
 */
@Repository
@ConditionalOnPersistenceRepositories
@Transactional(readOnly = true)
public class PlatformOverviewRepositoryAdapter implements PlatformOverviewRepository {

    private final UserSpringDataRepository userRepository;
    private final SavingsGroupSpringDataRepository groupRepository;
    private final MemberSpringDataRepository memberRepository;
    private final PaymentSpringDataRepository paymentRepository;
    private final ReceiptSpringDataRepository receiptRepository;
    private final NotificationSpringDataRepository notificationRepository;
    private final StoredFileSpringDataRepository storedFileRepository;
    private final TenantSpringDataRepository tenantRepository;

    public PlatformOverviewRepositoryAdapter(
            UserSpringDataRepository userRepository,
            SavingsGroupSpringDataRepository groupRepository,
            MemberSpringDataRepository memberRepository,
            PaymentSpringDataRepository paymentRepository,
            ReceiptSpringDataRepository receiptRepository,
            NotificationSpringDataRepository notificationRepository,
            StoredFileSpringDataRepository storedFileRepository,
            TenantSpringDataRepository tenantRepository) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.groupRepository = Objects.requireNonNull(groupRepository, "groupRepository must not be null");
        this.memberRepository = Objects.requireNonNull(memberRepository, "memberRepository must not be null");
        this.paymentRepository = Objects.requireNonNull(paymentRepository, "paymentRepository must not be null");
        this.receiptRepository = Objects.requireNonNull(receiptRepository, "receiptRepository must not be null");
        this.notificationRepository =
                Objects.requireNonNull(notificationRepository, "notificationRepository must not be null");
        this.storedFileRepository = Objects.requireNonNull(storedFileRepository, "storedFileRepository must not be null");
        this.tenantRepository = Objects.requireNonNull(tenantRepository, "tenantRepository must not be null");
    }

    @Override
    public PlatformOverviewSnapshot compute(Instant todayStart, Instant todayEnd) {
        Objects.requireNonNull(todayStart, "todayStart must not be null");
        Objects.requireNonNull(todayEnd, "todayEnd must not be null");
        long knownTenants = userRepository.countDistinctTenantIds();
        long inactiveTenants = tenantRepository.countByStatusAndDeletedFalse(TenantStatus.SUSPENDED)
                + tenantRepository.countByStatusAndDeletedFalse(TenantStatus.ARCHIVED);
        return new PlatformOverviewSnapshot(
                userRepository.countByDeletedFalse(),
                memberRepository.countDistinctUsersByRoleAndDeletedFalse(GroupRole.ORGANIZER),
                groupRepository.countByDeletedFalse(),
                memberRepository.countByDeletedFalse(),
                paymentRepository.countByDeletedFalse(),
                receiptRepository.countByDeletedFalse(),
                notificationRepository.countByDeletedFalse(),
                storedFileRepository.countByDeletedFalse(),
                Math.max(0, knownTenants - inactiveTenants),
                paymentRepository.sumAmountPaiseByStatus(PaymentStatus.VERIFIED),
                userRepository.countByCreatedAtBetween(todayStart, todayEnd),
                paymentRepository.countByCreatedAtBetween(todayStart, todayEnd),
                groupRepository.countByCreatedAtBetween(todayStart, todayEnd),
                notificationRepository.countByCreatedAtBetween(todayStart, todayEnd),
                storedFileRepository.countByUploadedAtBetween(todayStart, todayEnd));
    }
}
