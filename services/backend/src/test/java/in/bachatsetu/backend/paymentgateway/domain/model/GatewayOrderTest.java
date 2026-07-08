package in.bachatsetu.backend.paymentgateway.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.bachatsetu.backend.paymentgateway.domain.exception.InvalidGatewayOrderStateException;
import in.bachatsetu.backend.shared.domain.AggregateId;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class GatewayOrderTest {

    private static final Instant NOW = Instant.parse("2026-07-08T08:00:00Z");

    @Test
    void createsAnOrderWithNoStatusOrRefundYet() {
        AggregateId actorId = AggregateId.newId();
        GatewayOrder order = newOrder(actorId);

        assertThat(order.gatewayType()).isEqualTo(GatewayType.RAZORPAY);
        assertThat(order.providerOrderId()).isEqualTo("order_123");
        assertThat(order.paymentLink()).isEqualTo("https://example.test/pay/order_123");
        assertThat(order.providerStatus()).isNull();
        assertThat(order.providerRefundId()).isNull();
    }

    @Test
    void updatesTheObservedProviderStatus() {
        AggregateId actorId = AggregateId.newId();
        GatewayOrder order = newOrder(actorId);

        order.updateProviderStatus("captured", actorId, NOW.plusSeconds(60));

        assertThat(order.providerStatus()).isEqualTo("captured");
    }

    @Test
    void recordsARefundExactlyOnce() {
        AggregateId actorId = AggregateId.newId();
        GatewayOrder order = newOrder(actorId);

        order.recordRefund("rfnd_123", actorId, NOW.plusSeconds(60));

        assertThat(order.providerRefundId()).isEqualTo("rfnd_123");
        assertThatThrownBy(() -> order.recordRefund("rfnd_456", actorId, NOW.plusSeconds(90)))
                .isInstanceOf(InvalidGatewayOrderStateException.class);
    }

    @Test
    void rejectsBlankProviderOrderId() {
        assertThatThrownBy(() -> GatewayOrder.create(
                        AggregateId.newId(), AggregateId.newId(), AggregateId.newId(), GatewayType.RAZORPAY,
                        " ", null, AggregateId.newId(), NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private GatewayOrder newOrder(AggregateId actorId) {
        return GatewayOrder.create(
                AggregateId.newId(), AggregateId.newId(), AggregateId.newId(), GatewayType.RAZORPAY,
                "order_123", "https://example.test/pay/order_123", actorId, NOW);
    }
}
