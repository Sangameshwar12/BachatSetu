package in.bachatsetu.backend.infrastructure.persistence.integration;

import static in.bachatsetu.backend.group.domain.GroupDomainFixtures.NOW;
import static in.bachatsetu.backend.group.domain.GroupDomainFixtures.newGroup;
import static org.assertj.core.api.Assertions.assertThat;

import in.bachatsetu.backend.BachatSetuBackendApplication;
import in.bachatsetu.backend.group.application.port.SavingsGroupRepository;
import in.bachatsetu.backend.group.domain.model.SavingsGroup;
import in.bachatsetu.backend.infrastructure.persistence.audit.CurrentAuditorProvider;
import in.bachatsetu.backend.infrastructure.persistence.entity.identity.UserJpaEntity;
import in.bachatsetu.backend.infrastructure.persistence.repository.jpa.UserSpringDataRepository;
import in.bachatsetu.backend.payment.domain.model.IdempotencyKey;
import in.bachatsetu.backend.payment.domain.model.Payment;
import in.bachatsetu.backend.payment.domain.model.PaymentMethod;
import in.bachatsetu.backend.payment.domain.model.PaymentReference;
import in.bachatsetu.backend.payment.domain.port.PaymentPage;
import in.bachatsetu.backend.payment.domain.port.PaymentPageRequest;
import in.bachatsetu.backend.payment.domain.port.PaymentRepository;
import in.bachatsetu.backend.payment.domain.port.PaymentSortField;
import in.bachatsetu.backend.payment.domain.port.SortDirection;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Money;
import in.bachatsetu.backend.user.domain.model.PreferredLanguage;
import in.bachatsetu.backend.user.domain.model.UserStatus;
import jakarta.persistence.EntityManager;
import java.util.Locale;
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
@Import(PaymentPersistencePostgreSqlIntegrationTest.PaymentPersistenceTestConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class PaymentPersistencePostgreSqlIntegrationTest extends PostgreSqlIntegrationTest {

    private static final UUID AUDITOR_ID = UUID.fromString("f3000000-0000-0000-0000-000000000001");

    @Autowired
    private SavingsGroupRepository groupRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private UserSpringDataRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @Transactional
    void persistsAndRehydratesAPayment() {
        SavingsGroup group = newGroup(AggregateId.newId(), 5);
        persistUser(group.organizerId(), group.tenantId(), "owner@example.in");
        AggregateId payerId = AggregateId.newId();
        persistUser(payerId, group.tenantId(), "payer@example.in");
        groupRepository.save(group);
        flushAndClear();

        AggregateId paymentId = AggregateId.newId();
        Payment payment = Payment.initiate(
                paymentId, group.tenantId(), group.id(), payerId,
                new PaymentReference("PAY-1A2B3C4D5E6F7A8B"),
                new IdempotencyKey("checkout-attempt-0001"),
                Money.inr(500_000), PaymentMethod.UPI, group.organizerId(), NOW);

        paymentRepository.save(payment);
        flushAndClear();

        Payment restored = paymentRepository.findById(group.tenantId(), paymentId).orElseThrow();
        assertThat(restored.memberId()).isEqualTo(payerId);
        assertThat(restored.groupId()).isEqualTo(group.id());
        assertThat(restored.reference().value()).isEqualTo("PAY-1A2B3C4D5E6F7A8B");
        assertThat(restored.amount()).isEqualTo(Money.inr(500_000));
        assertThat(restored.status().name()).isEqualTo("INITIATED");

        assertThat(paymentRepository.findByReference(group.tenantId(), payment.reference())).isPresent();
        assertThat(paymentRepository.findByIdempotencyKey(group.tenantId(), payment.idempotencyKey())).isPresent();
    }

    @Test
    @Transactional
    void reportsNoMatchForAnotherTenant() {
        SavingsGroup group = newGroup(AggregateId.newId(), 5);
        persistUser(group.organizerId(), group.tenantId(), "owner-two@example.in");
        AggregateId payerId = AggregateId.newId();
        persistUser(payerId, group.tenantId(), "payer-two@example.in");
        groupRepository.save(group);
        flushAndClear();

        AggregateId paymentId = AggregateId.newId();
        Payment payment = Payment.initiate(
                paymentId, group.tenantId(), group.id(), payerId,
                new PaymentReference("PAY-2A2B3C4D5E6F7A8B"),
                new IdempotencyKey("checkout-attempt-0002"),
                Money.inr(500_000), PaymentMethod.UPI, group.organizerId(), NOW);
        paymentRepository.save(payment);
        flushAndClear();

        assertThat(paymentRepository.findById(AggregateId.newId(), paymentId)).isEmpty();
    }

    @Test
    @Transactional
    void paginatesAndSortsPaymentsAtTheDatabase() {
        SavingsGroup group = newGroup(AggregateId.newId(), 5);
        persistUser(group.organizerId(), group.tenantId(), "owner-three@example.in");
        AggregateId tenantId = group.tenantId();
        groupRepository.save(group);
        flushAndClear();

        Payment cheap = newPayment(group, "PAY-ALPHA00000000", 100_000);
        Payment medium = newPayment(group, "PAY-BETA000000000", 300_000);
        Payment expensive = newPayment(group, "PAY-GAMMA00000000", 900_000);
        paymentRepository.save(cheap);
        paymentRepository.save(medium);
        paymentRepository.save(expensive);
        flushAndClear();

        PaymentPage<Payment> byAmountAscending = paymentRepository.findPage(
                tenantId, new PaymentPageRequest(0, 2, PaymentSortField.AMOUNT, SortDirection.ASC));
        assertThat(byAmountAscending.content()).extracting(payment -> payment.reference().value())
                .containsExactly("PAY-ALPHA00000000", "PAY-BETA000000000");
        assertThat(byAmountAscending.totalElements()).isEqualTo(3);
        assertThat(byAmountAscending.hasNext()).isTrue();
        assertThat(byAmountAscending.hasPrevious()).isFalse();

        PaymentPage<Payment> secondPage = paymentRepository.findPage(
                tenantId, new PaymentPageRequest(1, 2, PaymentSortField.AMOUNT, SortDirection.ASC));
        assertThat(secondPage.content()).extracting(payment -> payment.reference().value())
                .containsExactly("PAY-GAMMA00000000");
        assertThat(secondPage.hasNext()).isFalse();
        assertThat(secondPage.hasPrevious()).isTrue();

        PaymentPage<Payment> byCreatedAtDescending = paymentRepository.findPage(
                tenantId, new PaymentPageRequest(0, 10, PaymentSortField.CREATED_AT, SortDirection.DESC));
        assertThat(byCreatedAtDescending.content()).extracting(payment -> payment.reference().value())
                .containsExactly("PAY-GAMMA00000000", "PAY-BETA000000000", "PAY-ALPHA00000000");
    }

    private Payment newPayment(SavingsGroup group, String reference, long amountPaise) {
        AggregateId payerId = AggregateId.newId();
        persistUser(payerId, group.tenantId(), reference.toLowerCase(Locale.ROOT) + "@example.in");
        return Payment.initiate(
                AggregateId.newId(), group.tenantId(), group.id(), payerId,
                new PaymentReference(reference),
                new IdempotencyKey("checkout-" + reference.toLowerCase(Locale.ROOT)),
                Money.inr(amountPaise), PaymentMethod.UPI, group.organizerId(), NOW);
    }

    private void persistUser(AggregateId userId, AggregateId tenantId, String email) {
        userRepository.saveAndFlush(new UserJpaEntity(
                userId.value(),
                tenantId.value(),
                "Community",
                "Payer",
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
    static class PaymentPersistenceTestConfiguration {

        @Bean
        CurrentAuditorProvider currentAuditorProvider() {
            return () -> Optional.of(AUDITOR_ID);
        }
    }
}
