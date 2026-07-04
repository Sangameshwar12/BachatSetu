package in.bachatsetu.backend.receipt.domain.port;

import in.bachatsetu.backend.receipt.domain.model.Receipt;
import in.bachatsetu.backend.receipt.domain.model.ReceiptNumber;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.Optional;

public interface ReceiptRepository {

    Optional<Receipt> findById(AggregateId receiptId);

    Optional<Receipt> findByNumber(AggregateId tenantId, ReceiptNumber number);

    Optional<Receipt> findByPaymentId(AggregateId paymentId);

    void save(Receipt receipt);
}
