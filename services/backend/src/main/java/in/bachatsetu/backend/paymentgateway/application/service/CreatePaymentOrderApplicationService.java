package in.bachatsetu.backend.paymentgateway.application.service;

import in.bachatsetu.backend.paymentgateway.application.command.CreatePaymentOrderCommand;
import in.bachatsetu.backend.paymentgateway.application.exception.AmountMismatchException;
import in.bachatsetu.backend.paymentgateway.application.mapper.PaymentGatewayApplicationMapper;
import in.bachatsetu.backend.paymentgateway.application.port.ClockPort;
import in.bachatsetu.backend.paymentgateway.application.port.DomainEventPublisherPort;
import in.bachatsetu.backend.paymentgateway.application.port.PaymentGatewayPort;
import in.bachatsetu.backend.paymentgateway.application.port.TransactionPort;
import in.bachatsetu.backend.paymentgateway.application.query.PaymentOrderResult;
import in.bachatsetu.backend.paymentgateway.application.usecase.CreatePaymentOrderUseCase;
import in.bachatsetu.backend.paymentgateway.domain.model.GatewayOrder;
import in.bachatsetu.backend.paymentgateway.domain.model.GatewayType;
import in.bachatsetu.backend.paymentgateway.domain.port.GatewayOrderRepository;
import in.bachatsetu.backend.payment.application.query.PaymentResult;
import in.bachatsetu.backend.payment.application.usecase.GetPaymentUseCase;
import in.bachatsetu.backend.shared.domain.AggregateId;
import in.bachatsetu.backend.shared.domain.Money;
import java.util.Currency;
import java.util.List;
import java.util.Objects;

/**
 * Creates a provider order for an existing payment, using the pre-existing {@link GetPaymentUseCase} to
 * read the payment (never the {@code Payment} aggregate or its repository directly) and validating the
 * caller-confirmed amount against it before ever calling the provider.
 *
 * <p>Idempotent: if a gateway order already exists for this payment, it is returned unchanged rather than
 * creating a second one or calling the provider again — {@code finance.payment_gateway_orders} enforces
 * at most one order per payment at the database level too.
 */
public final class CreatePaymentOrderApplicationService implements CreatePaymentOrderUseCase {

    private final GetPaymentUseCase getPayment;
    private final GatewayOrderRepository orderRepository;
    private final List<PaymentGatewayPort> gateways;
    private final ClockPort clock;
    private final TransactionPort transaction;
    private final GatewayType defaultProvider;
    private final PaymentGatewayApplicationSupport support;
    private final PaymentGatewayApplicationMapper mapper;

    public CreatePaymentOrderApplicationService(
            GetPaymentUseCase getPayment,
            GatewayOrderRepository orderRepository,
            List<PaymentGatewayPort> gateways,
            DomainEventPublisherPort eventPublisher,
            ClockPort clock,
            TransactionPort transaction,
            PaymentGatewayApplicationMapper mapper,
            GatewayType defaultProvider) {
        this.getPayment = Objects.requireNonNull(getPayment, "get payment use case must not be null");
        this.orderRepository = Objects.requireNonNull(orderRepository, "order repository must not be null");
        this.gateways = List.copyOf(Objects.requireNonNull(gateways, "gateways must not be null"));
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.transaction = Objects.requireNonNull(transaction, "transaction must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
        this.defaultProvider = Objects.requireNonNull(defaultProvider, "default provider must not be null");
        this.support = new PaymentGatewayApplicationSupport(orderRepository, eventPublisher, mapper);
    }

    @Override
    public PaymentOrderResult execute(CreatePaymentOrderCommand command) {
        Objects.requireNonNull(command, "create order command must not be null");
        return transaction.execute(() -> create(command));
    }

    private PaymentOrderResult create(CreatePaymentOrderCommand command) {
        return orderRepository.findByPaymentId(command.tenantId(), command.paymentId())
                .map(mapper::toOrderResult)
                .orElseGet(() -> createNewOrder(command));
    }

    private PaymentOrderResult createNewOrder(CreatePaymentOrderCommand command) {
        PaymentResult payment = getPayment.execute(command.tenantId(), command.paymentId());
        Money paymentAmount = new Money(payment.amountPaise(), Currency.getInstance(payment.currencyCode()));
        if (paymentAmount.compareTo(command.confirmedAmount()) != 0) {
            throw new AmountMismatchException("confirmed amount does not match the payment's recorded amount");
        }
        PaymentGatewayPort gateway = GatewayPortResolver.resolveGateway(gateways, defaultProvider);
        PaymentOrderResult providerResult = gateway.createOrder(command.paymentId(), paymentAmount);
        GatewayOrder order = GatewayOrder.create(
                AggregateId.newId(),
                command.tenantId(),
                command.paymentId(),
                defaultProvider,
                providerResult.providerOrderId(),
                providerResult.paymentLink(),
                command.actorId(),
                clock.now());
        return support.saveAndPublish(order);
    }
}
