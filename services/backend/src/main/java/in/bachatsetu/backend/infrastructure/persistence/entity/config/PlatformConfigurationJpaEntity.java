package in.bachatsetu.backend.infrastructure.persistence.entity.config;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "platform_configuration", schema = "config")
public class PlatformConfigurationJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private short id;

    @Column(name = "default_language", nullable = false, length = 10)
    private String defaultLanguage;

    @Column(name = "otp_expiry_seconds", nullable = false)
    private int otpExpirySeconds;

    @Column(name = "default_storage_provider", nullable = false, length = 50)
    private String defaultStorageProvider;

    @Column(name = "default_payment_provider", nullable = false, length = 50)
    private String defaultPaymentProvider;

    @Column(name = "notification_retry_count", nullable = false)
    private int notificationRetryCount;

    @Column(name = "maximum_upload_size_bytes", nullable = false)
    private long maximumUploadSizeBytes;

    @Column(name = "maximum_members_per_group", nullable = false)
    private int maximumMembersPerGroup;

    @Column(name = "maximum_groups_per_organizer", nullable = false)
    private int maximumGroupsPerOrganizer;

    @Column(name = "maintenance_enabled", nullable = false)
    private boolean maintenanceEnabled;

    @Column(name = "maintenance_message")
    private String maintenanceMessage;

    @Column(name = "maintenance_start_at")
    private Instant maintenanceStartAt;

    @Column(name = "maintenance_end_at")
    private Instant maintenanceEndAt;

    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;

    public PlatformConfigurationJpaEntity() {
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
            long version,
            Instant updatedAt,
            UUID updatedBy) {
        this.id = 1;
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
        this.version = version;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    public short getId() {
        return id;
    }

    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    public int getOtpExpirySeconds() {
        return otpExpirySeconds;
    }

    public String getDefaultStorageProvider() {
        return defaultStorageProvider;
    }

    public String getDefaultPaymentProvider() {
        return defaultPaymentProvider;
    }

    public int getNotificationRetryCount() {
        return notificationRetryCount;
    }

    public long getMaximumUploadSizeBytes() {
        return maximumUploadSizeBytes;
    }

    public int getMaximumMembersPerGroup() {
        return maximumMembersPerGroup;
    }

    public int getMaximumGroupsPerOrganizer() {
        return maximumGroupsPerOrganizer;
    }

    public boolean isMaintenanceEnabled() {
        return maintenanceEnabled;
    }

    public String getMaintenanceMessage() {
        return maintenanceMessage;
    }

    public Instant getMaintenanceStartAt() {
        return maintenanceStartAt;
    }

    public Instant getMaintenanceEndAt() {
        return maintenanceEndAt;
    }

    public long getVersion() {
        return version;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }
}
