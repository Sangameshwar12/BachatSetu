package in.bachatsetu.backend.user.domain.model;

import in.bachatsetu.backend.shared.domain.Address;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.AuditInfo;
import in.bachatsetu.backend.shared.domain.BaseAggregateRoot;
import in.bachatsetu.backend.user.domain.event.ProfileCompleted;
import in.bachatsetu.backend.user.domain.event.UserContactChanged;
import in.bachatsetu.backend.user.domain.event.UserRegistered;
import in.bachatsetu.backend.user.domain.exception.InvalidUserStateException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class UserProfile extends BaseAggregateRoot {

    private PersonName name;
    private UserContact contact;
    private Address address;
    private PreferredLanguage preferredLanguage;
    private UserStatus status;
    private final List<ContactPreference> contactPreferences;
    private String city;
    private String state;
    private AggregateId photoFileId;
    private boolean notificationsEnabled = true;
    private boolean onboarded;

    public UserProfile(
            AggregateId id,
            PersonName name,
            UserContact contact,
            Address address,
            PreferredLanguage preferredLanguage,
            UserStatus status,
            List<ContactPreference> contactPreferences,
            AuditInfo auditInfo,
            long version) {
        this(
                id, name, contact, address, preferredLanguage, status, contactPreferences, null, null, null, true,
                false, auditInfo, version);
    }

    public UserProfile(
            AggregateId id,
            PersonName name,
            UserContact contact,
            Address address,
            PreferredLanguage preferredLanguage,
            UserStatus status,
            List<ContactPreference> contactPreferences,
            String city,
            String state,
            AggregateId photoFileId,
            boolean notificationsEnabled,
            boolean onboarded,
            AuditInfo auditInfo,
            long version) {
        super(id, auditInfo, version);
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.contact = Objects.requireNonNull(contact, "contact must not be null");
        this.address = address;
        this.preferredLanguage = Objects.requireNonNull(preferredLanguage, "preferredLanguage must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.contactPreferences = new ArrayList<>(
                Objects.requireNonNull(contactPreferences, "contactPreferences must not be null"));
        this.city = city;
        this.state = state;
        this.photoFileId = photoFileId;
        this.notificationsEnabled = notificationsEnabled;
        this.onboarded = onboarded;
    }

    public static UserProfile register(
            AggregateId id,
            PersonName name,
            UserContact contact,
            PreferredLanguage preferredLanguage,
            AggregateId actorId,
            Instant registeredAt) {
        UserProfile user = new UserProfile(
                id,
                name,
                contact,
                null,
                preferredLanguage,
                UserStatus.INVITED,
                List.of(),
                AuditInfo.createdBy(actorId, registeredAt),
                0);
        user.registerEvent(new UserRegistered(UUID.randomUUID(), id, registeredAt));
        return user;
    }

    public void changeContact(UserContact newContact, AggregateId actorId, Instant changedAt) {
        ensureMutable();
        UserContact previousContact = contact;
        contact = Objects.requireNonNull(newContact, "newContact must not be null");
        markChanged(actorId, changedAt);
        registerEvent(new UserContactChanged(
                UUID.randomUUID(), id(), previousContact, newContact, changedAt));
    }

    public void activate(AggregateId actorId, Instant changedAt) {
        if (status != UserStatus.INVITED && status != UserStatus.LOCKED) {
            throw new InvalidUserStateException("only invited or locked users can be activated");
        }
        status = UserStatus.ACTIVE;
        markChanged(actorId, changedAt);
    }

    /** Completes the post-signup onboarding step exactly once. */
    public void completeOnboarding(
            String city,
            String state,
            AggregateId photoFileId,
            boolean notificationsEnabled,
            AggregateId actorId,
            Instant completedAt) {
        ensureMutable();
        if (onboarded) {
            throw new InvalidUserStateException("profile onboarding has already been completed");
        }
        this.city = city;
        this.state = state;
        this.photoFileId = photoFileId;
        this.notificationsEnabled = notificationsEnabled;
        this.onboarded = true;
        markChanged(actorId, completedAt);
        registerEvent(new ProfileCompleted(UUID.randomUUID(), id(), completedAt));
    }

    private void ensureMutable() {
        if (status == UserStatus.DELETED) {
            throw new InvalidUserStateException("deleted user profile is immutable");
        }
    }

    public PersonName name() {
        return name;
    }

    public UserContact contact() {
        return contact;
    }

    public Address address() {
        return address;
    }

    public PreferredLanguage preferredLanguage() {
        return preferredLanguage;
    }

    public UserStatus status() {
        return status;
    }

    public List<ContactPreference> contactPreferences() {
        return List.copyOf(contactPreferences);
    }

    public String city() {
        return city;
    }

    public String state() {
        return state;
    }

    public AggregateId photoFileId() {
        return photoFileId;
    }

    public boolean notificationsEnabled() {
        return notificationsEnabled;
    }

    public boolean onboarded() {
        return onboarded;
    }
}
