package in.bachatsetu.backend.auth.domain.model;

import in.bachatsetu.backend.auth.domain.event.AuthAccountCreated;
import in.bachatsetu.backend.auth.domain.event.AuthAccountStatusChanged;
import in.bachatsetu.backend.auth.domain.exception.InvalidAuthAccountStateException;
import in.bachatsetu.backend.auth.domain.exception.InvalidVerificationChallengeException;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.AuditInfo;
import in.bachatsetu.backend.shared.domain.BaseAggregateRoot;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class AuthAccount extends BaseAggregateRoot {

    private final AggregateId userId;
    private final LoginIdentifier loginIdentifier;
    private AuthAccountStatus status;
    private final List<VerificationChallenge> challenges;

    public AuthAccount(
            AggregateId id,
            AggregateId userId,
            LoginIdentifier loginIdentifier,
            AuthAccountStatus status,
            List<VerificationChallenge> challenges,
            AuditInfo auditInfo,
            long version) {
        super(id, auditInfo, version);
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.loginIdentifier = Objects.requireNonNull(loginIdentifier, "loginIdentifier must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.challenges = new ArrayList<>(Objects.requireNonNull(challenges, "challenges must not be null"));
    }

    public static AuthAccount create(
            AggregateId id,
            AggregateId userId,
            LoginIdentifier loginIdentifier,
            AggregateId actorId,
            Instant createdAt) {
        AuthAccount account = new AuthAccount(
                id,
                userId,
                loginIdentifier,
                AuthAccountStatus.PENDING_VERIFICATION,
                List.of(),
                AuditInfo.createdBy(actorId, createdAt),
                0);
        account.registerEvent(new AuthAccountCreated(UUID.randomUUID(), id, userId, createdAt));
        return account;
    }

    public VerificationChallenge requestVerification(
            VerificationPurpose purpose,
            Instant expiresAt,
            AggregateId actorId,
            Instant requestedAt) {
        if (!expiresAt.isAfter(requestedAt)) {
            throw new InvalidVerificationChallengeException("challenge expiry must be in the future");
        }
        boolean pending = challenges.stream().anyMatch(challenge ->
                challenge.purpose() == purpose && challenge.status() == VerificationStatus.PENDING);
        if (pending) {
            throw new InvalidVerificationChallengeException("a pending challenge already exists");
        }
        VerificationChallenge challenge = new VerificationChallenge(
                AggregateId.newId(), purpose, expiresAt, VerificationStatus.PENDING);
        challenges.add(challenge);
        markChanged(actorId, requestedAt);
        return challenge;
    }

    public void activate(AggregateId actorId, Instant changedAt) {
        requireStatus(AuthAccountStatus.PENDING_VERIFICATION, AuthAccountStatus.LOCKED, AuthAccountStatus.SUSPENDED);
        changeStatus(AuthAccountStatus.ACTIVE, actorId, changedAt);
    }

    public void lock(AggregateId actorId, Instant changedAt) {
        requireStatus(AuthAccountStatus.ACTIVE);
        changeStatus(AuthAccountStatus.LOCKED, actorId, changedAt);
    }

    public void suspend(AggregateId actorId, Instant changedAt) {
        requireStatus(AuthAccountStatus.ACTIVE, AuthAccountStatus.LOCKED);
        changeStatus(AuthAccountStatus.SUSPENDED, actorId, changedAt);
    }

    private void requireStatus(AuthAccountStatus... allowedStatuses) {
        for (AuthAccountStatus allowedStatus : allowedStatuses) {
            if (status == allowedStatus) {
                return;
            }
        }
        throw new InvalidAuthAccountStateException("auth account state does not permit this operation");
    }

    private void changeStatus(AuthAccountStatus nextStatus, AggregateId actorId, Instant changedAt) {
        if (status == nextStatus) {
            throw new InvalidAuthAccountStateException("invalid auth account status transition");
        }
        AuthAccountStatus previousStatus = status;
        status = nextStatus;
        markChanged(actorId, changedAt);
        registerEvent(new AuthAccountStatusChanged(
                UUID.randomUUID(), id(), previousStatus, nextStatus, changedAt));
    }

    public AggregateId userId() {
        return userId;
    }

    public LoginIdentifier loginIdentifier() {
        return loginIdentifier;
    }

    public AuthAccountStatus status() {
        return status;
    }

    public List<VerificationChallenge> challenges() {
        return List.copyOf(challenges);
    }
}
