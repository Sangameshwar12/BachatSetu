package in.bachatsetu.backend.invitation.application.query;

import java.util.Objects;

/** Safe, pre-join preview shown before a caller commits to joining a group. */
public record InvitationPreviewResult(
        String groupName,
        String organizerName,
        long contributionAmountPaise,
        String currencyCode,
        String frequency,
        int memberCount,
        int maximumMembers) {

    public InvitationPreviewResult {
        Objects.requireNonNull(groupName, "groupName must not be null");
        Objects.requireNonNull(organizerName, "organizerName must not be null");
        Objects.requireNonNull(currencyCode, "currencyCode must not be null");
        Objects.requireNonNull(frequency, "frequency must not be null");
    }
}
