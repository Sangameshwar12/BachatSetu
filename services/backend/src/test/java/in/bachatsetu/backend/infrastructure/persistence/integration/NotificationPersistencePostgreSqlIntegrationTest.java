package in.bachatsetu.backend.infrastructure.persistence.integration;

import static org.assertj.core.api.Assertions.assertThat;

import in.bachatsetu.backend.BachatSetuBackendApplication;
import in.bachatsetu.backend.infrastructure.persistence.audit.CurrentAuditorProvider;
import in.bachatsetu.backend.infrastructure.persistence.entity.identity.UserJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.UserSpringDataRepository;
import in.bachatsetu.backend.notification.domain.model.Notification;
import in.bachatsetu.backend.notification.domain.model.NotificationCategory;
import in.bachatsetu.backend.notification.domain.model.NotificationChannel;
import in.bachatsetu.backend.notification.domain.model.NotificationContent;
import in.bachatsetu.backend.notification.domain.model.NotificationRecipient;
import in.bachatsetu.backend.notification.domain.port.NotificationPage;
import in.bachatsetu.backend.notification.domain.port.NotificationPageRequest;
import in.bachatsetu.backend.notification.domain.port.NotificationRepository;
import in.bachatsetu.backend.notification.domain.port.NotificationSortField;
import in.bachatsetu.backend.notification.domain.port.SortDirection;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.user.domain.model.PreferredLanguage;
import in.bachatsetu.backend.user.domain.model.UserStatus;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.Optional;
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
@Import(NotificationPersistencePostgreSqlIntegrationTest.NotificationPersistenceTestConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class NotificationPersistencePostgreSqlIntegrationTest extends PostgreSqlIntegrationTest {

    private static final UUID AUDITOR_ID = UUID.fromString("f6000000-0000-0000-0000-000000000001");
    private static final Instant NOW = Instant.parse("2026-07-08T08:00:00Z");

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserSpringDataRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @Transactional
    void persistsAndRehydratesANotificationThroughItsFullLifecycle() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId recipientId = AggregateId.newId();
        persistUser(recipientId, tenantId, "recipient@example.in");

        AggregateId notificationId = AggregateId.newId();
        Notification notification = Notification.queue(
                notificationId, tenantId,
                new NotificationRecipient(recipientId, "recipient@example.in"),
                NotificationChannel.EMAIL, NotificationCategory.VERIFICATION,
                new NotificationContent("Account verification", "Please verify your account."),
                NOW, recipientId, NOW);
        notificationRepository.save(notification);
        flushAndClear();

        Notification queued = notificationRepository.findById(tenantId, notificationId).orElseThrow();
        assertThat(queued.status().name()).isEqualTo("QUEUED");
        assertThat(queued.attempts()).isEmpty();

        queued.startDelivery(recipientId, NOW.plusSeconds(1));
        queued.markSent("provider-msg-1", recipientId, NOW.plusSeconds(2));
        notificationRepository.save(queued);
        flushAndClear();

        Notification sent = notificationRepository.findById(tenantId, notificationId).orElseThrow();
        assertThat(sent.status().name()).isEqualTo("SENT");

        sent.markDelivered(recipientId, NOW.plusSeconds(3));
        notificationRepository.save(sent);
        flushAndClear();

        Notification delivered = notificationRepository.findById(tenantId, notificationId).orElseThrow();
        assertThat(delivered.status().name()).isEqualTo("DELIVERED");
    }

    @Test
    @Transactional
    void reportsNoMatchForAnotherTenant() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId recipientId = AggregateId.newId();
        persistUser(recipientId, tenantId, "other-recipient@example.in");

        AggregateId notificationId = AggregateId.newId();
        Notification notification = Notification.queue(
                notificationId, tenantId,
                new NotificationRecipient(recipientId, "other-recipient@example.in"),
                NotificationChannel.SMS, NotificationCategory.SECURITY_ALERT,
                new NotificationContent(null, "A security event occurred."),
                NOW, recipientId, NOW);
        notificationRepository.save(notification);
        flushAndClear();

        assertThat(notificationRepository.findById(AggregateId.newId(), notificationId)).isEmpty();
    }

    @Test
    @Transactional
    void paginatesAndSortsNotificationsAtTheDatabase() {
        AggregateId tenantId = AggregateId.newId();
        AggregateId recipientId = AggregateId.newId();
        persistUser(recipientId, tenantId, "paged-recipient@example.in");

        notificationRepository.save(newNotification(tenantId, recipientId, NOW));
        notificationRepository.save(newNotification(tenantId, recipientId, NOW.plusSeconds(60)));
        notificationRepository.save(newNotification(tenantId, recipientId, NOW.plusSeconds(120)));
        flushAndClear();

        NotificationPage<Notification> firstPage = notificationRepository.findPage(
                tenantId, new NotificationPageRequest(0, 2, NotificationSortField.SCHEDULED_AT, SortDirection.ASC));
        assertThat(firstPage.content()).hasSize(2);
        assertThat(firstPage.totalElements()).isEqualTo(3);
        assertThat(firstPage.hasNext()).isTrue();

        NotificationPage<Notification> descending = notificationRepository.findPage(
                tenantId, new NotificationPageRequest(0, 10, NotificationSortField.SCHEDULED_AT, SortDirection.DESC));
        assertThat(descending.content().getFirst().scheduledAt()).isEqualTo(NOW.plusSeconds(120));
    }

    private Notification newNotification(AggregateId tenantId, AggregateId recipientId, Instant scheduledAt) {
        return Notification.queue(
                AggregateId.newId(), tenantId,
                new NotificationRecipient(recipientId, "paged-recipient@example.in"),
                NotificationChannel.EMAIL, NotificationCategory.GROUP_UPDATE,
                new NotificationContent("Group update", "There is an update for your group."),
                scheduledAt, recipientId, scheduledAt);
    }

    private void persistUser(AggregateId userId, AggregateId tenantId, String email) {
        userRepository.saveAndFlush(new UserJpaEntity(
                userId.value(),
                tenantId.value(),
                "Community",
                "Member",
                email,
                null,
                UserStatus.ACTIVE,
                PreferredLanguage.ENGLISH));
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class NotificationPersistenceTestConfiguration {

        @Bean
        CurrentAuditorProvider currentAuditorProvider() {
            return () -> Optional.of(AUDITOR_ID);
        }
    }
}
