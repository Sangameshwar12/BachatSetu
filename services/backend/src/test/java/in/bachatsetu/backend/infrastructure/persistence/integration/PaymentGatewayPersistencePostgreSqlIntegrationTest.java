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
import in.bachatsetu.backend.payment.domain.port.PaymentRepository;
import in.bachatsetu.backend.paymentgateway.domain.model.GatewayOrder;
import in.bachatsetu.backend.paymentgateway.domain.model.GatewayType;
import in.bachatsetu.backend.paymentgateway.domain.port.GatewayOrderRepository;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Money;
import in.bachatsetu.backend.user.domain.model.PreferredLanguage;
import in.bachatsetu.backend.user.domain.model.UserStatus;
import jakarta.persistence.EntityManager;
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
@Import(PaymentGatewayPersistencePostgreSqlIntegrationTest.PaymentGatewayPersistenceTestConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class PaymentGatewayPersistencePostgreSqlIntegrationTest extends PostgreSqlIntegrationTest {

    private static final UUID AUDITOR_ID = UUID.fromString("f6000000-0000-0000-0000-000000000001");

    @Autowired
    private SavingsGroupRepository groupRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private GatewayOrderRepository gatewayOrderRepository;

    @Autowired
    private UserSpringDataRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @Transactional
    void persistsAndRehydratesAGatewayOrder() {
        SavingsGroup group = newGroup(AggregateId.newId(), 5);
        persistUser(group.organizerId(), group.tenantId(), "owner@example.in");
        AggregateId payerId = AggregateId.newId();
        persistUser(payerId, group.tenantId(), "payer@example.in");
        groupRepository.save(group);
        flushAndClear();

        Payment payment = Payment.initiate(
                AggregateId.newId(), group.tenantId(), group.id(), payerId,
                new PaymentReference("PAY-1A2B3C4D5E6F7A8B"), new IdempotencyKey("checkout-attempt-0001"),
                Money.inr(500_000), PaymentMethod.UPI, group.organizerId(), NOW);
        paymentRepository.save(payment);
        flushAndClear();

        AggregateId orderId = AggregateId.newId();
        GatewayOrder order = GatewayOrder.create(
                orderId, group.tenantId(), payment.id(), GatewayType.RAZORPAY, "order_rzp_1",
                "https://razorpay.example/pay/order_rzp_1", group.organizerId(), NOW);
        gatewayOrderRepository.save(order);
        flushAndClear();

        GatewayOrder restored = gatewayOrderRepository.findByPaymentId(group.tenantId(), payment.id()).orElseThrow();
        assertThat(restored.gatewayType()).isEqualTo(GatewayType.RAZORPAY);
        assertThat(restored.providerOrderId()).isEqualTo("order_rzp_1");
        assertThat(restored.providerStatus()).isNull();
        assertThat(restored.providerRefundId()).isNull();

        assertThat(gatewayOrderRepository.findByProviderOrderId(GatewayType.RAZORPAY, "order_rzp_1"))
                .isPresent()
                .get()
                .extracting(GatewayOrder::paymentId)
                .isEqualTo(payment.id());
    }

    @Test
    @Transactional
    void updatesTheProviderStatusAndRecordsARefundAcrossAReload() {
        SavingsGroup group = newGroup(AggregateId.newId(), 5);
        persistUser(group.organizerId(), group.tenantId(), "owner-two@example.in");
        AggregateId payerId = AggregateId.newId();
        persistUser(payerId, group.tenantId(), "payer-two@example.in");
        groupRepository.save(group);
        flushAndClear();

        Payment payment = Payment.initiate(
                AggregateId.newId(), group.tenantId(), group.id(), payerId,
                new PaymentReference("PAY-2A2B3C4D5E6F7A8B"), new IdempotencyKey("checkout-attempt-0002"),
                Money.inr(500_000), PaymentMethod.UPI, group.organizerId(), NOW);
        paymentRepository.save(payment);
        flushAndClear();

        GatewayOrder order = GatewayOrder.create(
                AggregateId.newId(), group.tenantId(), payment.id(), GatewayType.STRIPE, "pi_1",
                "https://checkout.stripe.example/pay/pi_1", group.organizerId(), NOW);
        gatewayOrderRepository.save(order);
        flushAndClear();

        GatewayOrder loaded = gatewayOrderRepository.findByPaymentId(group.tenantId(), payment.id()).orElseThrow();
        loaded.updateProviderStatus("succeeded", group.organizerId(), NOW.plusSeconds(60));
        loaded.recordRefund("re_1", group.organizerId(), NOW.plusSeconds(120));
        gatewayOrderRepository.save(loaded);
        flushAndClear();

        GatewayOrder restored = gatewayOrderRepository.findByPaymentId(group.tenantId(), payment.id()).orElseThrow();
        assertThat(restored.providerStatus()).isEqualTo("succeeded");
        assertThat(restored.providerRefundId()).isEqualTo("re_1");
    }

    @Test
    @Transactional
    void reportsNoMatchForAnotherTenant() {
        SavingsGroup group = newGroup(AggregateId.newId(), 5);
        persistUser(group.organizerId(), group.tenantId(), "owner-three@example.in");
        AggregateId payerId = AggregateId.newId();
        persistUser(payerId, group.tenantId(), "payer-three@example.in");
        groupRepository.save(group);
        flushAndClear();

        Payment payment = Payment.initiate(
                AggregateId.newId(), group.tenantId(), group.id(), payerId,
                new PaymentReference("PAY-3A2B3C4D5E6F7A8B"), new IdempotencyKey("checkout-attempt-0003"),
                Money.inr(500_000), PaymentMethod.UPI, group.organizerId(), NOW);
        paymentRepository.save(payment);
        flushAndClear();

        GatewayOrder order = GatewayOrder.create(
                AggregateId.newId(), group.tenantId(), payment.id(), GatewayType.CASHFREE, "cf_order_1", null,
                group.organizerId(), NOW);
        gatewayOrderRepository.save(order);
        flushAndClear();

        assertThat(gatewayOrderRepository.findByPaymentId(AggregateId.newId(), payment.id())).isEmpty();
    }

    private void persistUser(AggregateId userId, AggregateId tenantId, String email) {
        userRepository.saveAndFlush(new UserJpaEntity(
                userId.value(), tenantId.value(), "Community", "Payer", email, null,
                UserStatus.ACTIVE, PreferredLanguage.ENGLISH));
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class PaymentGatewayPersistenceTestConfiguration {

        @Bean
        CurrentAuditorProvider currentAuditorProvider() {
            return () -> Optional.of(AUDITOR_ID);
        }
    }
}
