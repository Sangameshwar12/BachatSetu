package in.bachatsetu.backend.admin.domain.configuration.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FeatureKeyTest {

    @Test
    void containsExactlyTheNineSpecifiedFeatures() {
        assertThat(FeatureKey.values()).containsExactlyInAnyOrder(
                FeatureKey.AUTHENTICATION, FeatureKey.PAYMENTS, FeatureKey.NOTIFICATIONS, FeatureKey.STORAGE,
                FeatureKey.RECEIPTS, FeatureKey.AUCTION, FeatureKey.ANALYTICS, FeatureKey.AUDIT,
                FeatureKey.SIGNUP);
    }
}
