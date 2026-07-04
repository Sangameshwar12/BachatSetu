package in.bachatsetu.backend.infrastructure.persistence.audit;

import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration(proxyBeanMethods = false)
@EnableJpaAuditing(auditorAwareRef = "jpaAuditorAware")
@ConditionalOnProperty(
        prefix = "bachatsetu.persistence.auditing",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class JpaAuditingConfig {

    @Bean
    @ConditionalOnMissingBean(CurrentAuditorProvider.class)
    CurrentAuditorProvider currentAuditorProvider() {
        return Optional::empty;
    }

    @Bean
    AuditorAware<UUID> jpaAuditorAware(CurrentAuditorProvider currentAuditorProvider) {
        return currentAuditorProvider::currentAuditorId;
    }
}
