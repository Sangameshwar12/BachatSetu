package in.bachatsetu.backend.payment.application.query;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/** One group member's contribution status for the current cycle. */
public record MemberCollectionResult(
        UUID memberId,
        String memberName,
        String status,
        long expectedAmountPaise,
        long collectedAmountPaise,
        Instant paidAt,
        LocalDate dueDate) {

    public MemberCollectionResult {
        Objects.requireNonNull(memberId, "member id must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(dueDate, "due date must not be null");
    }
}
