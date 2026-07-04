package in.bachatsetu.backend.infrastructure.persistence.config;

import in.bachatsetu.backend.infrastructure.persistence.PersistencePackageMarker;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration(proxyBeanMethods = false)
@EntityScan(basePackageClasses = PersistencePackageMarker.class)
@EnableJpaRepositories(basePackageClasses = PersistencePackageMarker.class)
@ConditionalOnProperty(
        prefix = "bachatsetu.persistence.repositories",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class PersistenceConfig {
}
