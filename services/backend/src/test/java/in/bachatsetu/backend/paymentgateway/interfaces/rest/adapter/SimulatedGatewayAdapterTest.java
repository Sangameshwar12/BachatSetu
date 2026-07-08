package in.bachatsetu.backend.paymentgateway.interfaces.rest.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import in.bachatsetu.backend.paymentgateway.application.query.PaymentOrderResult;
import in.bachatsetu.backend.paymentgateway.application.query.PaymentStatusResult;
import in.bachatsetu.backend.paymentgateway.application.query.RefundResult;
import in.bachatsetu.backend.paymentgateway.domain.model.GatewayType;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Money;
import org.junit.jupiter.api.Test;

class SimulatedGatewayAdapterTest {

    @Test
    void razorpayGatewayCreatesADistinctOrderPerCall() {
        SimulatedRazorpayGatewayAdapter adapter = new SimulatedRazorpayGatewayAdapter();
        AggregateId paymentId = AggregateId.newId();

        PaymentOrderResult first = adapter.createOrder(paymentId, Money.inr(100_000));
        PaymentOrderResult second = adapter.createOrder(paymentId, Money.inr(100_000));

        assertThat(adapter.supportedProvider()).isEqualTo(GatewayType.RAZORPAY);
        assertThat(first.providerOrderId()).isNotEqualTo(second.providerOrderId());
        assertThat(first.paymentLink()).contains(first.providerOrderId());
        PaymentStatusResult status = adapter.fetchStatus(paymentId, first.providerOrderId());
        assertThat(status.successful()).isTrue();
        assertThat(status.provider()).isEqualTo(GatewayType.RAZORPAY);
    }

    @Test
    void stripeGatewayCreatesAnOrderAndReportsStatus() {
        SimulatedStripeGatewayAdapter adapter = new SimulatedStripeGatewayAdapter();
        AggregateId paymentId = AggregateId.newId();

        PaymentOrderResult order = adapter.createOrder(paymentId, Money.inr(100_000));

        assertThat(adapter.supportedProvider()).isEqualTo(GatewayType.STRIPE);
        assertThat(order.provider()).isEqualTo(GatewayType.STRIPE);
        assertThat(adapter.fetchStatus(paymentId, order.providerOrderId()).successful()).isTrue();
    }

    @Test
    void cashfreeGatewayCreatesAnOrderAndReportsStatus() {
        SimulatedCashfreeGatewayAdapter adapter = new SimulatedCashfreeGatewayAdapter();
        AggregateId paymentId = AggregateId.newId();

        PaymentOrderResult order = adapter.createOrder(paymentId, Money.inr(100_000));

        assertThat(adapter.supportedProvider()).isEqualTo(GatewayType.CASHFREE);
        assertThat(order.provider()).isEqualTo(GatewayType.CASHFREE);
        assertThat(adapter.fetchStatus(paymentId, order.providerOrderId()).successful()).isTrue();
    }

    @Test
    void razorpayRefundReturnsASuccessfulResult() {
        SimulatedRazorpayRefundAdapter adapter = new SimulatedRazorpayRefundAdapter();
        AggregateId paymentId = AggregateId.newId();

        RefundResult result = adapter.initiateRefund(paymentId, "order_1", Money.inr(100_000));

        assertThat(adapter.supportedProvider()).isEqualTo(GatewayType.RAZORPAY);
        assertThat(result.successful()).isTrue();
        assertThat(result.providerRefundId()).isNotBlank();
        assertThat(result.paymentId()).isEqualTo(paymentId.value());
    }

    @Test
    void stripeRefundReturnsASuccessfulResult() {
        SimulatedStripeRefundAdapter adapter = new SimulatedStripeRefundAdapter();
        AggregateId paymentId = AggregateId.newId();

        RefundResult result = adapter.initiateRefund(paymentId, "pi_1", Money.inr(100_000));

        assertThat(adapter.supportedProvider()).isEqualTo(GatewayType.STRIPE);
        assertThat(result.successful()).isTrue();
    }

    @Test
    void cashfreeRefundReturnsASuccessfulResult() {
        SimulatedCashfreeRefundAdapter adapter = new SimulatedCashfreeRefundAdapter();
        AggregateId paymentId = AggregateId.newId();

        RefundResult result = adapter.initiateRefund(paymentId, "cf_order_1", Money.inr(100_000));

        assertThat(adapter.supportedProvider()).isEqualTo(GatewayType.CASHFREE);
        assertThat(result.successful()).isTrue();
    }
}
