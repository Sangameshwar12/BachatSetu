package in.bachatsetu.backend.auth.domain.model;

import in.bachatsetu.backend.auth.domain.event.PasswordChanged;
import in.bachatsetu.backend.auth.domain.event.UserRegistered;
import in.bachatsetu.backend.auth.domain.exception.IdentityDomainException;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.AuditInfo;
import in.bachatsetu.backend.shared.domain.BaseAggregateRoot;
import in.bachatsetu.backend.shared.domain.Email;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Authentication user aggregate. Profile and community membership remain in their own bounded contexts.
 */
public final class User extends BaseAggregateRoot {

    private final UserId userId;
    private final Email email;
    private final MobileNumber mobileNumber;
    private PasswordHash passwordHash;
    private UserStatus status;
    private final Set<RoleId> roleIds;

    private User(
            UserId userId,
            Email email,
            MobileNumber mobileNumber,
            PasswordHash passwordHash,
            UserStatus status,
            Set<RoleId> roleIds,
            AuditInfo auditInfo,
            long version) {
        super(userId.toAggregateId(), auditInfo, version);
        this.userId = Objects.requireNonNull(userId, "user id must not be null");
        this.email = Objects.requireNonNull(email, "email must not be null");
        this.mobileNumber = Objects.requireNonNull(mobileNumber, "mobile number must not be null");
        this.passwordHash = Objects.requireNonNull(passwordHash, "password hash must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.roleIds = new LinkedHashSet<>(Objects.requireNonNull(roleIds, "role ids must not be null"));
    }

    /** Registers a pending-verification authentication user. */
    public static User register(
            UserId userId,
            Email email,
            MobileNumber mobileNumber,
            PasswordHash passwordHash,
            AggregateId actorId,
            Instant registeredAt) {
        Objects.requireNonNull(registeredAt, "registeredAt must not be null");
        User user = new User(
                userId,
                email,
                mobileNumber,
                passwordHash,
                UserStatus.PENDING_VERIFICATION,
                Set.of(),
                AuditInfo.createdBy(actorId, registeredAt),
                0);
        user.registerEvent(new UserRegistered(UUID.randomUUID(), userId, email, mobileNumber, registeredAt));
        return user;
    }

    /**
     * Reconstructs persisted state without emitting domain events.
     *
     * @param userId persisted user identifier
     * @param email persisted email address
     * @param mobileNumber persisted mobile number
     * @param passwordHash persisted encoded password hash
     * @param status persisted lifecycle status
     * @param roleIds persisted role associations
     * @param auditInfo persisted audit metadata
     * @param version persisted optimistic-lock version
     * @return reconstructed aggregate
     */
    public static User rehydrate(
            UserId userId,
            Email email,
            MobileNumber mobileNumber,
            PasswordHash passwordHash,
            UserStatus status,
            Set<RoleId> roleIds,
            AuditInfo auditInfo,
            long version) {
        return new User(userId, email, mobileNumber, passwordHash, status, roleIds, auditInfo, version);
    }

    /** Assigns a role once. */
    public void assignRole(RoleId roleId, AggregateId actorId, Instant assignedAt) {
        Objects.requireNonNull(roleId, "role id must not be null");
        if (!roleIds.add(roleId)) {
            throw new IdentityDomainException("user already has the role");
        }
        markChanged(actorId, assignedAt);
    }

    /** Changes the encoded password hash and emits no secret material. */
    public void changePassword(PasswordHash newPasswordHash, AggregateId actorId, Instant changedAt) {
        Objects.requireNonNull(newPasswordHash, "new password hash must not be null");
        if (passwordHash.equals(newPasswordHash)) {
            throw new IdentityDomainException("new password hash must differ from the current hash");
        }
        passwordHash = newPasswordHash;
        markChanged(actorId, changedAt);
        registerEvent(new PasswordChanged(UUID.randomUUID(), userId, changedAt));
    }

    public UserId userId() {
        return userId;
    }

    public Email email() {
        return email;
    }

    public MobileNumber mobileNumber() {
        return mobileNumber;
    }

    public PasswordHash passwordHash() {
        return passwordHash;
    }

    public UserStatus status() {
        return status;
    }

    public Set<RoleId> roleIds() {
        return Set.copyOf(roleIds);
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof User that && userId.equals(that.userId);
    }

    @Override
    public int hashCode() {
        return userId.hashCode();
    }
}
