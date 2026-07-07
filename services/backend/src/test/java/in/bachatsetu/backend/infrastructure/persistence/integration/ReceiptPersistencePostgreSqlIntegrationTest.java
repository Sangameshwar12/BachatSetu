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
import in.bachatsetu.backend.receipt.domain.model.Receipt;
import in.bachatsetu.backend.receipt.domain.model.ReceiptDescription;
import in.bachatsetu.backend.receipt.domain.model.ReceiptLine;
import in.bachatsetu.backend.receipt.domain.model.ReceiptNumber;
import in.bachatsetu.backend.receipt.domain.model.ReceiptType;
import in.bachatsetu.backend.receipt.domain.port.ReceiptPage;
import in.bachatsetu.backend.receipt.domain.port.ReceiptPageRequest;
import in.bachatsetu.backend.receipt.domain.port.ReceiptRepository;
import in.bachatsetu.backend.receipt.domain.port.ReceiptSortField;
import in.bachatsetu.backend.receipt.domain.port.SortDirection;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Money;
import in.bachatsetu.backend.user.domain.model.PreferredLanguage;
import in.bachatsetu.backend.user.domain.model.UserStatus;
import jakarta.persistence.EntityManager;
import java.util.List;
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
@Import(ReceiptPersistencePostgreSqlIntegrationTest.ReceiptPersistenceTestConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class ReceiptPersistencePostgreSqlIntegrationTest extends PostgreSqlIntegrationTest {

    private static final UUID AUDITOR_ID = UUID.fromString("f5000000-0000-0000-0000-000000000001");

    @Autowired
    private SavingsGroupRepository groupRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ReceiptRepository receiptRepository;

    @Autowired
    private UserSpringDataRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @Transactional
    void persistsAndRehydratesAReceipt() {
        SavingsGroup group = newGroup(AggregateId.newId(), 5);
        persistUser(group.organizerId(), group.tenantId(), "owner@example.in");
        AggregateId payerId = AggregateId.newId();
        persistUser(payerId, group.tenantId(), "payer@example.in");
        groupRepository.save(group);
        flushAndClear();

        Payment payment = newPayment(group, payerId, "PAY-1A2B3C4D5E6F7A8B", 500_000);
        paymentRepository.save(payment);
        flushAndClear();

        AggregateId receiptId = AggregateId.newId();
        List<ReceiptLine> lines = List.of(
                new ReceiptLine(
                        AggregateId.newId(), ReceiptType.CONTRIBUTION,
                        new ReceiptDescription("Monthly contribution"), Money.inr(400_000)),
                new ReceiptLine(
                        AggregateId.newId(), ReceiptType.PENALTY,
                        new ReceiptDescription("Late payment penalty"), Money.inr(100_000)));
        Receipt receipt = Receipt.generate(
                receiptId, group.tenantId(), payment.id(), payerId,
                new ReceiptNumber("RCT/20260807/1A2B3C4D"), lines, group.organizerId(), NOW);

        receiptRepository.save(receipt);
        flushAndClear();

        Receipt restored = receiptRepository.findById(group.tenantId(), receiptId).orElseThrow();
        assertThat(restored.paymentId()).isEqualTo(payment.id());
        assertThat(restored.memberId()).isEqualTo(payerId);
        assertThat(restored.number().value()).isEqualTo("RCT/20260807/1A2B3C4D");
        assertThat(restored.status().name()).isEqualTo("GENERATED");
        assertThat(restored.total()).isEqualTo(Money.inr(500_000));

        assertThat(receiptRepository.findByPaymentId(payment.id())).isPresent();
        assertThat(receiptRepository.findByNumber(group.tenantId(), receipt.number())).isPresent();
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

        Payment payment = newPayment(group, payerId, "PAY-2A2B3C4D5E6F7A8B", 500_000);
        paymentRepository.save(payment);
        flushAndClear();

        AggregateId receiptId = AggregateId.newId();
        Receipt receipt = Receipt.generate(
                receiptId, group.tenantId(), payment.id(), payerId,
                new ReceiptNumber("RCT/20260807/2A2B3C4D"), singleLine(500_000), group.organizerId(), NOW);
        receiptRepository.save(receipt);
        flushAndClear();

        assertThat(receiptRepository.findById(AggregateId.newId(), receiptId)).isEmpty();
    }

    @Test
    @Transactional
    void paginatesAndSortsReceiptsAtTheDatabase() {
        SavingsGroup group = newGroup(AggregateId.newId(), 5);
        persistUser(group.organizerId(), group.tenantId(), "owner-three@example.in");
        AggregateId tenantId = group.tenantId();
        groupRepository.save(group);
        flushAndClear();

        Receipt cheap = newReceipt(group, "PAY-ALPHA00000000", "RCT/20260807/ALPHA001", 100_000);
        Receipt medium = newReceipt(group, "PAY-BETA000000000", "RCT/20260807/BETA0001", 300_000);
        Receipt expensive = newReceipt(group, "PAY-GAMMA00000000", "RCT/20260807/GAMMA001", 900_000);
        receiptRepository.save(cheap);
        receiptRepository.save(medium);
        receiptRepository.save(expensive);
        flushAndClear();

        ReceiptPage<Receipt> byAmountAscending = receiptRepository.findPage(
                tenantId, new ReceiptPageRequest(0, 2, ReceiptSortField.AMOUNT, SortDirection.ASC));
        assertThat(byAmountAscending.content()).extracting(receipt -> receipt.number().value())
                .containsExactly("RCT/20260807/ALPHA001", "RCT/20260807/BETA0001");
        assertThat(byAmountAscending.totalElements()).isEqualTo(3);
        assertThat(byAmountAscending.hasNext()).isTrue();
        assertThat(byAmountAscending.hasPrevious()).isFalse();

        ReceiptPage<Receipt> secondPage = receiptRepository.findPage(
                tenantId, new ReceiptPageRequest(1, 2, ReceiptSortField.AMOUNT, SortDirection.ASC));
        assertThat(secondPage.content()).extracting(receipt -> receipt.number().value())
                .containsExactly("RCT/20260807/GAMMA001");
        assertThat(secondPage.hasNext()).isFalse();
        assertThat(secondPage.hasPrevious()).isTrue();

        ReceiptPage<Receipt> byCreatedAtDescending = receiptRepository.findPage(
                tenantId, new ReceiptPageRequest(0, 10, ReceiptSortField.CREATED_AT, SortDirection.DESC));
        assertThat(byCreatedAtDescending.content()).extracting(receipt -> receipt.number().value())
                .containsExactly("RCT/20260807/GAMMA001", "RCT/20260807/BETA0001", "RCT/20260807/ALPHA001");
    }

    private Receipt newReceipt(SavingsGroup group, String paymentReference, String receiptNumber, long amountPaise) {
        AggregateId payerId = AggregateId.newId();
        persistUser(payerId, group.tenantId(), receiptNumber.toLowerCase(Locale.ROOT).replace("/", "-") + "@example.in");
        Payment payment = newPayment(group, payerId, paymentReference, amountPaise);
        paymentRepository.save(payment);
        entityManager.flush();
        return Receipt.generate(
                AggregateId.newId(), group.tenantId(), payment.id(), payerId,
                new ReceiptNumber(receiptNumber), singleLine(amountPaise), group.organizerId(), NOW);
    }

    private Payment newPayment(SavingsGroup group, AggregateId payerId, String reference, long amountPaise) {
        return Payment.initiate(
                AggregateId.newId(), group.tenantId(), group.id(), payerId,
                new PaymentReference(reference),
                new IdempotencyKey("checkout-" + reference.toLowerCase(Locale.ROOT)),
                Money.inr(amountPaise), PaymentMethod.UPI, group.organizerId(), NOW);
    }

    private List<ReceiptLine> singleLine(long amountPaise) {
        return List.of(new ReceiptLine(
                AggregateId.newId(), ReceiptType.CONTRIBUTION,
                new ReceiptDescription("Monthly contribution"), Money.inr(amountPaise)));
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
    static class ReceiptPersistenceTestConfiguration {

        @Bean
        CurrentAuditorProvider currentAuditorProvider() {
            return () -> Optional.of(AUDITOR_ID);
        }
    }
}
