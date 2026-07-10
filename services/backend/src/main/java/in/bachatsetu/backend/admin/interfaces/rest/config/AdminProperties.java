package in.bachatsetu.backend.admin.interfaces.rest.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Strongly typed Admin configuration, overridable per environment. */
@ConfigurationProperties(prefix = "bachatsetu.admin")
public record AdminProperties(boolean enabled, int pageSizeDefault, int pageSizeMax) {

    public AdminProperties {
        if (pageSizeDefault < 1) {
            throw new IllegalArgumentException("page size default must be positive");
        }
        if (pageSizeMax < pageSizeDefault) {
            throw new IllegalArgumentException("page size max must not be smaller than the default page size");
        }
    }
}
