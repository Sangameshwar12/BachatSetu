package in.bachatsetu.backend.automation.domain.model;

import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.LocalDate;
import java.util.Objects;

/**
 * A read-only projection of one member's outstanding contribution for a monthly cycle, used only to drive
 * reminder notifications. Not an aggregate: it has no identity-based lifecycle or behavior of its own, and
 * is never persisted independently — it is assembled entirely from the pre-existing {@code installments}
 * table.
 */
public record DueInstallment(
        AggregateId tenantId,
        AggregateId installmentId,
        AggregateId recipientUserId,
        String groupName,
        long outstandingAmountPaise,
        String currencyCode,
        LocalDate dueDate) {

    public DueInstallment {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(installmentId, "installmentId must not be null");
        Objects.requireNonNull(recipientUserId, "recipientUserId must not be null");
        Objects.requireNonNull(groupName, "groupName must not be null");
        Objects.requireNonNull(currencyCode, "currencyCode must not be null");
        Objects.requireNonNull(dueDate, "dueDate must not be null");
    }
}
