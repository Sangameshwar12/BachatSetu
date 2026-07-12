package in.bachatsetu.backend.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import in.bachatsetu.backend.shared.domain.AggregateId;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TenantScopeProviderConfigTest {

    @Test
    void resolvesTheConfiguredTenantIdOnEveryCall() {
        UUID configuredTenantId = UUID.randomUUID();
        TenantScopeProvider provider = new TenantScopeProviderConfig().tenantScopeProvider(configuredTenantId);

        assertThat(provider.currentTenantId()).isEqualTo(new AggregateId(configuredTenantId));
        assertThat(provider.currentTenantId()).isEqualTo(new AggregateId(configuredTenantId));
    }
}
