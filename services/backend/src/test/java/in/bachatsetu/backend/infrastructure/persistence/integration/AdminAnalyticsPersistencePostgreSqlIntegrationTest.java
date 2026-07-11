package in.bachatsetu.backend.infrastructure.persistence.integration;

import static in.bachatsetu.backend.group.domain.GroupDomainFixtures.NOW;
import static in.bachatsetu.backend.group.domain.GroupDomainFixtures.newGroup;
import static org.assertj.core.api.Assertions.assertThat;

import in.bachatsetu.backend.BachatSetuBackendApplication;
import in.bachatsetu.backend.admin.domain.analytics.model.GroupAnalytics;
import in.bachatsetu.backend.admin.domain.analytics.model.NotificationAnalytics;
import in.bachatsetu.backend.admin.domain.analytics.model.OverviewAnalytics;
import in.bachatsetu.backend.admin.domain.analytics.model.PaymentAnalytics;
import in.bachatsetu.backend.admin.domain.analytics.model.StorageAnalytics;
import in.bachatsetu.backend.admin.domain.analytics.model.UserAnalytics;
import in.bachatsetu.backend.admin.domain.analytics.port.GroupAnalyticsRepository;
import in.bachatsetu.backend.admin.domain.analytics.port.NotificationAnalyticsRepository;
import in.bachatsetu.backend.admin.domain.analytics.port.OverviewAnalyticsRepository;
import in.bachatsetu.backend.admin.domain.analytics.port.PaymentAnalyticsRepository;
import in.bachatsetu.backend.admin.domain.analytics.port.StorageAnalyticsRepository;
import in.bachatsetu.backend.admin.domain.analytics.port.UserAnalyticsRepository;
import in.bachatsetu.backend.group.application.port.SavingsGroupRepository;
import in.bachatsetu.backend.group.domain.model.SavingsGroup;
import in.bachatsetu.backend.infrastructure.persistence.audit.CurrentAuditorProvider;
import in.bachatsetu.backend.infrastructure.persistence.entity.community.GroupMemberJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.community.SavingsGroupJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.entity.identity.UserJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.MemberSpringDataRepository;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.SavingsGroupSpringDataRepository;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.StoredFileSpringDataRepository;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.UserSpringDataRepository;
import in.bachatsetu.backend.member.domain.model.GroupRole;
import in.bachatsetu.backend.member.domain.model.ParticipationStatus;
import in.bachatsetu.backend.notification.domain.model.Notification;
import in.bachatsetu.backend.notification.domain.model.NotificationCategory;
import in.bachatsetu.backend.notification.domain.model.NotificationChannel;
import in.bachatsetu.backend.notification.domain.model.NotificationContent;
import in.bachatsetu.backend.notification.domain.model.NotificationRecipient;
import in.bachatsetu.backend.notification.domain.port.NotificationRepository;
import in.bachatsetu.backend.payment.domain.model.IdempotencyKey;
import in.bachatsetu.backend.payment.domain.model.Payment;
import in.bachatsetu.backend.payment.domain.model.PaymentMethod;
import in.bachatsetu.backend.payment.domain.model.PaymentReference;
import in.bachatsetu.backend.payment.domain.model.ProviderReference;
import in.bachatsetu.backend.payment.domain.port.PaymentRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Money;
import in.bachatsetu.backend.storage.domain.model.StorageProvider;
import in.bachatsetu.backend.storage.domain.model.StoredFile;
import in.bachatsetu.backend.storage.domain.port.StorageRepository;
import in.bachatsetu.backend.user.domain.model.PreferredLanguage;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
        classes = BachatSetuBackendApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
            "spring.flyway.enabled=true",
            "spring.jpa.hibernate.ddl-auto=validate",
            "spring.data.redis.repositories.enabled=false",
            "bachatsetu.persistence.auditing.enabled=true",
            "bachatsetu.persistence.repositories.enabled=true"
        })
@Import(AdminAnalyticsPersistencePostgreSqlIntegrationTest.AdminAnalyticsPersistenceTestConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class AdminAnalyticsPersistencePostgreSqlIntegrationTest extends PostgreSqlIntegrationTest {

    private static final UUID AUDITOR_ID = UUID.fromString("f9100000-0000-0000-0000-000000000001");

    @Autowired
    private OverviewAnalyticsRepository overviewAnalyticsRepository;

    @Autowired
    private PaymentAnalyticsRepository paymentAnalyticsRepository;

    @Autowired
    private GroupAnalyticsRepository groupAnalyticsRepository;

    @Autowired
    private UserAnalyticsRepository userAnalyticsRepository;

    @Autowired
    private NotificationAnalyticsRepository notificationAnalyticsRepository;

    @Autowired
    private StorageAnalyticsRepository storageAnalyticsRepository;

    @Autowired
    private SavingsGroupRepository savingsGroupRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private StorageRepository storageRepository;

    @Autowired
    private UserSpringDataRepository userSpringDataRepository;

    @Autowired
    private SavingsGroupSpringDataRepository savingsGroupSpringDataRepository;

    @Autowired
    private MemberSpringDataRepository memberSpringDataRepository;

    @Autowired
    private StoredFileSpringDataRepository storedFileSpringDataRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @Transactional
    void computesOverviewAndUserAnalyticsAcrossTenants() {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        saveUser(tenantA, "overview-a@example.in", "+919100000001", true);
        saveUser(tenantA, "overview-b@example.in", "+919100000002", true);
        saveUser(tenantB, "overview-c@example.in", "+919100000003", false);
        flushAndClear();

        OverviewAnalytics overview = overviewAnalyticsRepository.compute();
        UserAnalytics userAnalytics = userAnalyticsRepository.compute();

        assertThat(overview.totalUsers()).isGreaterThanOrEqualTo(3);
        assertThat(overview.activeUsers()).isGreaterThanOrEqualTo(2);
        assertThat(overview.totalTenants()).isGreaterThanOrEqualTo(2);
        assertThat(userAnalytics.totalUsers()).isGreaterThanOrEqualTo(3);
        assertThat(userAnalytics.preferredLanguageDistribution()).isNotEmpty();
        assertThat(userAnalytics.usersPerTenant()).anySatisfy(
                tenantCount -> assertThat(tenantCount.userCount()).isGreaterThanOrEqualTo(0));
    }

    @Test
    @Transactional
    void computesGroupAnalyticsIncludingAverageMembersAndContribution() {
        AggregateId organizerId = AggregateId.newId();
        saveUser(UUID.randomUUID(), "organizer-analytics@example.in", "+919100000010", true);
        SavingsGroup activeGroup = newGroup(organizerId, 5);
        activeGroup.activate(organizerId, NOW);
        savingsGroupRepository.save(activeGroup);
        SavingsGroup closedGroup = newGroup(organizerId, 5);
        closedGroup.close(organizerId, NOW);
        savingsGroupRepository.save(closedGroup);
        flushAndClear();

        SavingsGroupJpaEntity activeGroupEntity =
                savingsGroupSpringDataRepository.findById(activeGroup.id().value()).orElseThrow();
        UserJpaEntity member = saveUser(
                activeGroup.tenantId().value(), "member-analytics@example.in", "+919100000011", true);
        memberSpringDataRepository.save(new GroupMemberJpaEntity(
                UUID.randomUUID(), activeGroup.tenantId().value(), activeGroupEntity, member,
                "MB-ANALYTICS0001", GroupRole.MEMBER, ParticipationStatus.ACTIVE, NOW, null));
        flushAndClear();

        GroupAnalytics groupAnalytics = groupAnalyticsRepository.compute();

        assertThat(groupAnalytics.totalGroups()).isGreaterThanOrEqualTo(2);
        assertThat(groupAnalytics.activeGroups()).isGreaterThanOrEqualTo(1);
        assertThat(groupAnalytics.completedGroups()).isGreaterThanOrEqualTo(1);
        assertThat(groupAnalytics.averageMembersPerGroup()).isGreaterThan(0.0);
        assertThat(groupAnalytics.averageContributionAmountPaise()).isGreaterThan(0.0);
        assertThat(groupAnalytics.drawCompletionRate()).isGreaterThanOrEqualTo(0.0);
    }

    @Test
    @Transactional
    void computesPaymentAnalyticsIncludingRatesAndTrend() {
        AggregateId organizerId = AggregateId.newId();
        SavingsGroup group = newGroup(organizerId, 5);
        group.activate(organizerId, NOW);
        savingsGroupRepository.save(group);
        saveUser(group.tenantId().value(), "payer-analytics@example.in", "+919100000020", true);
        flushAndClear();

        AggregateId payerId = AggregateId.newId();
        Payment verifiedPayment = Payment.initiate(
                AggregateId.newId(), group.tenantId(), group.id(), payerId,
                new PaymentReference("PAY-ANALYTICS0001"), new IdempotencyKey("analytics-attempt-0001"),
                Money.inr(500_000), PaymentMethod.UPI, group.organizerId(), NOW);
        verifiedPayment.verify(new ProviderReference("razorpay", "txn-analytics-1"), group.organizerId(), NOW);
        paymentRepository.save(verifiedPayment);

        Payment failedPayment = Payment.initiate(
                AggregateId.newId(), group.tenantId(), group.id(), payerId,
                new PaymentReference("PAY-ANALYTICS0002"), new IdempotencyKey("analytics-attempt-0002"),
                Money.inr(250_000), PaymentMethod.UPI, group.organizerId(), NOW);
        failedPayment.fail("PROVIDER_DECLINED", group.organizerId(), NOW);
        paymentRepository.save(failedPayment);
        flushAndClear();

        PaymentAnalytics paymentAnalytics = paymentAnalyticsRepository.compute();

        assertThat(paymentAnalytics.totalPaymentVolumePaise()).isGreaterThanOrEqualTo(500_000L);
        assertThat(paymentAnalytics.verifiedPaymentVolumePaise()).isGreaterThanOrEqualTo(500_000L);
        assertThat(paymentAnalytics.failedPaymentCount()).isGreaterThanOrEqualTo(1);
        assertThat(paymentAnalytics.paymentSuccessRate()).isBetween(0.0, 1.0);
        assertThat(paymentAnalytics.paymentFailureRate()).isBetween(0.0, 1.0);
        assertThat(paymentAnalytics.paymentTrend()).isNotEmpty();
    }

    @Test
    @Transactional
    void computesNotificationAnalyticsIncludingUnreadApproximation() {
        UUID tenantId = UUID.randomUUID();
        AggregateId recipientId = AggregateId.newId();
        saveUser(tenantId, "recipient-analytics@example.in", "+919100000030", true);

        Notification delivered = Notification.queue(
                AggregateId.newId(), new AggregateId(tenantId),
                new NotificationRecipient(recipientId, "recipient-analytics@example.in"),
                NotificationChannel.EMAIL, NotificationCategory.PAYMENT,
                new NotificationContent("Payment received", "Your payment was verified."),
                NOW, recipientId, NOW);
        delivered.startDelivery(recipientId, NOW.plusSeconds(1));
        delivered.markSent("provider-msg-analytics-1", recipientId, NOW.plusSeconds(2));
        delivered.markDelivered(recipientId, NOW.plusSeconds(3));
        notificationRepository.save(delivered);

        Notification queued = Notification.queue(
                AggregateId.newId(), new AggregateId(tenantId),
                new NotificationRecipient(recipientId, "recipient-analytics@example.in"),
                NotificationChannel.SMS, NotificationCategory.GROUP_UPDATE,
                new NotificationContent("Group update", "Your group status changed."),
                NOW, recipientId, NOW);
        notificationRepository.save(queued);
        flushAndClear();

        NotificationAnalytics notificationAnalytics = notificationAnalyticsRepository.compute();

        assertThat(notificationAnalytics.totalNotifications()).isGreaterThanOrEqualTo(2);
        assertThat(notificationAnalytics.unreadNotifications()).isGreaterThanOrEqualTo(1);
        assertThat(notificationAnalytics.deliveryStatusCounts()).isNotEmpty();
        assertThat(notificationAnalytics.notificationTypeDistribution()).isNotEmpty();
    }

    @Test
    @Transactional
    void computesStorageAnalyticsIncludingAverageSizeAndTrend() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId actorId = AggregateId.newId();
        StoredFile fileOne = StoredFile.upload(
                AggregateId.newId(), tenantId, StorageProvider.LOCAL, "/data/storage/tenant/analytics-1",
                "receipt-1.pdf", "application/pdf", 2048L, "checksum-analytics-1", actorId, NOW);
        StoredFile fileTwo = StoredFile.upload(
                AggregateId.newId(), tenantId, StorageProvider.LOCAL, "/data/storage/tenant/analytics-2",
                "receipt-2.pdf", "application/pdf", 4096L, "checksum-analytics-2", actorId, NOW);
        storageRepository.save(fileOne);
        storageRepository.save(fileTwo);
        flushAndClear();

        StorageAnalytics storageAnalytics = storageAnalyticsRepository.compute();

        assertThat(storageAnalytics.totalFiles()).isGreaterThanOrEqualTo(2);
        assertThat(storageAnalytics.totalStorageBytes()).isGreaterThanOrEqualTo(6144L);
        assertThat(storageAnalytics.averageFileSizeBytes()).isGreaterThan(0.0);
        assertThat(storageAnalytics.storageProviderDistribution()).isNotEmpty();
        assertThat(storageAnalytics.uploadsPerDay()).isNotEmpty();
    }

    private UserJpaEntity saveUser(UUID tenantId, String email, String phoneNumber, boolean active) {
        UserJpaEntity entity = new UserJpaEntity(
                UUID.randomUUID(), tenantId, "Test", "User", email, phoneNumber,
                in.bachatsetu.backend.user.domain.model.UserStatus.ACTIVE, PreferredLanguage.ENGLISH);
        entity.updateAuthentication(
                email,
                "hash",
                active ? in.bachatsetu.backend.auth.domain.model.UserStatus.ACTIVE
                        : in.bachatsetu.backend.auth.domain.model.UserStatus.DISABLED,
                Set.of());
        return userSpringDataRepository.save(entity);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class AdminAnalyticsPersistenceTestConfiguration {

        @Bean
        CurrentAuditorProvider currentAuditorProvider() {
            return () -> Optional.of(AUDITOR_ID);
        }
    }
}
