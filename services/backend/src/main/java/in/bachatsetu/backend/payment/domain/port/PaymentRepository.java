package in.bachatsetu.backend.payment.domain.port;

import in.bachatsetu.backend.payment.domain.model.IdempotencyKey;
import in.bachatsetu.backend.payment.domain.model.Payment;
import in.bachatsetu.backend.payment.domain.model.PaymentReference;
import in.bachatsetu.backend.payment.domain.model.ProviderReference;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Optional;

public interface PaymentRepository {

    Optional<Payment> findById(AggregateId paymentId);

    Optional<Payment> findById(AggregateId tenantId, AggregateId paymentId);

    PaymentPage<Payment> findPage(AggregateId tenantId, PaymentPageRequest pageRequest);

    Optional<Payment> findByReference(AggregateId tenantId, PaymentReference reference);

    Optional<Payment> findByIdempotencyKey(AggregateId tenantId, IdempotencyKey idempotencyKey);

    Optional<Payment> findByProviderReference(ProviderReference providerReference);

    /** Most recent payment a given member has made toward a given group, if any. */
    Optional<Payment> findLatestByGroupAndMember(AggregateId tenantId, AggregateId groupId, AggregateId memberId);

    void save(Payment payment);
}
