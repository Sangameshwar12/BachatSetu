package in.bachatsetu.backend.storage.interfaces.rest.config;

import in.bachatsetu.backend.storage.domain.model.StorageProvider;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Strongly typed Storage configuration. Every cloud credential defaults to an empty string via
 * {@code ${ENV_VAR:}} placeholders in {@code application.yml} — never a hardcoded value. AWS/Azure/GCP
 * settings exist ahead of real SDK integration (see {@code docs/application/storage.md}); today's simulated
 * adapters do not read them.
 */
@ConfigurationProperties(prefix = "bachatsetu.storage")
public record StorageProperties(
        boolean enabled, StorageProvider defaultProvider, Local local, Aws aws, Azure azure, Gcp gcp) {

    public StorageProperties {
        Objects.requireNonNull(defaultProvider, "default provider must not be null");
        Objects.requireNonNull(local, "local configuration must not be null");
        Objects.requireNonNull(aws, "aws configuration must not be null");
        Objects.requireNonNull(azure, "azure configuration must not be null");
        Objects.requireNonNull(gcp, "gcp configuration must not be null");
    }

    public record Local(String path) {
        public Local {
            path = Objects.requireNonNullElse(path, "./data/storage");
        }
    }

    public record Aws(String bucket, String region, String accessKeyId, String secretAccessKey) {
        public Aws {
            bucket = Objects.requireNonNullElse(bucket, "");
            region = Objects.requireNonNullElse(region, "");
            accessKeyId = Objects.requireNonNullElse(accessKeyId, "");
            secretAccessKey = Objects.requireNonNullElse(secretAccessKey, "");
        }
    }

    public record Azure(String accountName, String accountKey, String containerName) {
        public Azure {
            accountName = Objects.requireNonNullElse(accountName, "");
            accountKey = Objects.requireNonNullElse(accountKey, "");
            containerName = Objects.requireNonNullElse(containerName, "");
        }
    }

    public record Gcp(String bucket, String projectId, String credentialsJson) {
        public Gcp {
            bucket = Objects.requireNonNullElse(bucket, "");
            projectId = Objects.requireNonNullElse(projectId, "");
            credentialsJson = Objects.requireNonNullElse(credentialsJson, "");
        }
    }
}
