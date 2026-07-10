package in.bachatsetu.backend.admin.interfaces.rest.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Binds {@link AdminProperties} unconditionally — unlike every other module's {@code *Properties} class, it
 * is needed by {@code AdminApiMapper}, which (like every other module's API mapper) is a plain, always
 * component-scanned bean, not gated behind {@code bachatsetu.persistence.repositories.enabled}. Gating this
 * binding the same way {@code AdminInfrastructureConfig} gates its own beans would leave {@code
 * AdminApiMapper} without its required {@link AdminProperties} dependency whenever persistence is disabled.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AdminProperties.class)
public class AdminPropertiesConfig {
}
