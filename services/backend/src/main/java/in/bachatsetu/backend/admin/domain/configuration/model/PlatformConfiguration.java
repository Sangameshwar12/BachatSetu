package in.bachatsetu.backend.admin.domain.configuration.model;

import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import java.util.Objects;

/**
 * The single, platform-wide configuration singleton: general settings plus maintenance-mode state.
 * There is exactly one instance of this aggregate for the whole platform (no tenant scoping).
 */
public final class PlatformConfiguration {

    private String defaultLanguage;
    private int otpExpirySeconds;
    private String defaultStorageProvider;
    private String defaultPaymentProvider;
    private int notificationRetryCount;
    private long maximumUploadSizeBytes;
    private int maximumMembersPerGroup;
    private int maximumGroupsPerOrganizer;
    private boolean maintenanceEnabled;
    private String maintenanceMessage;
    private Instant maintenanceStartAt;
    private Instant maintenanceEndAt;
    private long version;
    private Instant updatedAt;
    private AggregateId updatedBy;

    private PlatformConfiguration(
            String defaultLanguage,
            int otpExpirySeconds,
            String defaultStorageProvider,
            String defaultPaymentProvider,
            int notificationRetryCount,
            long maximumUploadSizeBytes,
            int maximumMembersPerGroup,
            int maximumGroupsPerOrganizer,
            boolean maintenanceEnabled,
            String maintenanceMessage,
            Instant maintenanceStartAt,
            Instant maintenanceEndAt,
            long version,
            Instant updatedAt,
            AggregateId updatedBy) {
        assign(
                defaultLanguage, otpExpirySeconds, defaultStorageProvider, defaultPaymentProvider,
                notificationRetryCount, maximumUploadSizeBytes, maximumMembersPerGroup, maximumGroupsPerOrganizer,
                maintenanceEnabled, maintenanceMessage, maintenanceStartAt, maintenanceEndAt);
        this.version = version;
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        this.updatedBy = updatedBy;
    }

    /** Reconstructs an existing configuration row from persistence. */
    public static PlatformConfiguration of(
            String defaultLanguage,
            int otpExpirySeconds,
            String defaultStorageProvider,
            String defaultPaymentProvider,
            int notificationRetryCount,
            long maximumUploadSizeBytes,
            int maximumMembersPerGroup,
            int maximumGroupsPerOrganizer,
            boolean maintenanceEnabled,
            String maintenanceMessage,
            Instant maintenanceStartAt,
            Instant maintenanceEndAt,
            long version,
            Instant updatedAt,
            AggregateId updatedBy) {
        return new PlatformConfiguration(
                defaultLanguage, otpExpirySeconds, defaultStorageProvider, defaultPaymentProvider,
                notificationRetryCount, maximumUploadSizeBytes, maximumMembersPerGroup, maximumGroupsPerOrganizer,
                maintenanceEnabled, maintenanceMessage, maintenanceStartAt, maintenanceEndAt, version, updatedAt,
                updatedBy);
    }

    public void update(
            String defaultLanguage,
            int otpExpirySeconds,
            String defaultStorageProvider,
            String defaultPaymentProvider,
            int notificationRetryCount,
            long maximumUploadSizeBytes,
            int maximumMembersPerGroup,
            int maximumGroupsPerOrganizer,
            boolean maintenanceEnabled,
            String maintenanceMessage,
            Instant maintenanceStartAt,
            Instant maintenanceEndAt,
            AggregateId actorId,
            Instant at) {
        Objects.requireNonNull(actorId, "actorId must not be null");
        Objects.requireNonNull(at, "at must not be null");
        assign(
                defaultLanguage, otpExpirySeconds, defaultStorageProvider, defaultPaymentProvider,
                notificationRetryCount, maximumUploadSizeBytes, maximumMembersPerGroup, maximumGroupsPerOrganizer,
                maintenanceEnabled, maintenanceMessage, maintenanceStartAt, maintenanceEndAt);
        this.version = this.version + 1;
        this.updatedAt = at;
        this.updatedBy = actorId;
    }

    /** Whether maintenance mode is in effect at the given instant, honoring an optional start/end window. */
    public boolean isMaintenanceActiveAt(Instant at) {
        Objects.requireNonNull(at, "at must not be null");
        if (!maintenanceEnabled) {
            return false;
        }
        if (maintenanceStartAt != null && at.isBefore(maintenanceStartAt)) {
            return false;
        }
        return maintenanceEndAt == null || !at.isAfter(maintenanceEndAt);
    }

    private void assign(
            String defaultLanguage,
            int otpExpirySeconds,
            String defaultStorageProvider,
            String defaultPaymentProvider,
            int notificationRetryCount,
            long maximumUploadSizeBytes,
            int maximumMembersPerGroup,
            int maximumGroupsPerOrganizer,
            boolean maintenanceEnabled,
            String maintenanceMessage,
            Instant maintenanceStartAt,
            Instant maintenanceEndAt) {
        if (defaultLanguage == null || defaultLanguage.isBlank()) {
            throw new IllegalArgumentException("defaultLanguage must not be blank");
        }
        if (otpExpirySeconds <= 0) {
            throw new IllegalArgumentException("otpExpirySeconds must be positive");
        }
        if (defaultStorageProvider == null || defaultStorageProvider.isBlank()) {
            throw new IllegalArgumentException("defaultStorageProvider must not be blank");
        }
        if (defaultPaymentProvider == null || defaultPaymentProvider.isBlank()) {
            throw new IllegalArgumentException("defaultPaymentProvider must not be blank");
        }
        if (notificationRetryCount < 0) {
            throw new IllegalArgumentException("notificationRetryCount must not be negative");
        }
        if (maximumUploadSizeBytes <= 0) {
            throw new IllegalArgumentException("maximumUploadSizeBytes must be positive");
        }
        if (maximumMembersPerGroup <= 0) {
            throw new IllegalArgumentException("maximumMembersPerGroup must be positive");
        }
        if (maximumGroupsPerOrganizer <= 0) {
            throw new IllegalArgumentException("maximumGroupsPerOrganizer must be positive");
        }
        if (maintenanceStartAt != null && maintenanceEndAt != null && maintenanceEndAt.isBefore(maintenanceStartAt)) {
            throw new IllegalArgumentException("maintenanceEndAt must not be before maintenanceStartAt");
        }
        this.defaultLanguage = defaultLanguage;
        this.otpExpirySeconds = otpExpirySeconds;
        this.defaultStorageProvider = defaultStorageProvider;
        this.defaultPaymentProvider = defaultPaymentProvider;
        this.notificationRetryCount = notificationRetryCount;
        this.maximumUploadSizeBytes = maximumUploadSizeBytes;
        this.maximumMembersPerGroup = maximumMembersPerGroup;
        this.maximumGroupsPerOrganizer = maximumGroupsPerOrganizer;
        this.maintenanceEnabled = maintenanceEnabled;
        this.maintenanceMessage = maintenanceMessage;
        this.maintenanceStartAt = maintenanceStartAt;
        this.maintenanceEndAt = maintenanceEndAt;
    }

    public String defaultLanguage() {
        return defaultLanguage;
    }

    public int otpExpirySeconds() {
        return otpExpirySeconds;
    }

    public String defaultStorageProvider() {
        return defaultStorageProvider;
    }

    public String defaultPaymentProvider() {
        return defaultPaymentProvider;
    }

    public int notificationRetryCount() {
        return notificationRetryCount;
    }

    public long maximumUploadSizeBytes() {
        return maximumUploadSizeBytes;
    }

    public int maximumMembersPerGroup() {
        return maximumMembersPerGroup;
    }

    public int maximumGroupsPerOrganizer() {
        return maximumGroupsPerOrganizer;
    }

    public boolean maintenanceEnabled() {
        return maintenanceEnabled;
    }

    public String maintenanceMessage() {
        return maintenanceMessage;
    }

    public Instant maintenanceStartAt() {
        return maintenanceStartAt;
    }

    public Instant maintenanceEndAt() {
        return maintenanceEndAt;
    }

    public long version() {
        return version;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public AggregateId updatedBy() {
        return updatedBy;
    }
}
