package in.bachatsetu.backend.infrastructure.persistence.audit;

import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * Provides a fixed, well-known placeholder auditor for local development and QA only.
 *
 * <p>{@link JpaAuditingConfig}'s default {@link CurrentAuditorProvider} always resolves to
 * {@link Optional#empty()} because no component in this codebase yet bridges the authenticated
 * JWT principal (or an anonymous/system actor for pre-login flows such as signup) into a real
 * auditor identity, mirroring the same "no resolution strategy designed yet" gap that
 * {@code LocalTenantScopeProviderConfig} already documents for tenant scoping. Since every
 * persisted entity's {@code created_by}/{@code updated_by} columns are required by
 * {@code JpaMappingSupport.auditInfo(...)}, leaving the default empty auditor active means no
 * entity can ever be read back locally. Returning one fixed actor unblocks local development;
 * this is a deliberate local-only stopgap, not a substitute for real auditor resolution, and must
 * never be active outside the "local" profile.
 */
@Configuration(proxyBeanMethods = false)
@Profile("local")
public class LocalCurrentAuditorProviderConfig {

    /** Placeholder actor recorded as the creator/updater of every locally persisted record. */
    public static final UUID DEFAULT_ACTOR_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Bean
    @Primary
    CurrentAuditorProvider localCurrentAuditorProvider() {
        return () -> Optional.of(DEFAULT_ACTOR_ID);
    }
}
